from datetime import datetime

from sqlalchemy import ForeignKey, Text, DateTime, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped
from sqlalchemy.testing.schema import mapped_column

from src.database import Base, uuid_fk, uuid_pk

class OfflineEventsQueue(Base):
    __tablename__ = "offline_events_queue"

    queue_uuid: Mapped[uuid_pk]
    device_uuid: Mapped[uuid_fk] = mapped_column(ForeignKey("devices.device_uuid"))
    event_data: Mapped[dict] = mapped_column(JSONB, nullable=False)
    signature: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True),
                                                 server_default=func.now())
    synced_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
