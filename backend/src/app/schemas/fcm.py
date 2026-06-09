from typing import Any

from pydantic import BaseModel


class FcmTokenUpdate(BaseModel):
    fcm_token: str


class PushNotificationRequest(BaseModel):
    title: str
    body: str
    data: dict[str, Any] | None = None
