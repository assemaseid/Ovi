from fastapi import Depends
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import String, Uuid
from .config import settings

from typing import Annotated

from sqlalchemy import UUID
from sqlalchemy.orm import mapped_column

import uuid as uuid_lib
from uuid import uuid4

engine = create_async_engine(
    url=settings.db_url,
    pool_pre_ping = True,
    echo=True,
    pool_size=5,
    max_overflow=10,
    )

async_session_factory = async_sessionmaker(
    engine,
    expire_on_commit=False,
    autoflush=False,
    autocommit=False,
    )

str_256 = Annotated[str, 256]
str_64 = Annotated[str, 64]
str_20 = Annotated[str, 20]
str_50 = Annotated[str, 50]
uuid_pk = Annotated[
    uuid_lib.UUID,
    mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid4)
]
uuid_fk = Annotated[
    uuid_lib.UUID,
    mapped_column(UUID(as_uuid=True)),
]


class Base(DeclarativeBase):
    __abstract__ = True
    
    type_annotation_map = {
        str_256: String(256),
        str_64: String(64),
        str_20: String(20),
        str_50: String(50),
    }
    
    
async def get_db():
    async with async_session_factory() as session:
        yield session
    
SessionDep = Annotated[AsyncSession, Depends(get_db)]

