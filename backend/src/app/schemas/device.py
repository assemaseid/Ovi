import uuid
from datetime import datetime

from pydantic import BaseModel, Field, ConfigDict
from typing import Any

class DeviceInfo(BaseModel):
    hardware_id: str = Field(max_length=64)
    public_key: str
    type: str = Field(default="smart_lock_v2")
    capabilities: list[str] = Field(default=["ble", "wifi", "keypad"])


class OwnerInfo(BaseModel):
    user_uuid: str
    location: str
    timezone: str


class DeviceRegisterRequest(BaseModel):
    device: DeviceInfo
    owner_info: OwnerInfo


class DeviceConfig(BaseModel):
    pin_length: int = 6
    rotation_hours: int = 24
    grace_period_minutes: int = 5
    max_attempts: int = 5
    lockout_seconds: int = 30


class MqttTopics(BaseModel):
    commands: str
    events: str
    status: str


class MqttConfig(BaseModel):
    broker: str
    port: int
    client_id: str
    topics: MqttTopics


class DeviceRegisterResponse(BaseModel):
    status: str = "registered"
    device_uuid: str
    server_public_key: str
    device_secret: str
    config: DeviceConfig
    mqtt_config: MqttConfig


class DeviceStatusUpdate(BaseModel):
    msg_id: str
    device_uuid: str
    timestamp: int
    status: dict[str, Any]
    signature: str


class DeviceOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    device_uuid: uuid.UUID
    hardware_id: str
    firmware_version: str | None
    battery_level: int | None
    # is_online: bool
    last_seen: datetime | None
    config: dict[str, Any]
    created_at: datetime

class OkResponse(BaseModel):
    message: str


class EventOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    event_uuid: uuid.UUID
    msg_id: str
    device_uuid: uuid.UUID
    user_uuid: uuid.UUID | None = None
    event_type: str
    event_data: dict[str, Any]
    verified: bool
    created_at: datetime


class PinStateOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    device_uuid: uuid.UUID
    rotation_counter: int
    last_rotation_slot: int | None = None
    last_rotation_at: datetime | None = None
    updated_at: datetime | None = None


class DiagnosticsResponse(BaseModel):
    device_uuid: str
    # is_online: bool | None
    battery_level: int | None
    firmware_version: str | None
    last_seen: datetime | None
    last_time_sync: datetime | None
    pin_state: PinStateOut | None
    recent_events: list[EventOut]
    config: dict[str, Any]


class PinScheduleRequest(BaseModel):
    enabled: bool
    rotation_interval_hours: int = 24
    next_rotation_at: str


class PinScheduleResponse(BaseModel):
    enabled: bool
    rotation_interval_hours: int
    next_rotation_at: str | None
    current_pin: str | None
