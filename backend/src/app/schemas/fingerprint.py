from datetime import datetime
from pydantic import BaseModel


class FingerprintOut(BaseModel):
    model_config = {"from_attributes": True}

    finger_id: int
    name: str
    created_at: datetime


class FingerprintEnrollResponse(BaseModel):
    finger_id: int
    status: str = "enrollment_started"


class FingerprintRenameRequest(BaseModel):
    name: str
