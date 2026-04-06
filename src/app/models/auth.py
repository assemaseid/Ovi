from datetime import datetime
from typing import Optional

from sqlalchemy import Integer, String, DateTime, func, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base, uuid_fk, str_64,str_256


class TokenBlacklist(Base):
    __tablename__ = "revoked_tokens"

    token_id: Mapped[str_64] = mapped_column(String(64), primary_key=True)  # jti
    token_hash: Mapped[str_256] = mapped_column(unique=True, index=True)
    user_uuid: Mapped[uuid_fk] = mapped_column(ForeignKey("users.user_uuid"))
    revoked_at: Mapped[datetime] = mapped_column(DateTime(timezone=True),
                                                 server_default=func.now())
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True),
                                                 index=True)