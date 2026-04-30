import uuid
from datetime import datetime
from typing import Optional

from sqlalchemy import String, ForeignKey, Integer, func, DateTime, Text
from sqlalchemy.dialects.postgresql import INET, UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.database import Base, uuid_pk, uuid_fk, str_64


class Device(Base):
    __tablename__ = "devices"

    device_uuid: Mapped[uuid_pk]
    hardware_id: Mapped[str] = mapped_column(unique=True, nullable=False)
    public_key: Mapped[str] = mapped_column(Text) #сервера

    # Владелец который зарегал замок в систему Ovi
    user_uuid: Mapped[uuid_fk] = mapped_column(ForeignKey("users.user_uuid"))
    config: Mapped[dict] = mapped_column(JSONB, default=dict)
    firmware_version: Mapped[Optional[str]]
    last_seen: Mapped[Optional[datetime]] = mapped_column(
        DateTime(timezone=True),
        default=None,
    )
    last_time_sync: Mapped[Optional[datetime]] = mapped_column(
        DateTime(timezone=True),
        default=None,
    )
    battery_level: Mapped[Optional[int]]
    # is_online: Mapped[bool] = mapped_column(default=False)
    wifi_ssid: Mapped[Optional[str_64]]
    ip_address: Mapped[Optional[str]] = mapped_column(INET, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
                 server_default=func.now(),
                 )
