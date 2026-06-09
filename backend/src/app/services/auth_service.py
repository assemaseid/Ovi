import uuid
from datetime import timedelta, datetime, UTC
from sqlalchemy import select

from src.database import SessionDep

from src.app.models.auth import TokenBlacklist
from src.app.models.user import User
from src.app.schemas.auth import TokenResponse
from src.app.schemas.user import UserResponseSchema
from src.config  import settings
from src.app.security import jwt_utils

TOKEN_TYPE_FIELD = "type" # nosec B105
_ACCESS_TOKEN_TYPE = "access" # nosec B105
_REFRESH_TOKEN_TYPE = "refresh" # nosec B105

def create_token(
    token_type: str,
    token_data: dict,
    expire_minutes: int = 0,
    expire_timedelta: timedelta | None = None,
    ) -> str:
    jwt_payload = {TOKEN_TYPE_FIELD: token_type}
    jwt_payload.update(token_data)
    return jwt_utils.encode_jwt(
        payload=jwt_payload,
        expire_minutes=expire_minutes,
        expire_timedelta=expire_timedelta,
        )

def create_access_token(user: UserResponseSchema) -> str:
    jti = str(uuid.uuid4())
    jwt_payload = {
        "sub": str(user.user_uuid),
        "email": user.email,
        "jti": jti,
        "iat": datetime.now(UTC),
    }
    return create_token(
        token_type=_ACCESS_TOKEN_TYPE,
        token_data=jwt_payload,
        expire_minutes=settings.auth_jwt.access_token_expire_minutes,
        )

def create_refresh_token(user: UserResponseSchema) -> str:
    jti = str(uuid.uuid4())
    jwt_payload = {
        "sub": str(user.user_uuid),
        "email": user.email,
        "jti": jti,
        "iat": datetime.now(UTC),
    }
    return create_token(
        token_type=_REFRESH_TOKEN_TYPE,
        token_data=jwt_payload,
        expire_timedelta=timedelta(days=settings.auth_jwt.refresh_token_expire_days),
        )

def create_token_pair(user: UserResponseSchema):
    access_token = create_access_token(user)
    refresh_token = create_refresh_token(user)
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user_data=user.model_dump(),
    )

async def get_user_by_email(session: SessionDep, email: str | None) -> User | None:
    result = await session.execute(select(User).where(User.email == email))
    return result.scalar_one_or_none()

async def get_user_by_uuid(session: SessionDep, user_uuid: str | None) -> User | None:
    result = await session.execute(select(User).where(
        User.user_uuid==uuid.UUID(str(user_uuid))))
    return result.scalar_one_or_none()


async def is_token_revoked(session: SessionDep, jti: str) -> bool:
    result = await session.execute(select(TokenBlacklist).where(
        TokenBlacklist.token_id==jti))
    return bool(result.scalar_one_or_none())