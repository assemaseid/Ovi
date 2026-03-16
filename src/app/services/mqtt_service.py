import asyncio
import json
from typing import Callable, Any

import aiomqtt
import logging

from src.config import settings

logger = logging.getLogger(__name__)

class MQTTService:
    "Singleton for all mqtt services as publish and subscribe"

    _instance: "MQTTService | None" = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls) #выделяет память и возвращает пустой обьект
            cls._instance._initialized = False
        return cls._instance

    def __init__(self) -> None:
        if self._initialized:
            return
        self._initialized = True
        self._client: aiomqtt.Client | None
        self._subscriptions: dict[str, list[Callable]] = {}
        self._last_messages: dict[str, list] = {}
        self._listener_task: asyncio.Task | None = None

    async def connect(self):
        self._client = aiomqtt.Client(
            hostname=settings.MQTT_HOST,
            port=settings.MQTT_PORT,
            username=settings.MQTT_USERNAME or None,
            password=settings.MQTT_PASSWORD or None,
            identifier=settings.MQTT_CLIENT_ID,
        )
        await self._client.__aenter__()
        self._listener_task = asyncio.create_task(self._listen())
        logger.info(f"MQTT connected to {settings.MQTT_HOST} {settings.MQTT_PORT}")

    async def disconnect(self):
        if self._listener_task:
            self._listener_task.cancel()
        try:
            await self._listener_task
        except asyncio.CancelledError:
            pass

        if self._client:
            await self._client.__aexit__(None, None, None)
            self._client = None
        logger.info("MQTT disconnected")

    async def publish(
            self,
            payload: Any,
            packetId: int,
            topicName: str,
            qos: int = 1,
            retain: bool = False,
    ):
        msg = json.dumps(payload) if not isinstance(payload, (str, bytes)) else payload
        await self._client.publish(topicName, payload=msg, qos=qos, retain=retain)
        logger.debug("MQTT publish -> %s : %s", topicName, msg)



# client.tls_set(
#     ca_certs="certs/mqtt/ca.crt",
#     certfile="certs/mqtt/client.crt",
#     keyfile="certs/mqtt/client.key"
# )