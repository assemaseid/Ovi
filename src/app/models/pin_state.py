from datetime import datetime

from sqlalchemy import ForeignKey, String, func
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base, uuid_fk


class PinState(Base):
    __tablename__ = "pin_states"

    device_uuid: Mapped[uuid_fk] = mapped_column(
        ForeignKey("devices.device_uuid"),
        on_delete="CASCADE",
        primary_key=True,
    )
    current_pin_hash: Mapped[str] = mapped_column(String(128))
    valid_from: Mapped[datetime]
    valid_until: Mapped[datetime]
    grace_until: Mapped[datetime]
    rotation_counter: Mapped[int] = mapped_column(default=0)
    updated_at: Mapped[datetime] = mapped_column(server_default=func.now())

    # device: Mapped["Device"] = relationship(back_populates="pin_state")