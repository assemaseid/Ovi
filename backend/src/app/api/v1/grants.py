import uuid
import logging
from datetime import datetime
from typing import Any

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, ConfigDict
from sqlalchemy import select

from src.app.api.dependencies import require_device_permission
from src.app.models.grant import Grant
from src.app.models.user import User
from src.database import SessionDep
from src.dependencies import get_current_user

router = APIRouter(prefix="/grants", tags=["Grants"])
logger = logging.getLogger(__name__)


class GrantCreate(BaseModel):
    device_uuid: str
    user_uuid: str
    permissions: list[str] = ["read_status", "unlock", "lock"]
    valid_from: datetime | None = None
    valid_until: datetime | None = None


class GrantOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    grant_uuid: uuid.UUID
    device_uuid: uuid.UUID
    user_uuid: uuid.UUID
    permissions: list[Any]
    valid_from: datetime
    valid_until: datetime | None
    created_by: uuid.UUID
    created_at: datetime


@router.post("", response_model=GrantOut, status_code=201)
async def create_grant(
    body: GrantCreate,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> GrantOut:
    """Give a user access to a device. Owner only."""
    # giva a user access to a device. Owner only
    await require_device_permission(body.device_uuid, "admin", current_user, session)

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
