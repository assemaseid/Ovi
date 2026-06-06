import logging
import time
import uuid as uuid_lib

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select

from src.app.api.dependencies import require_device_permission
from src.app.models.fingerprint import Fingerprint
from src.app.schemas.fingerprint import FingerprintEnrollResponse, FingerprintOut, FingerprintRenameRequest
from src.app.services.crypto_service import crypto_service
from src.app.services.mqtt_service import MQTTService
from src.database import SessionDep
from src.dependencies import get_current_user
from src.app.models.user import User

router = APIRouter(prefix="/devices", tags=["Fingerprints"])
logger = logging.getLogger(__name__)

MAX_FINGER_SLOTS = 127


@router.get("/{device_uuid}/fingerprints", response_model=list[FingerprintOut])
async def list_fingerprints(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> list[FingerprintOut]:
    device = await require_device_permission(device_uuid, "admin", current_user, session)
    rows = await session.execute(
        select(Fingerprint)
        .where(Fingerprint.device_uuid == device.device_uuid)
        .order_by(Fingerprint.finger_id)
    )
    return [FingerprintOut.model_validate(f) for f in rows.scalars().all()]


@router.post("/{device_uuid}/fingerprints/enroll", response_model=FingerprintEnrollResponse)
async def enroll_fingerprint(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> FingerprintEnrollResponse:
    device = await require_device_permission(device_uuid, "admin", current_user, session)

    used_q = await session.execute(
        select(Fingerprint.finger_id).where(Fingerprint.device_uuid == device.device_uuid)
    )
    used_ids = set(used_q.scalars().all())
    next_id = next((i for i in range(1, MAX_FINGER_SLOTS + 1) if i not in used_ids), None)
    if next_id is None:
        raise HTTPException(status_code=409, detail="All fingerprint slots are full (max 127)")

    mqtt = MQTTService()
    if not mqtt.is_connected:
        raise HTTPException(status_code=503, detail="Device is offline — cannot start enrollment")

    canonical = f'{{"device_uuid":"{device_uuid}","finger_id":{next_id},"type":"finger_enroll"}}'
    signature = crypto_service.sign_dict({
        "device_uuid": device_uuid,
        "finger_id": next_id,
        "type": "finger_enroll",
    })
    payload = {
        "cmd": "finger_enroll",
        "msg_id": f"msg_{uuid_lib.uuid4()}",
        "timestamp": int(time.time()),
        "finger_id": next_id,
        "signature": signature,
    }
    await mqtt.publish(payload=payload, packetId=0, topicName=MQTTService.cmd_topic(device_uuid))
    logger.info("finger_enroll sent to device=%s slot=%d", device_uuid, next_id)

    return FingerprintEnrollResponse(finger_id=next_id, status="enrollment_started")


@router.patch("/{device_uuid}/fingerprints/{finger_id}/name", response_model=FingerprintOut)
async def rename_fingerprint(
    device_uuid: str,
    finger_id: int,
    body: FingerprintRenameRequest,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> FingerprintOut:
    device = await require_device_permission(device_uuid, "admin", current_user, session)
    row = await session.execute(
        select(Fingerprint).where(
            Fingerprint.device_uuid == device.device_uuid,
            Fingerprint.finger_id == finger_id,
        )
    )
    fp = row.scalar_one_or_none()
    if not fp:
        raise HTTPException(status_code=404, detail="Fingerprint not found")

    fp.name = body.name[:64]
    await session.commit()
    return FingerprintOut.model_validate(fp)


@router.delete("/{device_uuid}/fingerprints/{finger_id}", status_code=200)
async def delete_fingerprint(
    device_uuid: str,
    finger_id: int,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> dict:
    device = await require_device_permission(device_uuid, "admin", current_user, session)
    row = await session.execute(
        select(Fingerprint).where(
            Fingerprint.device_uuid == device.device_uuid,
            Fingerprint.finger_id == finger_id,
        )
    )
    fp = row.scalar_one_or_none()
    if not fp:
        raise HTTPException(status_code=404, detail="Fingerprint not found")

    await session.delete(fp)
    await session.commit()

    mqtt = MQTTService()
    if mqtt.is_connected:
        signature = crypto_service.sign_dict({
            "device_uuid": device_uuid,
            "finger_id": finger_id,
            "type": "finger_delete",
        })
        payload = {
            "cmd": "finger_delete",
            "msg_id": f"msg_{uuid_lib.uuid4()}",
            "timestamp": int(time.time()),
            "finger_id": finger_id,
            "signature": signature,
        }
        await mqtt.publish(payload=payload, packetId=0, topicName=MQTTService.cmd_topic(device_uuid))
        logger.info("finger_delete sent to device=%s slot=%d", device_uuid, finger_id)
    else:
        logger.warning("finger_delete: device=%s offline, slot %d removed from DB only", device_uuid, finger_id)

    return {"status": "deleted", "finger_id": finger_id}
