import hashlib
from datetime import datetime

from fastapi import (
    APIRouter, 
    Depends,
    HTTPException,
    status,
    )

from src.app.schemas.auth import (
    LoginSchema,
    RefreshTokenResponse,
    RefreshTokenRequest,
)
from src.app.schemas.user import UserCreateSchema, UserResponseSchema
from src.app.security import jwt_password, jwt_utils
from fastapi.security import HTTPBearer, OAuth2PasswordBearer
from .auth_helpers import (
    create_token_pair,
    create_access_token
)
from src.database import SessionDep
from src.app.models.user import User
from sqlalchemy import select

from ...models.auth import TokenBlacklist

http_bearer = HTTPBearer(auto_error=False)
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/jwt/login_user/")
    
router = APIRouter(prefix="/auth",
                   tags=["JWT"],
                   dependencies=[Depends(http_bearer)])


async def validate_user_auth(
        session: SessionDep,
        login_data: LoginSchema,
) -> UserResponseSchema:
    unauthed_exec = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="invalid email or password",
    )

    result = await session.execute(select(User).where(User.email == login_data.email))
    user = result.scalars().first()

    if not user:
        raise unauthed_exec
    
    if not jwt_password.validate_pwd(
             password=login_data.password,
             hashed_password=user.password
             ):
        raise unauthed_exec
    
    # if not user.is_active:
    #     raise HTTPException(
    #         status_code=status.HTTP_403_FORBIDDEN,
    #         detail="user inactive",
    #     )
         
    return UserResponseSchema.model_validate(user)

@router.post("/register_user/", status_code=status.HTTP_201_CREATED)
async def register_user(
        session: SessionDep,
        user_data: UserCreateSchema,
):
    result = await session.execute(select(User).where(User.email == user_data.email))
    existing_email = result.scalars().first()

    if existing_email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="email already exist",
        )

    hashed_password = jwt_password.hash_password(user_data.password)

    new_user = User(
        name=user_data.name,
        password=hashed_password,
        email=user_data.email,
    )
    session.add(new_user)
    await session.commit()
    await session.refresh(new_user)
    user_response = UserResponseSchema.model_validate(new_user)

    return create_token_pair(user=user_response)


@router.post("/login_user/")
async def auth_user(user: UserResponseSchema = Depends(validate_user_auth)):
    return create_token_pair(user=user)


@router.post("/refresh/", response_model=RefreshTokenResponse)
async def refresh_access_token(session: SessionDep,
                               refresh_token_request: RefreshTokenRequest):
    try:
        payload = jwt_utils.decode_jwt(refresh_token_request.refresh_token)
        print(payload, "\n\n\n\n")

        if payload.get("type") != "refresh":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="invalid token type",
            )

        user_id = int(payload.get("sub"))
        result = await session.execute(select(User).where(User.id == user_id))
        user = result.scalars().first()

        if not user:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="user not found",
            )

        user_response = UserResponseSchema.model_validate(user)
        new_access_token = create_access_token(user_response)

        return RefreshTokenResponse(access_token=new_access_token)

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
        )

"""Logout с добавлнием refresh_token в blacklist"""
@router.post("/logout/")
async def logout(session: SessionDep,
                 refresh_token_request: RefreshTokenRequest):
    ...
    try:
        payload = jwt_utils.decode_jwt(refresh_token_request.refresh_token)
        user_id = int(payload.get("sub"))
        exp_timestamp = payload.get("exp")

        token_hash = hashlib.sha256(
            refresh_token_request.refresh_token.encode()
        ).hexdigest()

        existing = await session.execute(select(TokenBlacklist)
                                   .where(TokenBlacklist.token_hash == token_hash)
                                   )

        if not existing.scalars().first():
            blacklisted = TokenBlacklist(
                user_id=user_id,
                token_hash=token_hash,
                expires_at=datetime.fromtimestamp(exp_timestamp),
            )
        session.add(blacklisted)
        await session.commit()

        return {
            "message": "Successfully logged out",
            "user_id": f"{user_id}",
        }

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Logout failed: {str(e)}"
        )