import uuid as uuid_lib
from datetime import datetime
from typing import Any

from pydantic import BaseModel


class GuestOut(BaseModel):
    grant_uuid: uuid_lib.UUID
    user_uuid: uuid_lib.UUID
    user_name: str | None = None
    user_email: str | None = None
    permissions: list[Any]
    created_at: datetime
    valid_until: datetime | None = None


class GuestRequestBody(BaseModel):
    hardware_id: str


class GuestRequestResponse(BaseModel):
    device_uuid: str
    status: str


class GuestJoinBody(BaseModel):
    pin: str
