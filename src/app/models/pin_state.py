from datetime import datetime
from typing import Optional

from sqlalchemy import ForeignKey, func, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base, uuid_fk


class PinState(Base):
    __tablename__ = "pin_states"

    device_uuid: Mapped[uuid_fk] = mapped_column(
        ForeignKey("devices.device_uuid", ondelete="CASCADE"),
        primary_key=True,
    )
    rotation_counter: Mapped[int] = mapped_column(default=0)
    last_rotation_slot: Mapped[Optional[int]] = mapped_column(nullable=True)
    last_rotation_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now())