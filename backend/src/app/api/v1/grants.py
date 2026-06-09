import hashlib
import secrets
import uuid
import logging
from datetime import datetime, UTC, timedelta

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select, or_
from sqlalchemy.orm.attributes import flag_modified

from src.app.api.dependencies import require_device_permission
from src.app.models.device import Device
from src.app.models.grant import Grant
from src.app.models.notification import UserNotification
from src.app.models.user import User
from src.app.schemas.grant import GrantCreate, GrantOut
from src.app.schemas.guest import GuestRequestBody, GuestRequestResponse, GuestJoinBody
from src.app.services.fcm_service import send_notification
from src.database import SessionDep
from src.dependencies import get_current_user

router = APIRouter(prefix="/grants", tags=["Grants"])
logger = logging.getLogger(__name__)


@router.post("/guest-request", response_model=GuestRequestResponse)
async def guest_request(
    body: GuestRequestBody,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> GuestRequestResponse:
    conditions = [Device.hardware_id == body.hardware_id]

    try:
        conditions.append(Device.device_uuid == uuid.UUID(body.hardware_id))
    except ValueError:
        pass

    result = await session.execute(select(Device).where(or_(*conditions)))
    device = result.scalar_one_or_none()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    pin = f"{secrets.randbelow(900000) + 100000:06d}"
    pin_hash = hashlib.sha256(pin.encode()).hexdigest()

    now = datetime.now(UTC)
    expires = now + timedelta(minutes=15)

    pending = device.config.get("pending_guest_pins", [])
    pending = [
        p for p in pending
        if datetime.fromisoformat(p["expires_at"]).replace(tzinfo=UTC) > now
    ]
    pending.append({
        "hash": pin_hash,
        "expires_at": expires.isoformat(),
        "requester_uuid": str(current_user.user_uuid),
    })
    device.config = {**device.config, "pending_guest_pins": pending}
    flag_modified(device, "config")
    await session.commit()

    owner_q = await session.execute(select(User).where(User.user_uuid == device.user_uuid))
    owner = owner_q.scalar_one_or_none()
    notif_title = "Guest Access Request"
    notif_body = f"Share this PIN with your guest: {pin}"

    notif = UserNotification(
        user_uuid=device.user_uuid,
        title=notif_title,
        body=notif_body,
    )
    session.add(notif)
    await session.commit()
    await session.refresh(notif)

    if owner and owner.fcm_token:
        await send_notification(
            fcm_token=owner.fcm_token,
            title=notif_title,
            body=notif_body,
            data={"action": "guest_request", "device_uuid": str(device.device_uuid), "pin": pin},
        )

    logger.info("Guest request for device=%s requester=%s",
                device.device_uuid, current_user.user_uuid)

    return GuestRequestResponse(device_uuid=str(device.device_uuid), status="pending")


@router.post("/{device_uuid}/guest-join", response_model=GrantOut, status_code=201)
async def guest_join(
    device_uuid: str,
    body: GuestJoinBody,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> GrantOut:
    result = await session.execute(
        select(Device).where(Device.device_uuid == uuid.UUID(device_uuid))
    )
    device = result.scalar_one_or_none()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    pending = device.config.get("pending_guest_pins", [])
    now = datetime.now(UTC)
    pin_hash = hashlib.sha256(body.pin.encode()).hexdigest()

    matched = None
    new_pending = []
    for p in pending:
        exp = datetime.fromisoformat(p["expires_at"])
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=UTC)
        if p["hash"] == pin_hash and exp > now:
            matched = p
        else:
            new_pending.append(p)

    if not matched:
        raise HTTPException(status_code=400, detail="Invalid or expired PIN")

    existing = await session.execute(
        select(Grant).where(
            Grant.device_uuid == uuid.UUID(device_uuid),
            Grant.user_uuid == current_user.user_uuid,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="Already have access to this device")

    grant = Grant(
        device_uuid=uuid.UUID(device_uuid),
        user_uuid=current_user.user_uuid,
        permissions=["read_status", "unlock", "lock"],
        created_by=device.user_uuid,
    )
    session.add(grant)

    device.config = {**device.config, "pending_guest_pins": new_pending}
    flag_modified(device, "config")

    await session.commit()
    await session.refresh(grant)
    logger.info("Guest joined device=%s user=%s",
                device_uuid, current_user.user_uuid)

    return GrantOut.model_validate(grant)


@router.post("", response_model=GrantOut, status_code=201)
async def create_grant(
    body: GrantCreate,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> GrantOut:
    # give a user access to a device. Owner only
    await require_device_permission(
        body.device_uuid,
        "admin",
        current_user,
        session)

    existing = await session.execute(
        select(Grant).where(
            Grant.device_uuid == uuid.UUID(body.device_uuid),
            Grant.user_uuid == uuid.UUID(body.user_uuid),
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="Grant already exists for this user")

    grant = Grant(
        device_uuid=uuid.UUID(body.device_uuid),
        user_uuid=uuid.UUID(body.user_uuid),
        permissions=body.permissions,
        valid_from=body.valid_from,
        valid_until=body.valid_until,
        created_by=current_user.user_uuid,
    )
    session.add(grant)
    await session.commit()
    await session.refresh(grant)
    return GrantOut.model_validate(grant)


@router.get("/{device_uuid}", response_model=list[GrantOut])
async def list_grants(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> list[GrantOut]:

    # list all grants for a device, owner only
    await require_device_permission(device_uuid, "admin", current_user, session)

    result = await session.execute(
        select(Grant).where(Grant.device_uuid == uuid.UUID(device_uuid))
    )
    return [GrantOut.model_validate(g) for g in result.scalars().all()]


@router.delete("/self/{device_uuid}", status_code=204)
async def revoke_own_grant(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> None:
    """Пользователь удаляет свой собственный доступ к замку (без прав admin)."""
    result = await session.execute(
        select(Grant).where(
            Grant.device_uuid == uuid.UUID(device_uuid),
            Grant.user_uuid == current_user.user_uuid,
        )
    )
    grant = result.scalar_one_or_none()
    if not grant:
        raise HTTPException(status_code=404, detail="Grant not found")

    await session.delete(grant)
    await session.commit()


@router.delete("/{grant_uuid}", status_code=204)
async def revoke_grant(
    grant_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> None:
    """Владелец отзывает чужой доступ к замку."""
    result = await session.execute(
        select(Grant).where(Grant.grant_uuid == uuid.UUID(grant_uuid))
    )
    grant = result.scalar_one_or_none()
    if not grant:
        raise HTTPException(status_code=404, detail="Grant not found")

    await require_device_permission(str(grant.device_uuid), "admin", current_user, session)

    await session.delete(grant)
    await session.commit()
