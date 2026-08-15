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
responders_collection = database["responders"]
rescue_teams_collection = database["rescue_teams"]
responder_rescue_events_collection = database["responder_rescue_events"]


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
    await responders_collection.create_index("responderId", unique=True)
    await responders_collection.create_index("deviceId", unique=True)
    await rescue_teams_collection.create_index("teamId", unique=True)
    await responder_rescue_events_collection.create_index("eventId", unique=True)
    await responder_rescue_events_collection.create_index([("incidentId", 1), ("eventTimestamp", 1)])


async def seed_rescue_teams() -> None:
    teams = [
        {"teamId": "BAGMATI-ALPHA-01", "teamName": "Team Alpha", "callsign": "ALPHA-1", "province": "Bagmati", "district": "Chitwan", "status": "AVAILABLE"},
        {"teamId": "BAGMATI-BETA-01", "teamName": "Team Beta", "callsign": "BETA-1", "province": "Bagmati", "district": "Chitwan", "status": "AVAILABLE"},
        {"teamId": "BAGMATI-GAMMA-01", "teamName": "Team Gamma", "callsign": "GAMMA-1", "province": "Bagmati", "district": "Chitwan", "status": "AVAILABLE"},
    ]
    for team in teams:
        await rescue_teams_collection.update_one(
            {"teamId": team["teamId"]},
            {"$setOnInsert": team},
            upsert=True,
        )
