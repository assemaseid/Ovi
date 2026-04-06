from datetime import UTC, datetime

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import PyJWTError
from sqlalchemy.ext.asyncio import AsyncSession
from src.database import SessionDep

from src.app.models.user import User
from src.app.services.auth_service import is_token_revoked, get_user_by_uuid
from src.database import SessionDep
from src.app.security.jwt_utils import decode_jwt

bearer_scheme = HTTPBearer(auto_error=True)

async def get_current_user(
    session: SessionDep,
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
) -> User:
    token = credentials.credentials
    exc = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or expired token",
        headers={"WWW-Authenticate": "Bearer"},
    )

    try:
        payload = decode_jwt(token)
    # только JWT ошибки → 401, остальное → 500
    except PyJWTError:
        raise exc

    if payload.get("type") != "access":
        raise exc

    jti = payload.get("jti")
    if jti and is_token_revoked(session, jti):
        raise exc

    user_uuid_str: str | None = payload.get("sub")
    if not user_uuid_str:
        raise exc

    user = get_user_by_uuid(session, user_uuid_str)
    if not user:
        raise exc

    # Update last_login
    user.last_login = datetime.now(UTC)
    return user
