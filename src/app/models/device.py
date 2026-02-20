from datetime import datetime
from typing import Optional

from sqlalchemy import ForeignKey, func
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import JSONB, INET
from src.database import Base, str_64, uuid_pk, str_20, uuid_fk


class Device(Base):
    __tablename__ = "devices"

    device_uuid: Mapped[uuid_pk]
    hardware_id: Mapped[str_64] = mapped_column(unique=True)
    public_key: Mapped[str]
    owner_uuid: Mapped[Optional[uuid_fk]] = mapped_column(ForeignKey("users.id"))
    config: Mapped[Optional[dict]] = mapped_column(JSONB, default=dict)
    firmware_version: Mapped[Optional[str_20]]
    last_seen: Mapped[Optional[datetime]]
    battery_level: Mapped[Optional[int]]
    wifi_ssid: Mapped[Optional[str_64]]
    ip_address: Mapped[Optional[str]] = mapped_column(INET)
    created_at: Mapped[datetime] = mapped_column(server_default=func.now())