from pydantic import BaseModel


class UnlockRequest(BaseModel):
    device_uuid: str


class TokenData(BaseModel):
    version: int
    device_uuid: str
    user_uuid: str
    action: str
    nonce: str
    issued_at: int
    expires_at: int
    session_id: str


class SignatureData(BaseModel):
    algorithm: str
    curve: str
    value: str
    public_key_id: str


class UnlockResponse(BaseModel):
    token: TokenData
    signature: SignatureData


class MqttCommand(BaseModel):
    msg_id: str
    timestamp: int
    command: dict
    signature: str
