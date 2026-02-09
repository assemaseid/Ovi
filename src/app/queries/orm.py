from src.app.models.user import User
from src.app.security import jwt_password
from src.app.security.jwt_password import hash_password
from src.database import engine, Base, async_session_factory
from sqlalchemy import select

class AsyncOrm:
    @staticmethod
    async def create_tables():
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)

    @staticmethod
    async def insert_users():
        async with async_session_factory() as session:

            result = await session.execute(select(User).where(User.username == "user1"))
            existing_user = result.scalars().first()

            if not existing_user:
                user1 = User(username="user1", email="user1@gmail.com", password=hash_password("12345678"))
                user2 = User(username="user2", email="user2@gmail.com", password=hash_password("87654321"))
                session.add_all([user1, user2])
                await session.commit()
