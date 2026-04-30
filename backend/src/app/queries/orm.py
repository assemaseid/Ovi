from datetime import datetime, timezone

from src.app.models.user import User
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

            result = await session.execute(select(User).where(User.email == "user1@gmail.com"))
            existing_user = result.scalars().first()

            if not existing_user:
                user1 = User(name="User1", email="user1@gmail.com", hashed_password=hash_password("12345678"),
                             public_key="temp1", fcm_token="temp1",
                             last_login=datetime.now(timezone.utc))
                user2 = User(name="User2", email="user2@gmail.com", hashed_password=hash_password("87654321"),
                             public_key="temp1", fcm_token="temp1",
                             last_login=datetime.now(timezone.utc))
                session.add_all([user1, user2])
                await session.commit()
