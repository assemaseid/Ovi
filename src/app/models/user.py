from datetime import datetime

from sqlalchemy import func, Text, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from src.database import Base, uuid_pk


class User(Base):
    __tablename__ = "users"

    user_uuid: Mapped[uuid_pk]
    email: Mapped[str] = mapped_column(unique=True)
    hashed_password: Mapped[str]
    public_key: Mapped[str] = mapped_column(Text)
    fcm_token: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
    )
    last_login: Mapped[datetime] = mapped_column(
        DateTime(timezone=True)
    )
    is_active: Mapped[bool] = mapped_column(default=True)


