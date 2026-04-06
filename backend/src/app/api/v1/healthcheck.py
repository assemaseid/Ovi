from datetime import datetime, timezone

from fastapi import APIRouter

health_router = APIRouter()

@health_router.get("/health")
def healthcheck():
    return {
        "status": "OK",
        "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%d-%dT-%H-%M-%SZ")
            }