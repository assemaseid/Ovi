from datetime import datetime
from typing import Optional

from sqlalchemy import Integer, String, DateTime, func, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base, uuid_fk


class TokenBlacklist(Base):
    __tablename__ = "token_blacklist"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    token_hash: Mapped[str] = mapped_column(String(256), unique=True, index=True)
    user_uuid: Mapped[Optional[uuid_fk]] = mapped_column(ForeignKey("users.user_uuid"))
    expires_at: Mapped[DateTime] = mapped_column(DateTime, index=True)
    revoked_at: Mapped[DateTime] = mapped_column(DateTime, server_default=func.now())

    # user: Mapped[Optional["User"]] = relationship(back_populates="revoked_tokens")