from contextlib import asynccontextmanager

from fastapi import FastAPI, APIRouter
from src.app.api.v1 import auth
from starlette.middleware.cors import CORSMiddleware

from src.app.queries.orm import AsyncOrm
from src.app.services.device_service import mqtt_service

@asynccontextmanager
async def lifespan(app: FastAPI):
    await AsyncOrm.create_tables()
    await AsyncOrm.insert_users()
    await mqtt_service.connect()
    await mqtt_service.subscribe("devices/#")
    yield
    await mqtt_service.disconnect()

def create_app() -> FastAPI:
    api_v1 = APIRouter(prefix="/api/v1")
    
    api_v1.include_router(auth.router)
    
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