from typing import Annotated
from fastapi import Depends
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import String
from .config import settings

engine = create_async_engine(
    url=settings.DATABASE_URL_asyncpg,
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

str_256 = Annotated[str,256]


class Base(DeclarativeBase):
    __abstract__ = True
    
    type_annotation_map = {
        str_256: String(256)
    }
    
    
async def get_db():
    async with async_session_factory() as session:
        yield session
    
SessionDep = Annotated[AsyncSession, Depends(get_db)]

