from datetime import datetime

from sqlalchemy import ForeignKey, String, func, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base, uuid_fk


class PinState(Base):
    __tablename__ = "pin_states"

    device_uuid: Mapped[uuid_fk] = mapped_column(
        ForeignKey("devices.device_uuid"),
        on_delete="CASCADE",
        primary_key=True,
    )
    rotation_counter: Mapped[int] = mapped_column(default=0)
    last_rotation_slot: Mapped[int]
    last_rotation_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now())