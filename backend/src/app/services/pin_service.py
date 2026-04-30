import hashlib
import hmac
import logging
import time
from datetime import datetime, UTC

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


async def run_rotation_check() -> None:
    async with async_session_factory() as session:
        result = await session.execute(select(Device))
        devices = result.scalars().all()
        for device in devices:
            try:
                await check_and_rotate(session, device)
            except Exception as e:
                logger.error("PIN rotation check failed for device=%s: %s", device.device_uuid, e)
