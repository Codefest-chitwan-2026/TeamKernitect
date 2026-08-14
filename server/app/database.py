import os

from dotenv import load_dotenv
from pymongo import AsyncMongoClient
from pymongo.server_api import ServerApi


load_dotenv()

MONGODB_URL = os.getenv("MONGODB_URL")
MONGODB_DB_NAME = os.getenv("MONGODB_DB_NAME", "sahara")


if not MONGODB_URL:
    raise RuntimeError(
        "MONGODB_URL is not configured. "
        "Create server/.env and add your MongoDB Atlas connection string."
    )


client = AsyncMongoClient(
    MONGODB_URL,
    server_api=ServerApi("1"),
)

database = client[MONGODB_DB_NAME]

sos_collection = database["sos"]


async def ping_database() -> None:
    """Verify that Sahara can communicate with MongoDB Atlas."""
    await client.admin.command("ping")


async def close_database() -> None:
    """Close the MongoDB client cleanly."""
    await client.close()

async def ensure_indexes() -> None:
    await sos_collection.create_index(
        "id",
        unique=True
    )