from datetime import datetime
from typing import Optional

from sqlalchemy import ForeignKey, func
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import JSONB, INET
from src.database import Base, str_64, uuid_pk, str_20, uuid_fk, str_256


class Device(Base):
    __tablename__ = "devices"

    #IDs
    device_uuid: Mapped[uuid_pk]
    hardware_id: Mapped[str_64] = mapped_column(unique=True)

    owner_uuid: Mapped[Optional[uuid_fk]] = mapped_column(ForeignKey("users.id"))

    #Crypto
    public_key: Mapped[str]

    #Network
    last_seen: Mapped[Optional[datetime]]
    wifi_ssid: Mapped[Optional[str_64]]
    ip_address: Mapped[Optional[str]] = mapped_column(INET)

    #Device Info
    name: Mapped[str_256]


    config: Mapped[Optional[dict]] = mapped_column(JSONB, default=dict)
    firmware_version: Mapped[Optional[str_20]]

    #Status
    is_active: Mapped[bool] = mapped_column(default=True)
    battery_level: Mapped[Optional[int]]

    # PIN Configuration
    pin_length: Mapped[int] = mapped_column(default=6)
    pin_rotation_seconds: Mapped[int] = mapped_column(default=3600)  # 1 hour
    show_pin_on_display: Mapped[bool] = mapped_column(default=False)

    # MQTT
    mqtt_connected: Mapped[bool] = mapped_column(default=False)
    mqtt_last_connected_at: Mapped[Optional[datetime]]
    mqtt_last_disconnected_at: Mapped[Optional[datetime]]

    # Последняя телеметрия, пришедшая по MQTT (кэш, чтобы не лезть в Redis/брокер)
    mqtt_last_telemetry: Mapped[Optional[dict]] = mapped_column(JSONB, default=None)
    mqtt_last_telemetry_at: Mapped[Optional[datetime]]

    created_at: Mapped[datetime] = mapped_column(server_default=func.now())
