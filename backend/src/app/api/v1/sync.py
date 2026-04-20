import logging
import uuid
from datetime import datetime, UTC

from fastapi import APIRouter, HTTPException
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

from src.app.models.device import Device
from src.app.models.event import Event
from src.app.models.offline_events_queue import OfflineEventsQueue
from src.app.schemas.sync import OfflineEvent, SyncRequest, SyncResponse
from src.database import SessionDep

router = APIRouter(prefix="/sync", tags=["Sync"])
logger = logging.getLogger(__name__)


@router.post("", response_model=SyncResponse)
async def sync_offline_events(
    body: SyncRequest,
    session: SessionDep,
) -> SyncResponse:

    device_result = await session.execute(
        select(Device).where(Device.device_uuid == uuid.UUID(body.device_uuid))
    )
    device = device_result.scalar_one_or_none()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    received = 0
    duplicates = 0

    for item in body.events:
        event_type = item.event_data.get("type", "unknown")
        user_uuid_str = item.event_data.get("user_uuid")

        try:
            event = Event(
                msg_id=item.msg_id,
                device_uuid=device.device_uuid,
                user_uuid=uuid.UUID(user_uuid_str) if user_uuid_str else None,
                event_type=event_type,
                event_data=item.event_data,
                signature=item.signature,
                verified=False,  # signature verification can be done async later
            )
            session.add(event)
            await session.flush()

            # Mark as synced in offline queue if it was stored there
            queue_result = await session.execute(
                select(OfflineEventsQueue).where(
                    OfflineEventsQueue.device_uuid == device.device_uuid,
                    OfflineEventsQueue.event_data["msg_id"].astext == item.msg_id,
                )
            )
            queue_entry = queue_result.scalar_one_or_none()
            if queue_entry:
                queue_entry.synced_at = datetime.now(UTC)

            received += 1

        except IntegrityError:
            await session.rollback()
            duplicates += 1
            continue

    await session.commit()
    logger.info("Sync device=%s received=%d duplicates=%d", body.device_uuid, received, duplicates)
    return SyncResponse(received=received, duplicates=duplicates)
