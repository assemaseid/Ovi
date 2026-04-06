import datetime
import uuid

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from src.app.models.device import Device
from src.app.models.grant import Grant
from src.app.models.user import User
from src.database import SessionDep


async def require_device_permission(
    device_uuid: str,
    permission: str,
    current_user: User,
    session: SessionDep,
) -> Device:
    """Return the device if user has the required ACL permission, else 403."""
    try:
        device_uuid_formatted = uuid.UUID(device_uuid)
    except ValueError:
        raise HTTPException(status_code=400,
                            detail="Could not convert device_uuid from str to uuid format")

    # Owner always has full access
    result = await session.execute(
        select(Device).where(Device.device_uuid == device_uuid_formatted)
    )
    device = result.scalar_one_or_none()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    if device.owner_uuid == current_user.user_uuid:
        return device

    # Check grant
    now = datetime.now(datetime.UTC)
    grant_q = await session.execute(
        select(Grant).where(
            Grant.device_uuid == device_uuid_formatted,
            Grant.user_uuid == current_user.user_uuid,
            Grant.valid_from <= now,
            (Grant.valid_until == None) | (Grant.valid_until >= now),
        )
    )
    grant = grant_q.scalar_one_or_none()
    if not grant:
        raise HTTPException(status_code=403, detail="Access denied")

    if permission not in (grant.permissions or []):
        raise HTTPException(status_code=403, detail=f"Permission '{permission}' not granted")

    return device