from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import BaseModel, Field


BASE_DIR = Path(__file__).resolve().parent.parent

class AuthJWT(BaseModel):
    private_key_path: Path = BASE_DIR / "certs" / "private_key.pem"
    public_key_path: Path = BASE_DIR / "certs" / "public_key.pem"
    algorithm: str = "RS256"
    access_token_expire_minutes: int = 15 
    refresh_token_expire_days: int = 30

class Settings(BaseSettings):
    POSTGRES_USER: str
    POSTGRES_DB: str
    POSTGRES_HOST: str
    POSTGRES_PORT: int
    POSTGRES_PASSWORD: str
    
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
