from contextlib import asynccontextmanager

from fastapi import FastAPI, APIRouter
from src.app.api.v1 import auth, devices, commands
from starlette.middleware.cors import CORSMiddleware

import logging
from src.app.queries.orm import AsyncOrm
from src.app.services import mqtt_service

logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    await AsyncOrm.create_tables()
    await AsyncOrm.insert_users()
    mqtt = mqtt_service.MQTTService()
    try:
        await mqtt.connect()
        print("✓ MQTT connected")
    except Exception as e:
        print(f"✗ MQTT connection failed: {e}")
    yield
    if mqtt.is_connected:
        await mqtt.disconnect()

def create_app() -> FastAPI:
    api_v1 = APIRouter(prefix="/api/v1")
    
    api_v1.include_router(auth.router)
    api_v1.include_router(devices.router)
    api_v1.include_router(commands.router)
    
    app = FastAPI(lifespan=lifespan)
    app.include_router(api_v1)
    
    return app

app = create_app()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)