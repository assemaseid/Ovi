import time
import uuid
import logging

from fastapi import APIRouter, Depends

from src.app.api.dependencies import require_device_permission
from src.app.schemas.commands import (
    MqttCommand,
    SignatureData,
    TokenData,
    UnlockRequest,
    UnlockResponse,
)
from src.app.services.crypto_service import crypto_service
from src.app.services.mqtt_service import MQTTService
from src.config import settings
from src.database import SessionDep
from src.dependencies import get_current_user
from src.app.models.user import User

router = APIRouter(prefix="/commands", tags=["Сommands"])
logger = logging.getLogger(__name__)


@router.post("/unlock", response_model=UnlockResponse)
async def request_unlock_token(
    body: UnlockRequest,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> UnlockResponse:

    device = await require_device_permission(
        body.device_uuid, "unlock", current_user, session
    )

    now_timestamp = int(time.time())
    nonce = crypto_service.generate_nonce(8)
    expires_at = now_timestamp + settings.unlock_token_ttl_seconds
    session_id = f"sess_{uuid.uuid4().hex[:10]}"

    token = TokenData(
        version=1,
        device_uuid=str(device.device_uuid),
        user_uuid=str(current_user.user_uuid),
        action="unlock",
        nonce=nonce,
        issued_at=now_timestamp,
        expires_at=expires_at,
        session_id=session_id,
    )

    signature_value = crypto_service.sign_dict(token.model_dump())

    sig = SignatureData(
        algorithm="ECDSA-SHA256",
        curve="P-256",
        value=signature_value,
        public_key_id="server_key_1",
    )

    mqtt = MQTTService()
    if mqtt.is_connected:
        cmd_msg = MqttCommand(
            msg_id=f"msg_{uuid.uuid4()}",
            timestamp=now_timestamp,
            command={
                "type": "unlock",
                "token": token.model_dump(),
            },
            signature=signature_value,
        )
        try:
            await mqtt.publish(
                payload=cmd_msg.model_dump(),
                packetId=0,
                topicName=MQTTService.cmd_topic(str(device.device_uuid)),
                qos=1,
            )
        except Exception as e:
            logger.warning("MQTT publish failed: %s", e)

    return UnlockResponse(token=token, signature=sig)
