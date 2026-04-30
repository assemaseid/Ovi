import uuid
import logging

from fastapi import (
                     APIRouter,
                     Depends,
                     HTTPException,
                     Query,
                     Request
                     )
from pydantic import BaseModel
from sqlalchemy import select
from typing import Any

from src.app.api.dependencies import require_device_permission
from src.app.models.device import Device
from src.app.models.event import Event
from src.app.models.user import User
from src.app.schemas.device import EventOut
from src.app.services.fcm_service import send_notification
from src.app.services.event_handler import (
    _save_event,
    _verify_device_signature,
    _get_allowed_users,
    _get_ws_manager,
    _build_notification,
)
from src.database import SessionDep, async_session_factory
from src.dependencies import get_current_user

router = APIRouter(prefix="/events", tags=["Events"])
logger = logging.getLogger(__name__)


class DeviceEventRequest(BaseModel):
    msg_id: str
    device_uuid: str
    timestamp: int
    event: dict[str, Any]
    signature: str


class DeviceEventResponse(BaseModel):
    accepted: bool
    duplicate: bool = False

# дублирование handle_device_event
@router.post("", response_model=DeviceEventResponse, status_code=201)
async def post_device_event(
    body: DeviceEventRequest,
    request: Request,
    session: SessionDep,
) -> DeviceEventResponse:

    device_result = await session.execute(
        select(Device).where(Device.device_uuid == uuid.UUID(body.device_uuid))
    )
    device = device_result.scalar_one_or_none()
    if device is None:
        raise HTTPException(status_code=404, detail="Device not found")

    payload = body.model_dump()

    verified = await _verify_device_signature(
                                              payload,
                                              body.device_uuid
                                              )
    if not verified:
        logger.warning("Invalid signature on HTTP event from device=%s",
                       body.device_uuid)

    is_new = await _save_event(payload,
                               body.device_uuid,
                               verified)
    if not is_new:
        return DeviceEventResponse(accepted=True, duplicate=True)

    event_type = body.event.get("type", "unknown")
    allowed_users = await _get_allowed_users(body.device_uuid)

    ws_manager = _get_ws_manager()
    await ws_manager.broadcast_event(body.device_uuid, body.event, allowed_users)

    connected_users = set(ws_manager._connections.keys())
    offline_users = allowed_users - connected_users
    if offline_users:
        async with async_session_factory() as session:
            rows = await session.execute(
                select(User.user_uuid, User.fcm_token).where(
                    User.user_uuid.in_(offline_users),
                    User.fcm_token.isnot(None),
                )
            )
            fcm_tokens = dict()
            for r in rows:
                fcm_tokens[str(r.user_uuid)] = r.fcm_token

        title, body_text = _build_notification(event_type, body.event)
        for token in fcm_tokens.values():
            await send_notification(
                fcm_token=token,
                title=title,
                body=body_text,
                data={"event_type": event_type,
                      "device_uuid": body.device_uuid},
            )

    return DeviceEventResponse(accepted=True)


@router.get("/{device_uuid}", response_model=list[EventOut])
async def get_device_events(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
    limit: int = Query(default=50, le=200),
    offset: int = Query(default=0),
) -> list[EventOut]:
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
