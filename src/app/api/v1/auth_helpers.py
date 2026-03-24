from datetime import timedelta

from src.app.schemas.auth import TokenResponse
from src.app.schemas.user import UserResponseSchema
from src.config  import settings
from src.app.security import jwt_utils

TOKEN_TYPE_FIELD = "type"
ACCESS_TOKEN_TYPE = "access"
REFRESH_TOKEN_TYPE = "refresh"

def create_token(
    token_type: str,
    token_data: dict, 
    expire_minutes: int = settings.auth_jwt.access_token_expire_minutes,
    expire_timedelta: timedelta | None = None , 
    ) -> str:
    jwt_payload = {TOKEN_TYPE_FIELD: token_type}
    jwt_payload.update(token_data)
    return jwt_utils.encode_jwt(
        payload=jwt_payload,
        expire_minutes=expire_minutes,
        expire_timedelta=expire_timedelta,
        )
    

def create_access_token(user: UserResponseSchema) -> str:
    jwt_payload = {
        "sub": str(user.user_uuid),
        "email": user.email,
    }
    
    return create_token(
        token_type=ACCESS_TOKEN_TYPE,
        token_data=jwt_payload,
        expire_minutes=settings.auth_jwt.access_token_expire_minutes
        )


def create_refresh_token(user: UserResponseSchema) -> str:
    
    jwt_payload = {
        "sub": str(user.user_uuid),
    } 
    return create_token(token_type=REFRESH_TOKEN_TYPE,
                        token_data=jwt_payload,
                        expire_timedelta=timedelta(
                            days=settings.auth_jwt.refresh_token_expire_days),
                         )

def create_token_pair(user: UserResponseSchema):
    access_token = create_access_token(user)
    refresh_token = create_refresh_token(user)
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user_data=user.model_dump(),
    )