import asyncio
import json
from typing import Callable, Any

import aiomqtt
import logging

from src.config import settings

logger = logging.getLogger(__name__)

# Suffix → callback: e.g. "/events" → handle_device_event
_suffix_handlers: dict[str, Callable] = {}


class MQTTService:
    _instance: "MQTTService | None" = None

    def __new__(cls):
        if cls._instance is None:
            # выделяет память и возвращает пустой обьект
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance

    def __init__(self) -> None:
        if self._initialized:
            return
        self._initialized = True
        self._client: aiomqtt.Client | None = None
        self._subscriptions: dict[str, list[Callable]] = {}
        self._last_messages: dict[str, list] = {}
        self._listener_task: asyncio.Task | None = None

    @property
    def is_connected(self) -> bool:
        return bool(self._client)

    async def connect(self):
        client = aiomqtt.Client(
            hostname=settings.MQTT_HOST,
            port=settings.MQTT_PORT,
            username=settings.MQTT_USERNAME or None,
            password=settings.MQTT_PASSWORD or None,
            identifier=settings.MQTT_CLIENT_ID,
        )
        await client.__aenter__()
        self._client = client  # только после успешного подключения
        self._listener_task = asyncio.create_task(self._listen())
        logger.info("MQTT connected to %s:%s", settings.MQTT_HOST, settings.MQTT_PORT)

    def register_suffix_handler(self, suffix: str, callback: Callable) -> None:
        _suffix_handlers[suffix] = callback

    async def subscribe_device(self, device_uuid: str) -> None:
        # TODO: /cmd, /config, /notifications, admin/alerts
        for suffix in ("/events", "/status", "/cmd"):
            topic = f"devices/{device_uuid}{suffix}"
            if topic not in self._subscriptions:
                await self._client.subscribe(topic, qos=1)
                self._subscriptions[topic] = []
                logger.info("MQTT subscribed to %s", topic)

    async def _listen(self):
        async for message in self._client.messages:
            topic = str(message.topic)
            payload = json.loads(message.payload)
            logger.debug("MQTT recv <- %s : %s", topic, payload)

            # Route by suffix
            for suffix, handler in _suffix_handlers.items():
                if topic.endswith(suffix):
                    try:
                        await handler(topic, payload)
                    except Exception as e:
                        logger.error("MQTT handler error topic=%s: %s", topic, e)
                    break

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
        if type(payload) == str or type(payload) == bytes:
            msg = payload
        else:
            msg = json.dumps(payload)

        await self._client.publish(topicName,
                                   payload=msg,
                                   qos=qos,
                                   retain=retain)
        logger.debug("MQTT publish -> %s : %s", topicName, msg)

    @staticmethod
    def cmd_topic(device_uuid: str) -> str:
        return f"devices/{device_uuid}/cmd"

    @staticmethod
    def events_topic(device_uuid: str) -> str:
        return f"devices/{device_uuid}/events"

    @staticmethod
    def status_topic(device_uuid: str) -> str:
        return f"devices/{device_uuid}/status"


# client.tls_set(
#     ca_certs="certs/mqtt/ca.crt",
#     certfile="certs/mqtt/client.crt",
#     keyfile="certs/mqtt/client.key"
# )
