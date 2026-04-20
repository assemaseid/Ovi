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
from src.app.models.user import User
from src.app.security.jwt_utils import decode_jwt
from src.app.services.auth_service import get_user_by_uuid, is_token_revoked
from src.database import async_session_factory, SessionDep

router = APIRouter(tags=["Websocket"])
logger = logging.getLogger(__name__)

class ConnectionManager:
    def __init__(self) -> None:
        self._connections: dict[str, list[tuple[WebSocket, str | None]]] = {}
        self._lock = asyncio.Lock()

    async def connect(
                      self,
                      ws: WebSocket,
                      user_uuid: str,
                      device_filter: str | None
                      ) -> None:
        await ws.accept()
        async with self._lock:
            self._connections.setdefault(user_uuid, []).append((ws, device_filter))
        logger.info("WS connected user=%s filter=%s", user_uuid, device_filter)

    async def disconnect(self,
                         ws: WebSocket,
                         user_uuid: str
                         ) -> None:
        async with self._lock:
            conns = self._connections.get(user_uuid, [])
            new_conns = []
            for conn in conns:
                if conn[0] is not ws:
                    new_conns.append(conn)
            self._connections[user_uuid] = new_conns
        logger.info("WS disconnected user=%s", user_uuid)

    async def broadcast_event(
                              self,
                              device_uuid: str,
                              event: dict[str, Any],
                              allowed_users: set[str]
                              ) -> None:
        payload = json.dumps({
            "type": "device_event",
            "device_uuid": device_uuid,
            "event": event,
        })
        dead: list[tuple[str, WebSocket]] = []

        async with self._lock:
            items = list(self._connections.items())

        for user_uuid, conns in items:
            if user_uuid not in allowed_users:
                continue
            for ws, device_filter in conns:
                if device_filter and device_filter != device_uuid:
                    continue
                try:
                    await ws.send_text(payload)
                except Exception:
                    dead.append((user_uuid, ws))

        async with self._lock:
            for u, ws in dead:
                conns = self._connections.get(u, [])
                alive = []
                for conn in conns:
                    if conn[0] is not ws:
                        alive.append(conn)
                self._connections[u] = alive


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
        await manager.connect(websocket, user_uuid_str, device_uuid)

        try:
            await websocket.send_text(json.dumps({
                "type": "connected",
                "user_uuid": user_uuid_str,
                "filter": device_uuid,
                "timestamp": datetime.now(UTC).isoformat(),
            }))

            # Send current status of all user's devices on connect
            await _send_device_statuses(websocket, user, session)

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
            await manager.disconnect(websocket, user_uuid_str)
