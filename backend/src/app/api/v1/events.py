import uuid
import logging

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select

from src.app.api.dependencies import require_device_permission
from src.app.models.event import Event
from src.app.models.user import User
from src.app.schemas.device import EventOut
from src.database import SessionDep
from src.dependencies import get_current_user

router = APIRouter(prefix="/events", tags=["Events"])
logger = logging.getLogger(__name__)


@router.get("/{device_uuid}", response_model=list[EventOut])
async def get_device_events(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
    limit: int = Query(default=50, le=200),
    offset: int = Query(default=0),
) -> list[EventOut]:
    # require read-status permissions
    await require_device_permission(device_uuid,
                                    "read_status",
                                    current_user,
                                    session)

    result = await session.execute(
        select(Event)
        .where(Event.device_uuid == uuid.UUID(device_uuid))
        .order_by(Event.created_at.desc())
        .limit(limit)
        .offset(offset)
    )
    return result.scalars().all()
