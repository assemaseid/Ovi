import hashlib
import hmac
import logging
import secrets
import time
from datetime import datetime, UTC, timedelta

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from src.app.models.device import Device
from src.app.models.pin_state import PinState
from src.app.models.user import User
from src.app.services.fcm_service import send_notification
from src.database import async_session_factory, SessionDep

logger = logging.getLogger(__name__)

DEFAULT_ROTATION_SECONDS = 86400  # 24 hours
PIN_LENGTH = 6


def _compute_pin(device_secret: str, time_slot: int) -> str:
    key = device_secret.encode()
    msg = str(time_slot).encode()
    digest = hmac.new(key, msg, hashlib.sha256).digest()
    pin_numeric = int.from_bytes(digest, "big") % (10 ** PIN_LENGTH)
    return str(pin_numeric).zfill(PIN_LENGTH)


def _current_slot(rotation_seconds: int = DEFAULT_ROTATION_SECONDS) -> int:
    return int(time.time()) // rotation_seconds


async def get_current_pin(device: Device) -> str | None:
    device_secret = (device.config or {}).get("device_secret")
    if not device_secret:
        return None
    rotation_seconds = (device.config or {}).get("rotation_seconds", DEFAULT_ROTATION_SECONDS)
    slot = _current_slot(rotation_seconds)
    return _compute_pin(device_secret, slot)


async def check_and_rotate(session: SessionDep, device: Device) -> bool:
    device_secret = (device.config or {}).get("device_secret")
    if not device_secret:
        return False

    rotation_seconds = (device.config or {}).get("rotation_seconds", DEFAULT_ROTATION_SECONDS)
    current_slot = _current_slot(rotation_seconds)

    pin_state_q = await session.execute(
        select(PinState).where(PinState.device_uuid == device.device_uuid)
    )
    pin_state = pin_state_q.scalar_one_or_none()

    if pin_state and pin_state.last_rotation_slot == current_slot:
        return False  # already rotated for this slot

    # Rotation needed
    if pin_state is None:
        pin_state = PinState(device_uuid=device.device_uuid)
        session.add(pin_state)

    pin_state.last_rotation_slot = current_slot
    pin_state.last_rotation_at = datetime.now(UTC)
    pin_state.rotation_counter = (pin_state.rotation_counter or 0) + 1
    pin_state.updated_at = datetime.now(UTC)
    await session.commit()

    logger.info("PIN rotated for device=%s slot=%d", device.device_uuid, current_slot)

    # Notify owner via FCM
    owner_q = await session.execute(
        select(User).where(User.user_uuid == device.user_uuid)
    )
    owner = owner_q.scalar_one_or_none()
    if owner and owner.fcm_token:
        await send_notification(
            fcm_token=owner.fcm_token,
            title="New PIN Code",
            body="Your lock PIN has been rotated. Open the app to view it.",
            data={"action": "show_pin", "device_uuid": str(device.device_uuid)},
        )

    return True


async def check_scheduled_rotation(session, device: Device) -> bool:
    from sqlalchemy.orm.attributes import flag_modified
    from src.app.services.mqtt_service import MQTTService
    from src.app.services.crypto_service import crypto_service
    import uuid as uuid_lib

    await session.refresh(device)
    cfg = device.config or {}
    schedule = cfg.get("pin_schedule", {})
    if not schedule.get("enabled", False):
        return False
    next_rotation_str = schedule.get("next_rotation_at")
    if not next_rotation_str:
        return False
    try:
        next_rotation = datetime.fromisoformat(next_rotation_str.replace("Z", "+00:00"))
        if next_rotation.tzinfo is None:
            next_rotation = next_rotation.replace(tzinfo=UTC)
    except ValueError:
        logger.warning("Invalid next_rotation_at for device=%s: %s", device.device_uuid, next_rotation_str)
        return False

    now = datetime.now(UTC)
    if now < next_rotation:
        return False

    # Generate a new device_secret so the PIN actually changes
    new_secret = secrets.token_hex(32)
    interval_hours = schedule.get("rotation_interval_hours", 24)
    new_next = next_rotation + timedelta(hours=interval_hours)
    while new_next <= now:
        new_next += timedelta(hours=interval_hours)

    new_cfg = dict(cfg)
    new_cfg["device_secret"] = new_secret
    new_cfg["pin_schedule"] = {**schedule, "next_rotation_at": new_next.isoformat()}
    device.config = new_cfg
    flag_modified(device, "config")
    await session.commit()

    # Push new secret to device so it can verify keypad PINs with the new secret
    dev_uuid_str = str(device.device_uuid)
    mqtt = MQTTService()
    if mqtt.is_connected:
        payload = {
            "cmd": "rotate_pin",
            "msg_id": f"msg_{uuid_lib.uuid4()}",
            "timestamp": int(time.time()),
            "new_secret": new_secret,
            "signature": crypto_service.sign_dict({
                "device_uuid": dev_uuid_str,
                "new_secret": new_secret,
                "type": "rotate_pin",
            }),
        }
        await mqtt.publish(payload=payload, packetId=0,
                           topicName=MQTTService.cmd_topic(dev_uuid_str))

    # Push new PIN to any open app screens via WebSocket
    from src.app.services.event_handler import _get_ws_manager, _get_allowed_users
    new_pin = _compute_pin(new_secret, _current_slot(new_cfg.get("rotation_seconds", DEFAULT_ROTATION_SECONDS)))
    ws_manager = _get_ws_manager()
    allowed_users = await _get_allowed_users(dev_uuid_str)
    await ws_manager.broadcast_event(dev_uuid_str, {
        "type": "pin_rotated",
        "new_pin": new_pin,
        "next_rotation_at": new_next.isoformat(),
    }, allowed_users)


    # FCM notification
    owner_q = await session.execute(select(User).where(User.user_uuid == device.user_uuid))
    owner = owner_q.scalar_one_or_none()
    if owner and owner.fcm_token:
        await send_notification(
            fcm_token=owner.fcm_token,
            title="PIN Rotated",
            body="Your lock PIN has been automatically rotated. Open the app to view it.",
            data={"action": "show_pin", "device_uuid": dev_uuid_str},
        )

    logger.info("Scheduled rotation done for device=%s, next=%s", dev_uuid_str, new_next.isoformat())
    return True


async def run_rotation_check() -> None:
    async with async_session_factory() as session:
        result = await session.execute(select(Device))
        devices = result.scalars().all()
        for device in devices:
            try:
                await check_and_rotate(session, device)
                await check_scheduled_rotation(session, device)
            except Exception as e:
                logger.error("PIN rotation check failed for device=%s: %s", device.device_uuid, e)
