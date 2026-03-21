from sqlalchemy import func, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import JSONB
from typing import Optional
from datetime import datetime
from src.database import Base, uuid_pk, uuid_fk


class Grant(Base):
    __tablename__ = "grants"

    grant_uuid: Mapped[uuid_pk]
    device_uuid: Mapped[uuid_fk] = mapped_column(
        ForeignKey("devices.device_uuid")
    )
    user_uuid: Mapped[uuid_fk] = mapped_column(ForeignKey("users.user_uuid"))
    permissions: Mapped[list] = mapped_column(
        JSONB, default=lambda: ["read_status", "unlock"]
    )
    valid_from: Mapped[datetime] = mapped_column(server_default=func.now())
    valid_until: Mapped[datetime]
    created_by: Mapped[uuid_fk] = mapped_column(ForeignKey("users.user_uuid"))
    created_at: Mapped[datetime] = mapped_column(server_default=func.now())

    # device: Mapped[Optional["Device"]] = relationship(back_populates="grants")
    # user: Mapped[Optional["User"]] = relationship(foreign_keys=[user_uuid], back_populates="grants")
    # creator: Mapped[Optional["User"]] = relationship(foreign_keys=[created_by], back_populates="grants_created")