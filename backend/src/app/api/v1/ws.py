import asyncio
import json
import logging
from datetime import UTC, datetime
from typing import Any

from fastapi import (
                     APIRouter,
                     Body,
                     Query,
                     WebSocket,
                     WebSocketDisconnect,
                     status
                     )
from jwt import PyJWTError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from src.app.models.device import Device
from src.app.models.grant import Grant
from src.app.models.notification import UserNotification
from src.app.models.user import User
from src.app.security.jwt_utils import decode_jwt
from src.app.services.auth_service import get_user_by_uuid, is_token_revoked
from sqlalchemy.ext.asyncio import AsyncSession

from src.database import async_session_factory, SessionDep

router = APIRouter(tags=["Websocket"])
logger = logging.getLogger(__name__)

class ConnectionManager:
    def __init__(self) -> None:
        self._connections: dict[str, WebSocket] = {}

    async def connect(self, ws: WebSocket, user_uuid: str) -> None:
        await ws.accept()
        self._connections[user_uuid] = ws
        logger.warning("WS connected user=%s", user_uuid)

    async def disconnect(self, user_uuid: str) -> None:
        self._connections.pop(user_uuid, None)
        logger.warning("WS disconnected user=%s", user_uuid)

    async def broadcast_event(
        self,
        device_uuid: str,
        event: dict[str, Any],
        allowed_users: set[str],
    ) -> None:
        payload = json.dumps({
            "type": "device_event",
            "device_uuid": device_uuid,
            "event": event,
        })
        logger.warning("broadcast_event: device=%s allowed=%s connections=%s",
                       device_uuid, allowed_users, list(self._connections.keys()))
        dead = []
        for user_uuid, ws in list(self._connections.items()):
            if user_uuid not in allowed_users:
                continue
            try:
                await ws.send_text(payload)
            except Exception:
                dead.append(user_uuid)
        for u in dead:
            self._connections.pop(u, None)


manager = ConnectionManager()

async def _authenticate_ws(token: str, session: SessionDep) -> User | None:
    try:
        payload = decode_jwt(token)
    except PyJWTError:
        return None

    if payload.get("type") != "access":
        return None

    jti = payload.get("jti", "")
    if await is_token_revoked(session, jti):
        return None

    return await get_user_by_uuid(session, payload.get("sub"))


async def _get_allowed_devices(user: User, session: SessionDep) -> set[str]:
    owned_q = await session.execute(
        select(Device.device_uuid).where(Device.user_uuid == user.user_uuid)
    )
    owned = {str(r) for r in owned_q.scalars().all()}

    grant_q = await session.execute(
        select(Grant.device_uuid).where(Grant.user_uuid == user.user_uuid)
    )
    granted = {str(r) for r in grant_q.scalars().all()}

    return owned | granted


async def _send_pending_notifications(websocket: WebSocket, user: User, session: AsyncSession) -> None:
    result = await session.execute(
        select(UserNotification)
        .where(UserNotification.user_uuid == user.user_uuid, UserNotification.is_read == False)
        .order_by(UserNotification.created_at)
    )
    notifications = result.scalars().all()
    for notif in notifications:
        await websocket.send_text(json.dumps({
            "type": "notification",
            "id": notif.id,
            "title": notif.title,
            "body": notif.body,
            "timestamp": notif.created_at.isoformat(),
        }))
        notif.is_read = True
    if notifications:
        await session.commit()


async def _send_device_statuses(websocket: WebSocket,
                                user: User,
                                session: SessionDep
                                ) -> None:
    device_uuids = await _get_allowed_devices(user, session)
    if not device_uuids:
        return

    result = await session.execute(
        select(Device).where(Device.device_uuid.in_(device_uuids))
    )
    devices = result.scalars().all()

    for device in devices:
        await websocket.send_text(json.dumps({
            "type": "device_status",
            "device_uuid": str(device.device_uuid),
            "battery_level": device.battery_level,
            "last_seen": device.last_seen.isoformat() if device.last_seen else None,
            "firmware_version": device.firmware_version,
        }))


@router.websocket("/ws/events")
async def ws_events(
    websocket: WebSocket,
    token: str = Query(..., description="JWT access token"),
    device_uuid: str | None = Query(default=None, description="Filter by device UUID"),
) -> None:
    async with async_session_factory() as session:
        user = await _authenticate_ws(token, session)
        if not user:
            await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
            return

        user_uuid_str = str(user.user_uuid)
        await manager.connect(websocket, user_uuid_str)

        try:
            await websocket.send_text(json.dumps({
                "type": "connected",
                "user_uuid": user_uuid_str,
                "filter": device_uuid,
                "timestamp": datetime.now(UTC).isoformat(),
            }))

            # Send current status of all user's devices on connect
            await _send_device_statuses(websocket, user, session)
            # Deliver any unread notifications accumulated while offline
            await _send_pending_notifications(websocket, user, session)

            while True:
                try:
                    msg = await asyncio.wait_for(websocket.receive_text(), timeout=30.0)
                    if msg == "ping":
                        await websocket.send_text(json.dumps({"type": "pong"}))
                except asyncio.TimeoutError:
                    await websocket.send_text(json.dumps({"type": "ping"}))
        except WebSocketDisconnect:
            pass
        except Exception as exc:
            logger.error("WebSocket error: %s", exc)
        finally:
            await manager.disconnect(user_uuid_str)
