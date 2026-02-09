from fastapi import (
    APIRouter, 
    Depends,
    Form,
    HTTPException,
    status,
    )

from src.app.schemas.auth import (
    TokenResponse,
    LoginSchema,
    RefreshTokenResponse,
)
from src.app.schemas.user import UserCreateSchema, UserResponseSchema
from src.app.security import jwt_password
from fastapi.security import HTTPBearer, OAuth2PasswordBearer

from .auth_helpers import (
    create_access_token,
    create_refresh_token,
)
from src.database import SessionDep
from src.app.models.user import User
from sqlalchemy import select

http_bearer = HTTPBearer(auto_error=False)
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/jwt/login_user/")
    
router = APIRouter(prefix="/jwt", 
                   tags=["JWT"],
                   dependencies=[Depends(http_bearer)])


async def validate_user_auth(
        session: SessionDep,
        username: str = Form(),
        password: str = Form(),
):
    unauthed_exec = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="invalid username or password",
    )

    result = await session.execute(select(User).where(User.username == username))
    user = result.scalars().first()

    if not user:
        raise unauthed_exec
    
    if not jwt_password.validate_pwd(
             password=password,
             hashed_password=user.password
             ):
        raise unauthed_exec
    
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="user inactive",
        )
         
    return user 

@router.post("/register_user/",
             response_model=UserResponseSchema,
             status_code=status.HTTP_201_CREATED
             )
async def register_user(
        user_data: UserCreateSchema,
        session: SessionDep,
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
        username=user_data.username,
        password=hashed_password,
        email=user_data.email,
    )
    session.add(new_user)
    await session.commit()
    await session.refresh(new_user)
    return new_user

@router.post("/login_user/")
async def auth_user(user: UserResponseSchema = Depends(validate_user_auth)):
    
    access_token = create_access_token(user)
    refresh_token = create_refresh_token(user)
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
    )