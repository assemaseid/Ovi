from datetime import datetime
from typing import Optional
from sqlalchemy import Boolean, Text, ForeignKey, func
from sqlalchemy.dialects.postgresql import JSONB, INET
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base, uuid_pk, uuid_fk, str_50


class Event(Base):
    __tablename__ = "events"

    event_uuid: Mapped[uuid_pk]
    device_uuid: Mapped[Optional[uuid_fk]] = mapped_column(
        ForeignKey("devices.device_uuid")
    )
    user_id: Mapped[Optional[uuid_fk]] = mapped_column(
        ForeignKey("users.id")
    )
    event_type: Mapped[str_50]
    event_data: Mapped[dict] = mapped_column(JSONB, nullable=False)
    signature: Mapped[Optional[str]] = mapped_column(Text)
    verified: Mapped[bool] = mapped_column(Boolean, default=False)
    source_ip: Mapped[Optional[str]] = mapped_column(INET)
    created_at: Mapped[datetime] = mapped_column(server_default=func.now())