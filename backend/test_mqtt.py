import asyncio
import aiomqtt
import json
import uuid

async def pub():
    async with aiomqtt.Client("test.mosquitto.org", 1883) as c:
        await c.publish(
            "devices/d57e741d-1242-4cc9-a2f6-96d9105891bb/events",
            json.dumps({
                "device_uuid": "d57e741d-1242-4cc9-a2f6-96d9105891bb",
                "msg_id": str(uuid.uuid4()),
                "signature": "",
                "event": {
                    "type": "unlock_success",
                    "user_uuid": "19bc3c72-33de-4434-98ce-41307d0674f9"
                }
            })
        )
        print("Sent!")

asyncio.run(pub())
