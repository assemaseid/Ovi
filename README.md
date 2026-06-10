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

## Frontend (Android app)

The Android client in `android/` is the user-facing frontend of the system. It is written in
Kotlin with Jetpack Compose and follows a clean-architecture layout (`data` / `domain` /
`presentation`) with Hilt for dependency injection.

### Features

- **Auth flow** — registration, login, automatic JWT refresh, secure token storage.
- **Device list** — lists locks the user owns or has been granted access to, with live status.
- **Lock control** — open/close commands sent over MQTT (via backend) or directly over BLE
  when the phone is near the lock and the network is unavailable.
- **Guest access** — owners can issue time-limited PINs to guests from the app.
- **Live events** — opens a WebSocket to the backend to stream lock events into the UI.
- **Push notifications** — receives FCM alerts (e.g. unauthorized PIN attempts) even when
  the app is in the background.
- **Offline-first** — caches devices and events in a local Room database so the UI is
  usable without a network connection.

### Tech stack

| Concern         | Library                                                      |
| --------------- | ------------------------------------------------------------ |
| UI              | Jetpack Compose, Material 3                                  |
| DI              | Hilt                                                         |
| Networking      | Retrofit + OkHttp (REST), OkHttp WebSocket (events)          |
| Local storage   | Room, DataStore (tokens / preferences)                       |
| BLE             | Android Bluetooth GATT API                                   |
| Push            | Firebase Cloud Messaging                                     |
| Async           | Kotlin Coroutines + Flow                                     |

### Project layout

```
android/app/src/main/java/com/example/ovi/
├── data/           # Retrofit DTOs, repositories, Room, BLE, WebSocket, FCM
├── domain/         # Models, repository interfaces, BLE use-cases
├── presentation/   # Compose UI, ViewModels, navigation
├── di/             # Hilt modules
└── util/           # Helpers
```

### Build and run

Requirements: Android Studio (Hedgehog or newer), JDK 17, an Android device or emulator
with API 24+, and a running backend reachable from the device.

```bash
cd android
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install to a connected device
```

Configure the backend base URL and (if used) Firebase credentials before building:

- Backend URL — set in the app's Retrofit module (or via a `BuildConfig` field).
- FCM — drop a `google-services.json` into `android/app/`.

For BLE features the app needs the runtime permissions `BLUETOOTH_SCAN`,
`BLUETOOTH_CONNECT`, and (on Android 11 and below) `ACCESS_FINE_LOCATION`.

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
