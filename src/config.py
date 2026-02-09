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
    DB_USER: str
    DB_PASS: str
    DB_HOST: str
    DB_PORT: str
    DB_NAME: str
    
    @property
    def DATABASE_URL_asyncpg(self):
        return "postgresql+asyncpg://{}:{}@{}:{}/{}".format(
            self.DB_USER,
            self.DB_PASS,
            self.DB_HOST,
            self.DB_PORT,
            self.DB_NAME,
        )
    
    auth_jwt: AuthJWT = Field(default_factory=AuthJWT)
    model_config = SettingsConfigDict(env_file=".env")

settings = Settings()
