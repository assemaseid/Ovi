from datetime import datetime

from sqlalchemy import ForeignKey, Integer, String, DateTime, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base, uuid_pk, uuid_fk


class Fingerprint(Base):
    __tablename__ = "fingerprints"

    id: Mapped[uuid_pk]
    device_uuid: Mapped[uuid_fk] = mapped_column(
        ForeignKey("devices.device_uuid", ondelete="CASCADE")
    )
    finger_id: Mapped[int] = mapped_column(Integer, nullable=False)
    name: Mapped[str] = mapped_column(String(64), default="")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    __table_args__ = (UniqueConstraint("device_uuid", "finger_id", name="uq_device_finger"),)
