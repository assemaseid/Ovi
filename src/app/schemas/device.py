from pydantic import BaseModel, Field
from uuid import UUID
from datetime import datetime


class DeviceRegister(BaseModel):
    device_uuid: str
    public_key: str
    hw_version: str = "v2.0"
    name: str | None = None
    location: str | None = None


# after registration configuration
class DeviceConfig(BaseModel):
    server_public_key: str  # PEM format
    pin_rotation_seconds: int = 3600
    mqtt_broker: str
    mqtt_port: int = 8883
    device_uuid: str


class DeviceResponse(BaseModel):
    id: UUID
    device_uuid: str
    name: str | None
    location: str | None
    is_online: bool
    battery_level: int
    last_seen: datetime | None
    created_at: datetime

    class Config:
        from_attributes = True