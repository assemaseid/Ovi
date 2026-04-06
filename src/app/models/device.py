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
    public_key: Mapped[str] = mapped_column(Text)

    owner_uuid: Mapped[uuid_fk] = mapped_column(ForeignKey("users.user_uuid"))
    config: Mapped[dict] = mapped_column(JSONB, default=dict)
    firmware_version: Mapped[str]
    last_seen: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), # включаем таймзону
        default=None,
    )
    last_time_sync: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=None,
    )
    battery_level: Mapped[int]
    # is_online: Mapped[bool] = mapped_column(default=False)
    wifi_ssid: Mapped[Optional[str_64]]
    ip_address: Mapped[str] = mapped_column(INET)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
                 server_default=func.now(),
                 )
