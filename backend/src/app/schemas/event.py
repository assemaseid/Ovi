from typing import Any

from pydantic import BaseModel


class DeviceEventRequest(BaseModel):
    msg_id: str
    device_uuid: str
    timestamp: int
    event: dict[str, Any]
    signature: str


class DeviceEventResponse(BaseModel):
    accepted: bool
    duplicate: bool = False

