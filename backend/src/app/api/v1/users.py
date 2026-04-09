from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from src.app.models.user import User
from src.database import SessionDep
from src.dependencies import get_current_user

router = APIRouter(prefix="/users", tags=["Users"])


class FcmTokenUpdate(BaseModel):
    fcm_token: str


@router.put("/me/fcm-token", status_code=204)
async def update_fcm_token(
    body: FcmTokenUpdate,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> None:
    """Called by the mobile app after receiving a new FCM token from Firebase SDK."""
    current_user.fcm_token = body.fcm_token
    session.add(current_user)
    await session.commit()
