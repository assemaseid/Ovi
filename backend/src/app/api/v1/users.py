from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import Any

from src.app.models.user import User
from src.app.schemas.device import OkResponse
from src.app.services.fcm_service import send_notification
from src.database import SessionDep
from src.dependencies import get_current_user

router = APIRouter(prefix="/users", tags=["Users"])


class FcmTokenUpdate(BaseModel):
    fcm_token: str


class PushNotificationRequest(BaseModel):
    title: str
    body: str
    data: dict[str, Any] | None = None


@router.put("/me/fcm-token", status_code=204)
async def update_fcm_token(
    body: FcmTokenUpdate,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> None:
    current_user.fcm_token = body.fcm_token
    session.add(current_user)
    await session.commit()


@router.post("/me/test-notification", response_model=OkResponse)
async def send_test_notification(
    body: PushNotificationRequest,
    current_user: User = Depends(get_current_user),
) -> OkResponse:
    if not current_user.fcm_token:
        raise HTTPException(status_code=400, detail="No FCM token registered for this user")

    sent = await send_notification(
        fcm_token=current_user.fcm_token,
        title=body.title,
        body=body.body,
        data=body.data,
    )
    if not sent:
        raise HTTPException(status_code=502, detail="Failed to send push notification")

    return OkResponse(message="Notification sent")
