import uuid
from datetime import datetime
from typing import Optional

from psycopg2.extensions import JSONB
from sqlalchemy import String, ForeignKey, Integer, func
from sqlalchemy.dialects.postgresql import INET, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.database import Base, uuid_pk, uuid_fk, str_64


class Device(Base):
    __tablename__ = "devices"

    device_uuid: Mapped[uuid_pk]
    hardware_id: Mapped[str]
    public_key: Mapped[str]
    owner_uuid: Mapped[uuid_fk] = mapped_column(ForeignKey("users.user_uuid"))
    config: Mapped[dict] = mapped_column(JSONB, default=dict)
    firmware_version: Mapped[str]
    last_seen: Mapped[datetime] = mapped_column(default=None)
    battery_level: Mapped[int]
    wifi_ssid: Mapped[Optional[str]] = mapped_column(str_64)
    ip_address: Mapped[str] = mapped_column(INET)
    created_at: Mapped[datetime] = mapped_column(server_default=func.now())

    # owner: Mapped[Optional["User"]] = relationship(back_populates="devices")
    # grants: Mapped[list["Grant"]] = relationship(back_populates="device")
    # events: Mapped[list["Event"]] = relationship(back_populates="device")
    # pin_state: Mapped[Optional["PinState"]] = relationship(back_populates="device", uselist=False)
