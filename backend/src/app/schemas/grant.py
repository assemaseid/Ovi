import uuid
from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict


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