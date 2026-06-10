# Ovi

A smart-lock system consisting of an ESP32-based device, a FastAPI backend, and an Android client app.
Users can register and pair locks, share access with guests via temporary PINs, receive real-time
events over WebSockets and push notifications, and operate locks both over the internet (MQTT) and
locally via Bluetooth Low Energy when the network is unavailable.

## Repository layout

```
Ovi/
├── backend/    FastAPI service: REST API, MQTT broker integration, PostgreSQL, Redis
├── android/    Android client (Kotlin, Jetpack Compose)
└── firmware/   ESP32 device firmware
```

## Features

- **Authentication** — JWT access and refresh tokens with refresh-token blacklist on logout.
- **Devices** — register a lock to an account, query status, and send commands.
- **Grants** — owners can issue time-limited access to guests using a one-time PIN.
- **Events** — every lock action is recorded (open/close, PIN attempt, errors) and streamed to clients.
- **PIN rotation** — periodic background job rotates device PINs to limit credential exposure.
- **Offline mode** — events that the lock could not deliver online are queued and synced later.
- **Push notifications** — Firebase Cloud Messaging delivers alerts to the Android app.
- **Rate limiting** — login and registration endpoints are protected against brute-force.

## Tech stack

| Layer        | Tooling                                                              |
| ------------ | -------------------------------------------------------------------- |
| Backend      | Python 3.12, FastAPI, SQLAlchemy 2 (async), Alembic, Pydantic v2     |
| Database     | PostgreSQL 16                                                        |
| Cache / RL   | Redis 7 (slowapi rate limiter, transient state)                      |
| Messaging    | Eclipse Mosquitto (MQTT broker), Firebase Cloud Messaging            |
| Containers   | Docker, Docker Compose                                               |
| Mobile       | Kotlin, Jetpack Compose, Retrofit, OkHttp WebSocket, BLE             |
| Firmware     | ESP-IDF (C), MQTT-over-TLS client, BLE GATT server                   |

## Quick start (backend)

Requirements: Docker and Docker Compose.

```bash
cd backend
cp .env.example .env       # fill in POSTGRES_PASSWORD and other secrets
docker compose up -d --build
```

The API is then available at `http://localhost:8000` with interactive documentation at
`http://localhost:8000/api/v1/docs`.

## Local development (backend)

Without Docker, using Poetry:

```bash
cd backend
poetry install
poetry shell
alembic upgrade head
uvicorn src.main:app --reload
```

Postgres, Redis and Mosquitto can still be started via `docker compose up -d database_service
redis_container mqtt_broker` while the API runs on the host.

## Environment variables

| Variable                    | Description                                  | Default            |
| --------------------------- | -------------------------------------------- | ------------------ |
| `POSTGRES_USER`             | Database user                                | required           |
| `POSTGRES_PASSWORD`         | Database password                            | required           |
| `POSTGRES_DB`               | Database name                                | required           |
| `POSTGRES_HOST`             | Database host                                | required           |
| `POSTGRES_PORT`             | Database port                                | required           |
| `REDIS_HOST`                | Redis host                                   | `localhost`        |
| `REDIS_PORT`                | Redis port                                   | `6379`             |
| `MQTT_HOST`                 | MQTT broker host                             | `test.mosquitto.org` |
| `MQTT_PORT`                 | MQTT broker port                             | `1883`             |
| `MQTT_USERNAME`             | MQTT auth username                           | _empty_            |
| `MQTT_PASSWORD`             | MQTT auth password                           | _empty_            |
| `MQTT_CLIENT_ID`            | MQTT client identifier                       | `ovi_server`       |
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase service-account JSON        | _empty_            |

A complete reference can be found in `backend/.env.example`.

## Database migrations

```bash
cd backend
alembic upgrade head                            # apply all migrations
alembic revision --autogenerate -m "message"    # generate a new migration from models
alembic downgrade -1                            # roll back one revision
```

## API documentation

Once the backend is running, OpenAPI documentation is available at:

- Swagger UI: `/api/v1/docs`
- ReDoc:      `/api/v1/redoc`
- OpenAPI:    `/api/v1/openapi.json`
