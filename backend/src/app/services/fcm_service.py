import logging
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, messaging

from src.config import settings

logger = logging.getLogger(__name__)

_app: firebase_admin.App | None = None


def init_fcm() -> bool:
    global _app
    if _app is not None:
        return True

    creds_path = settings.FIREBASE_CREDENTIALS_PATH
    if not creds_path or not Path(creds_path).exists():
        logger.warning("FCM credentials not found at '%s' — push notifications disabled",
                       creds_path)
        return False

    try:
        cred = credentials.Certificate(creds_path)
        _app = firebase_admin.initialize_app(cred)
        logger.info("Firebase Admin SDK initialized")
        return True
    except Exception as e:
        logger.error("Failed to initialize Firebase: %s", e)
        return False


async def send_notification(fcm_token: str,
                            title: str,
                            body: str,
                            data: dict | None = None
                            ) -> bool:
    if _app is None:
        logger.debug("FCM not initialized, skipping notification")
        return False

    try:
        message = messaging.Message(
            notification=messaging.Notification(title=title, body=body),
            data={k: str(v) for k, v in (data or {}).items()},
            token=fcm_token,
        )
        response = messaging.send(message)
        logger.info("FCM sent: %s", response)
        return True
    except messaging.UnregisteredError:
        logger.warning("FCM token unregistered: %s", fcm_token[:20])
        return False
    except Exception as e:
        logger.error("FCM send failed: %s", e)
        return False
