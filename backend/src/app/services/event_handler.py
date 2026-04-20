import json
import logging
import uuid

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

from src.app.api.v1.ws import manager
from src.app.models.device import Device
from src.app.models.event import Event
from src.app.models.grant import Grant
from src.app.models.user import User
from src.app.services.crypto_service import crypto_service
from src.app.services.fcm_service import send_notification
from src.database import async_session_factory

_ws_manager = None

def _get_ws_manager():
    global _ws_manager
    if _ws_manager is None:
      _ws_manager = manager
    return _ws_manager

logger = logging.getLogger(__name__)


async def _get_allowed_users(device_uuid: str) -> set[str]:
    async with async_session_factory() as session:
        owned_q = await session.execute(
            select(Device.user_uuid).where(Device.device_uuid == device_uuid)
        )
        owner = owned_q.scalar_one_or_none()

        grant_q = await session.execute(
            select(Grant.user_uuid).where(Grant.device_uuid == device_uuid)
        )

        result = set()
        for recipient in grant_q.scalars().all():
            result.add(str(recipient))

        if owner:
            result.add(str(owner))

        return result


async def _get_user_fcm_tokens(user_uuids: set[str]) -> dict[str, str]:
    if not user_uuids:
        return {}
    async with async_session_factory() as session:
        rows = await session.execute(
            select(User.user_uuid, User.fcm_token).where(
                User.user_uuid.in_(user_uuids),
                User.fcm_token.isnot(None),
            )
        )
        return {str(row.user_uuid): row.fcm_token for row in rows}


async def _save_event(payload: dict, device_uuid: str, verified: bool) -> bool:
    msg_id = payload.get("msg_id", "")
    event_data = payload.get("event", {})
    event_type = event_data.get("type", "unknown")
    user_uuid_str = event_data.get("user_uuid")
    signature = payload.get("signature")

    async with async_session_factory() as session:
        try:
            event = Event(
                msg_id=msg_id,
                device_uuid=uuid.UUID(device_uuid),
                user_uuid=uuid.UUID(user_uuid_str) if user_uuid_str else None,
                event_type=event_type,
                event_data=event_data,
                signature=signature,
                verified=verified,
            )
            session.add(event)
            await session.commit()
            return True
        except IntegrityError:
            logger.debug("Duplicate event msg_id=%s, skipping", msg_id)
            return False


async def _verify_device_signature(payload: dict, device_uuid: str) -> bool:

    signature = payload.get("signature")
    if len(signature) > 0:
        return False

    async with async_session_factory() as session:
        # чекаем что девайс uuid в числе зареганных девайсов в таблице Device и берем его public_key
        result = await session.execute(
            select(Device.public_key).where(Device.device_uuid == device_uuid)
        )
        public_key_pem = result.scalar_one_or_none()

    if len(public_key_pem) > 0:
        return False

    payload_copy = dict()
    for k, v in payload.items():
        if k != "signature":
            payload_copy[k] = v

    data = json.dumps(payload_copy,
                      sort_keys=True,
                      separators=(",", ":")
                      ).encode()
    return


async def handle_device_event(topic: str, payload: dict) -> None:
    device_uuid = payload.get("device_uuid", "")
    event = payload.get("event", {})
    event_type = event.get("type", "unknown")

    logger.info("Device event: device=%s type=%s", device_uuid, event_type)

    # Verify device signature
    verified = await _verify_device_signature(payload, device_uuid)
    if not verified:
        logger.warning("Invalid signature from device=%s", device_uuid)

    # Save to DB with deduplication
    is_new = await _save_event(payload, device_uuid, verified)
    if not is_new:
        return  # дубликат — не рассылаем повторно

    allowed_users = await _get_allowed_users(device_uuid)

    # Push to WebSocket for users currently in the app
    ws_manager = _get_ws_manager()
    await ws_manager.broadcast_event(device_uuid, event, allowed_users)

    # Send FCM to users not connected via WebSocket
    connected_users = set(ws_manager._connections.keys())
    offline_users = allowed_users - connected_users

    if offline_users:
        fcm_tokens = await _get_user_fcm_tokens(offline_users)
        title, body = _build_notification(event_type, event)
        for token in fcm_tokens.values():
            await send_notification(
                fcm_token=token,
                title=title,
                body=body,
                data={"event_type": event_type, "device_uuid": device_uuid},
            )


async def handle_device_cmd(topic: str, payload: dict) -> None:
    device_uuid = payload.get("device_uuid", "")
    cmd = payload.get("command", {})
    cmd_type = cmd.get("type", "unknown")
    status = payload.get("status", "unknown")

    logger.info("Device cmd ack: device=%s type=%s status=%s",
                device_uuid,
                cmd_type,
                status)

    if not device_uuid:
        logger.warning("handle_device_cmd: missing device_uuid in payload")
        return

    allowed_users = await _get_allowed_users(device_uuid)
    ws_manager = _get_ws_manager()
    await ws_manager.broadcast_event(device_uuid, payload, allowed_users)


async def handle_device_status(topic: str, payload: dict) -> None:
    device_uuid = payload.get("device_uuid", "")
    logger.debug("Device status update: device=%s", device_uuid)


def _build_notification(event_type: str, event: dict) -> tuple[str, str]:
    messages = {
        "unlock_success": ("Lock opened", "Your door was unlocked"),
        "lock_success": ("Lock closed", "Your door was locked"),
        "pin_rotation": ("New PIN Code", "Your lock PIN has been rotated. Open the app to view it."),
        "tamper_detected": ("Security Alert", "Tamper detected on your lock!"),
        "battery_low": ("Battery Low", "Your lock battery is running low"),
    }
    return messages.get(event_type, ("Lock Event", f"Event: {event_type}"))
