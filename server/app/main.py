from contextlib import asynccontextmanager
from fastapi.middleware.cors import CORSMiddleware
from fastapi import FastAPI

from app.database import (
    close_database,
    ensure_indexes,
    ping_database,
    seed_rescue_teams,
)

from app.routes.sos import router as sos_router
from app.routes.responders import router as responders_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    await ping_database()
    await ensure_indexes()
    await seed_rescue_teams()

    print("MongoDB Atlas connection successful.")

    yield

    await close_database()


app = FastAPI(
    title="Sahara API",
    description=(
        "Backend API for the Sahara "
        "emergency command system."
    ),
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
    ],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(
    sos_router,
    prefix="/api"
)
app.include_router(responders_router, prefix="/api")


@app.get("/")
async def root():
    return {
        "name": "Sahara API",
        "status": "running",
    }


@app.get("/health")
async def health():
    await ping_database()

    return {
        "api": "healthy",
        "database": "connected",
    }
