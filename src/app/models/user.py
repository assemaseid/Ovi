from datetime import datetime

from sqlalchemy import func
from sqlalchemy.orm import Mapped, mapped_column
from src.database import Base, uuid_pk


class User(Base):
    __tablename__ = "users"

    user_uuid: Mapped[uuid_pk]
    email: Mapped[str] = mapped_column(unique=True)
    name: Mapped[str]
    password: Mapped[str]

    public_key: Mapped[str | None]
    created_at: Mapped[datetime] = mapped_column(server_default=func.now())
    last_login: Mapped[datetime | None] = mapped_column(default=None)
    is_active: Mapped[bool] = mapped_column(default=True)

    # devices: Mapped[list["Device"]] = relationship(back_populates="owner")
    # grants: Mapped[list["Grant"]] = relationship(foreign_keys="Grant.user_uuid", back_populates="user")
    # grants_created: Mapped[list["Grant"]] = relationship(foreign_keys="Grant.created_by", back_populates="creator")
    # events: Mapped[list["Event"]] = relationship(back_populates="user")
    # revoked_tokens: Mapped[list["RevokedToken"]] = relationship(back_populates="user")


