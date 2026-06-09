from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import BaseModel, Field


BASE_DIR = Path(__file__).resolve().parent.parent

class AuthJWT(BaseModel):
    private_key_path: Path = BASE_DIR / "certs" / "jwt-private.pem"
    public_key_path: Path = BASE_DIR / "certs" / "jwt-public.pem"
    algorithm: str = "RS256"
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 30

class Settings(BaseSettings):
    POSTGRES_USER: str
    POSTGRES_DB: str
    POSTGRES_HOST: str
    POSTGRES_PORT: int
    POSTGRES_PASSWORD: str

    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379

    server_private_key_pem: Path = BASE_DIR / "certs" / "server-private.pem"
    server_public_key_pem: Path = BASE_DIR / "certs" / "server-public.pem"

    unlock_token_ttl_seconds: int = 30

    MQTT_HOST: str = "test.mosquitto.org"
    MQTT_PORT: int = 1883
    MQTT_USERNAME: str | None = None
    MQTT_PASSWORD: str | None = None
    MQTT_CLIENT_ID: str = "ovi_server"
    MQTT_DEVICE_HOST: str = ""

    FIREBASE_CREDENTIALS_PATH: str | None = None

    MAX_FINGER_SLOTS: int = 127

    
    @property
    def db_url(self):
        return "postgresql+asyncpg://{}:{}@{}:{}/{}".format(
            self.POSTGRES_USER,
            self.POSTGRES_PASSWORD,
            self.POSTGRES_HOST,
            self.POSTGRES_PORT,
            self.POSTGRES_DB,
        )

    @property
    def alembic_db_url(self):
        return 'postgresql://{}:{}@{}:{}/{}'.format(
            self.POSTGRES_USER,
            self.POSTGRES_PASSWORD,
            self.POSTGRES_HOST,
            self.POSTGRES_PORT,
            self.POSTGRES_DB,
        )

    auth_jwt: AuthJWT = Field(default_factory=AuthJWT)
    model_config = SettingsConfigDict(env_file=".env",
                                      extra="ignore",
                                      )

settings = Settings()
