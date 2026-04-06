from datetime import datetime
from typing import Optional

from sqlalchemy import func, Text, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from src.database import Base, uuid_pk


class User(Base):
    __tablename__ = "users"

    user_uuid: Mapped[uuid_pk]
    email: Mapped[str] = mapped_column(unique=True)
    hashed_password: Mapped[str]
    public_key: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    fcm_token: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
    )
    last_login: Mapped[Optional[datetime]] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    is_active: Mapped[bool] = mapped_column(default=True)


