from contextlib import asynccontextmanager

from fastapi import FastAPI, APIRouter
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware
from sqlalchemy import select
from src.app.api.v1 import auth, devices, commands, ws, healthcheck, users, events, sync, grants, fingerprints
from src.app.models import fingerprint as _fingerprint_model  # ensure table is registered
from starlette.middleware.cors import CORSMiddleware

import asyncio
import logging
from src.app.utils.rate_limit import limiter
from src.app.models.device import Device
from src.app.queries.orm import AsyncOrm
from src.app.services import mqtt_service
from src.app.services.fcm_service import init_fcm
from src.app.services.event_handler import handle_device_event, handle_device_status, handle_device_cmd
from src.app.services.pin_service import run_rotation_check
from src.database import async_session_factory

logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    await AsyncOrm.create_tables()
    await AsyncOrm.insert_users()

    init_fcm()

    mqtt = mqtt_service.MQTTService()
    try:
        await mqtt.connect()
        mqtt.register_suffix_handler("/cmd", handle_device_cmd)
        mqtt.register_suffix_handler("/events", handle_device_event)
        mqtt.register_suffix_handler("/status", handle_device_status)

        async with async_session_factory() as session:
            result = await session.execute(select(Device.device_uuid))
            device_uuids = result.scalars().all()

        for device_uuid in device_uuids:
            await mqtt.subscribe_device(str(device_uuid))

        print(f"✓ MQTT connected, subscribed to {len(device_uuids)} device(s)")
    except Exception as e:
        print(f"✗ MQTT connection failed: {e}")
    async def _pin_rotation_loop():
        while True:
            await asyncio.sleep(300)  # check every 5 minutes
            try:
                await run_rotation_check()
            except Exception as e:
                logger.error("PIN rotation loop error: %s", e)

    asyncio.create_task(_pin_rotation_loop())

    yield
    if mqtt.is_connected:
        await mqtt.disconnect()

def create_app() -> FastAPI:
    api_v1 = APIRouter(prefix="/api/v1")

    routers = [
        auth.router,
        devices.router,
        commands.router,
        users.router,
        events.router,
        sync.router,
        grants.router,
        fingerprints.router,
    ]

    for router in routers:
        api_v1.include_router(router)
    
    app = FastAPI(lifespan=lifespan,
                  docs_url="/api/v1/docs",
                  redoc_url="/api/v1/redoc",
                  openapi_url="/api/v1/openapi.json",
                  )

    app.state.limiter = limiter
    app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
    # app.add_middleware(SlowAPIMiddleware)

    app.include_router(api_v1)
    app.include_router(ws.router)
    app.include_router(healthcheck.router)

    return app

app = create_app()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)