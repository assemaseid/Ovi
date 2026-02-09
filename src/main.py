from contextlib import asynccontextmanager

from fastapi import FastAPI, APIRouter
from src.app.api.v1 import auth
from starlette.middleware.cors import CORSMiddleware
# from starlette.middleware.sessions import SessionMiddleware
# from starlette.requests import Request

from src.app.queries.orm import AsyncOrm


# @asynccontextmanager
# async def lifespan(app: FastAPI):
#     app.state.settings = settings

#     engine = make_engine(settings.db_url)
#     app.state.engine = engine
#     app.state.sessionmaker = make_sessionmaker(engine)

#     oauth = OAuth()
#     oauth.register(
#         name="google",
#         client_id=app.state.settings.GOOGLE_CLIENT_ID,
#         client_secret=app.state.settings.GOOGLE_CLIENT_SECRET,
#         server_metadata_url="https://accounts.google.com/.well-known/openid-configuration",
#         client_kwargs={
#             "scope": "openid email profile",
#             "timeout": 10.0,
#         },
#     )
#     app.state.oauth = oauth

#     yield

@asynccontextmanager
async def lifespan(app: FastAPI):
    await AsyncOrm.create_tables()
    await AsyncOrm.insert_users()

    yield

def create_app() -> FastAPI:
    api_v1 = APIRouter(prefix="/api/v1")
    
    api_v1.include_router(auth.router)
    
    app = FastAPI(lifespan=lifespan)
    app.include_router(api_v1)
    
    return app

app = create_app()

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# is_prod = not settings.DEBUG

# app.add_middleware(
    # SessionMiddleware,
    # secret_key=settings.SECRET_KEY,
    # same_site="none" if is_prod else "lax",
    # https_only=True if is_prod else False,
# )