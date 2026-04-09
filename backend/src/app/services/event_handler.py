"""
Handles incoming MQTT messages from devices.
Bridges MQTT → WebSocket (for online users) and FCM (for offline users).
"""
import logging
from sqlalchemy import select

from src.app.models.device import Device
from src.app.models.grant import Grant
from src.app.models.user import User
from src.app.services.fcm_service import send_notification
from src.database import async_session_factory

logger = logging.getLogger(__name__)

# Imported lazily to avoid circular imports
_ws_manager = None


def _get_ws_manager():
    global _ws_manager
    if _ws_manager is None:
        from src.app.api.v1.ws import manager
        _ws_manager = manager
    return _ws_manager


async def _get_allowed_users(device_uuid: str) -> set[str]:
    async with async_session_factory() as session:

        # когда происходит какое то событие владелец замка тоже может видеть, что происходит с замком
        owned_q = await session.execute(
            select(Device.user_uuid).where(Device.device_uuid == device_uuid)
        )
        owner = owned_q.scalar_one_or_none()

        # клиент видит какие события с замком (к которому подключился) происходят
        grant_q = await session.execute(
            select(Grant.user_uuid).where(Grant.device_uuid == device_uuid)
        )

        grant_recipients = set()
        for recipient in grant_q.scalars().all():
            grant_recipients.add(str(recipient))

        if len(owner) > 0:
            grant_recipients.add(str(owner))

        return grant_recipients


async def _get_user_fcm_tokens(user_uuids: set[str]) -> dict[str, str]:
    """Return {user_uuid: fcm_token} for users that have fcm_token set."""
    if not user_uuids:
        return {}
    async with async_session_factory() as db:
        result = await db.execute(
            select(User.user_uuid, User.fcm_token).where(
                User.user_uuid.in_(user_uuids),
                User.fcm_token.isnot(None),
            )
        )
        return {str(row.user_uuid): row.fcm_token for row in result}


async def handle_device_event(topic: str, payload: dict) -> None:
    """Called by MQTTService when a message arrives on devices/{device_uuid}/events."""
    device_uuid = payload.get("device_uuid", "")
    event = payload.get("event", {})
    event_type = event.get("type", "unknown")

    logger.info("Device event: device=%s type=%s", device_uuid, event_type)

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
        for user_uuid, token in fcm_tokens.items():
            await send_notification(
                fcm_token=token,
                title=title,
                body=body,
                data={"event_type": event_type, "device_uuid": device_uuid},
            )


async def handle_device_status(topic: str, payload: dict) -> None:
    """Called by MQTTService when a message arrives on devices/+/status."""
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
