from datetime import datetime
from pydantic import BaseModel
from typing import Any


class OfflineEvent(BaseModel):
    msg_id: str
    event_data: dict[str, Any]
    signature: str
    created_at: datetime


class SyncRequest(BaseModel):
    device_uuid: str
    events: list[OfflineEvent]


class SyncResponse(BaseModel):
    received: int
    duplicates: int
