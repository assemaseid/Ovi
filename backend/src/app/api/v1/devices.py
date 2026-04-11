import logging

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select

from src.database import SessionDep
from src.dependencies import get_current_user
from src.app.api.dependencies import require_device_permission
from src.app.models.device import Device
from src.app.models.event import Event
from src.app.models.grant import Grant
from src.app.models.pin_state import PinState
from src.app.models.user import User
from src.app.schemas.device import (
    DeviceConfig,
    DeviceOut,
    DeviceRegisterRequest,
    DeviceRegisterResponse,
    DiagnosticsResponse,
    EventOut,
    MqttConfig,
    MqttTopics,
    OkResponse,
    PinStateOut,
)
from src.app.services.crypto_service import crypto_service
from src.app.services.mqtt_service import MQTTService
from src.app.services.pin_service import check_and_rotate, get_current_pin
from src.config import settings

router = APIRouter(prefix="/devices", tags=["Devices"])
logger = logging.getLogger(__name__)


def _cfg(attr: str, default):
    return getattr(settings, attr, default)


@router.post(
    "/register_device",
    status_code=status.HTTP_201_CREATED,
    response_model=DeviceRegisterResponse,
    summary="Register Device as Owner"
)
async def register_device(
    body: DeviceRegisterRequest,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> DeviceRegisterResponse:

    existing = await session.execute(
        select(Device).where(Device.hardware_id == body.device.hardware_id)
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="Device hardware_id already registered")

    if str(current_user.user_uuid) != body.owner_info.user_uuid:
        raise HTTPException(status_code=403, detail="owner_info.owner_uuid mismatch")

    pin_length = _cfg("pin_length", 6)
    rotation_hours = _cfg("pin_rotation_seconds", 86400) // 3600
    grace_period_minutes = _cfg("pin_grace_period_seconds", 300) // 60
    max_attempts = _cfg("pin_max_attempts", 5)
    lockout_seconds = _cfg("pin_lockout_seconds", 30)

    device = Device(
        hardware_id=body.device.hardware_id,
        public_key=body.device.public_key,
        user_uuid=current_user.user_uuid,
        config={
            "type": body.device.type,
            "capabilities": body.device.capabilities,
            "location": body.owner_info.location,
            "timezone": body.owner_info.timezone,
            "pin_length": pin_length,
            "rotation_hours": rotation_hours,
            "grace_period_minutes": grace_period_minutes,
            "max_attempts": max_attempts,
            "lockout_seconds": lockout_seconds,
        },
    )
    session.add(device)
    await session.flush()

    session.add(Grant(
        device_uuid=device.device_uuid,
        user_uuid=current_user.user_uuid,
        permissions=["read_status", "unlock", "lock", "admin"],
        created_by=current_user.user_uuid,
    ))
    session.add(PinState(device_uuid=device.device_uuid))

    await session.commit()
    await session.refresh(device)

    dev_uuid_str = str(device.device_uuid)

    mqtt = MQTTService()
    if mqtt.is_connected:
        try:
            await mqtt.subscribe_device(dev_uuid_str)
        except Exception as e:
            logger.warning("MQTT subscribe failed for new device %s: %s",
                           dev_uuid_str, e)

    return DeviceRegisterResponse(
        status="registered",
        device_uuid=dev_uuid_str,
        server_public_key=crypto_service.public_key_pem,
        config=DeviceConfig(
            pin_length=pin_length,
            rotation_hours=rotation_hours,
            grace_period_minutes=grace_period_minutes,
            max_attempts=max_attempts,
            lockout_seconds=lockout_seconds,
        ),
        mqtt_config=MqttConfig(
            broker=_cfg("MQTT_HOST", "localhost"),
            port=_cfg("MQTT_PORT", 8883),
            client_id=dev_uuid_str,
            topics=MqttTopics(
                commands=MQTTService.cmd_topic(dev_uuid_str),
                events=MQTTService.events_topic(dev_uuid_str),
                status=MQTTService.status_topic(dev_uuid_str),
            ),
        ),
    )

"""owned — замки которые ты купил и зарегистрировал, ты хозяин.                                                                                                                
  granted — замки чужие, но хозяин дал тебе ключ (временный или постоянный). """
@router.get("", response_model=list[DeviceOut])
async def list_devices(
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> list[DeviceOut]:
    # owned
    query = await session.execute(
        select(Device).where(Device.user_uuid == current_user.user_uuid)
    )
    owned = query.scalars().all()

    # granted
    """Это место используется если владелец захочет быть клиентом другого замка, 
    например ты владелец замка в офисе, но сосед дал тебе доступ к    
  замку в подъезде и владелец замка не будет видеть в списке свой замок дважды (как владелец и как клиент)"""
    grant_query = await session.execute(
        select(Grant).where(
            Grant.user_uuid == current_user.user_uuid,
            Grant.device_uuid.not_in([d.device_uuid for d in owned]),
        )
    )
    granted_device_ids = [g.device_uuid for g in grant_query.scalars().all()]

    granted_devices: list[Device] = []
    if len(granted_device_ids) > 0:
        device_query = await session.execute(select(Device).where(
            Device.device_uuid.in_(granted_device_ids)))
        granted_devices = list(device_query.scalars().all())

    result_unique_devices = []
    for unique_device in (list(owned) + granted_devices):
        result_unique_devices.append(DeviceOut.model_validate(unique_device))

    return result_unique_devices


@router.get("/{device_uuid}", response_model=DeviceOut)
async def get_device(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> DeviceOut:

    device = await require_device_permission(device_uuid,
                                             "read_status",
                                             current_user,
                                             session)
    return DeviceOut.model_validate(device)


@router.delete("/{device_uuid}",
               response_model=OkResponse,
               summary="Delete Device as owner from Ovi",
               description="Владелец замка полностью удаляет замок из системы Ovi, тут нет про клиента",
               )
async def delete_device(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> OkResponse:

    device = await require_device_permission(device_uuid,
                                             "admin",
                                             current_user,
                                             session)
    if device.user_uuid != current_user.user_uuid:
        raise HTTPException(status_code=403, detail="Only owner can delete device")
    await session.delete(device)
    await session.commit()
    return OkResponse(message="Device deleted")


@router.get("/{device_uuid}/status", response_model=DeviceOut)
async def get_device_status(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> DeviceOut:

    device = await require_device_permission(device_uuid,
                                             "read_status",
                                             current_user,
                                             session)
    return DeviceOut.model_validate(device)


@router.get("/{device_uuid}/diagnostics", response_model=DiagnosticsResponse)
async def get_diagnostics(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> DiagnosticsResponse:

    device = await require_device_permission(device_uuid,
                                             "admin",
                                             current_user,
                                             session)

    event_q = await session.execute(
        select(Event)
        .where(Event.device_uuid == device.device_uuid)
        .order_by(Event.created_at.desc())
        .limit(20)
    )
    events = event_q.scalars().all()

    pin_state_q = await session.execute(
        select(PinState).where(PinState.device_uuid == device.device_uuid)
    )
    pin_state = pin_state_q.scalar_one_or_none()

    return DiagnosticsResponse(
        device_uuid=str(device.device_uuid),
        # is_online=getattr(device, "is_online", None),
        battery_level=device.battery_level,
        firmware_version=device.firmware_version,
        last_seen=device.last_seen,
        last_time_sync=device.last_time_sync,
        pin_state=PinStateOut.model_validate(pin_state) if pin_state else None,
        recent_events=[EventOut.model_validate(e) for e in events],
        config=device.config or {},
    )


@router.get("/{device_uuid}/pin", response_model=OkResponse)
async def get_current_pin_endpoint(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> OkResponse:

    device = await require_device_permission(device_uuid, "admin", current_user, session)
    pin = await get_current_pin(device)
    if not pin:
        raise HTTPException(status_code=404, detail="Device secret not configured")
    return OkResponse(message=pin)


@router.post("/{device_uuid}/rotate-pin", response_model=OkResponse)
async def force_rotate_pin(
    device_uuid: str,
    session: SessionDep,
    current_user: User = Depends(get_current_user),
) -> OkResponse:

    device = await require_device_permission(device_uuid, "admin", current_user, session)

    # Send MQTT command to device
    mqtt = MQTTService()
    if mqtt.is_connected:
        import uuid as uuid_lib
        import time
        cmd = {
            "msg_id": f"msg_{uuid_lib.uuid4()}",
            "timestamp": int(time.time()),
            "command": {"type": "rotate_pin"},
            "signature": crypto_service.sign_dict({"type": "rotate_pin"}),
        }
        await mqtt.publish(
            payload=cmd,
            packetId=0,
            topicName=MQTTService.cmd_topic(device_uuid),
        )

    rotated = await check_and_rotate(session, device)
    if not rotated:
        raise HTTPException(status_code=400, detail="PIN already rotated for current time slot")
    return OkResponse(message="PIN rotation triggered")
