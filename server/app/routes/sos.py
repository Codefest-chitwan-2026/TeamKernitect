from datetime import datetime, timezone
from fastapi import (
    APIRouter,
    HTTPException,
    Query,
    status,
)

from app.models import (
    EmergencyType,
    RescueStatus,
    SosPacket,
    SosPriority,
    SosStatusUpdate,
)

from fastapi import APIRouter, HTTPException, status
from pymongo import ReturnDocument

from app.database import sos_collection
from app.models import (
    SosPacket,
    SosStatusUpdate,
)


router = APIRouter(
    prefix="/sos",
    tags=["SOS"],
)


@router.post("")
async def create_sos(packet: SosPacket):
    now = datetime.now(timezone.utc)

    existing = await sos_collection.find_one(
        {"id": packet.id}
    )

    # Completely new SOS
    if existing is None:
        document = packet.model_dump(by_alias=True)

        document["receivedAt"] = now
        document["updatedAt"] = now
        document["status"] = "NEW"

        await sos_collection.insert_one(document)

        return {
            "message": "SOS received successfully.",
            "id": packet.id,
            "revision": packet.revision,
            "result": "CREATED",
        }

    existing_revision = existing.get(
        "revision",
        1
    )

    # Same revision arrived through another relay path
    if packet.revision == existing_revision:
        return {
            "message": "Duplicate SOS packet ignored.",
            "id": packet.id,
            "revision": packet.revision,
            "result": "DUPLICATE_IGNORED",
        }

    # Older packet arrived after a newer copy
    if packet.revision < existing_revision:
        return {
            "message": "Older SOS revision ignored.",
            "id": packet.id,
            "revision": packet.revision,
            "latestRevision": existing_revision,
            "result": "STALE_IGNORED",
        }

    # A newer victim update has arrived
    updated_document = packet.model_dump(
        by_alias=True
    )

    updated_document["updatedAt"] = now

    await sos_collection.update_one(
        {"id": packet.id},
        {
            "$set": updated_document
        },
    )

    return {
        "message": "SOS updated successfully.",
        "id": packet.id,
        "revision": packet.revision,
        "result": "UPDATED",
    }
#list/filter sos

@router.get("")
async def list_sos(
    status_filter: RescueStatus | None = Query(
        default=None,
        alias="status",
    ),
    type_filter: EmergencyType | None = Query(
        default=None,
        alias="type",
    ),
    priority: SosPriority | None = Query(
        default=None,
    ),
    limit: int = Query(
        default=50,
        ge=1,
        le=100,
    ),
):
    query = {}

    if status_filter is not None:
        query["status"] = status_filter.value

    if type_filter is not None:
        query["type"] = type_filter.value

    if priority is not None:
        query["priority"] = priority.value

    cursor = (
        sos_collection
        .find(
            query,
            {"_id": 0},
        )
        .sort("receivedAt", -1)
        .limit(limit)
    )

    items = await cursor.to_list(
        length=limit
    )

    total = await sos_collection.count_documents(
        query
    )

    return {
        "count": total,
        "items": items,
    }
#dashboard statistics
@router.get("/stats")
async def get_sos_stats():
    total = await sos_collection.count_documents({})

    new_count = await sos_collection.count_documents(
        {"status": "NEW"}
    )

    responding_count = await sos_collection.count_documents(
        {"status": "RESPONDING"}
    )

    resolved_count = await sos_collection.count_documents(
        {"status": "RESOLVED"}
    )

    critical_count = await sos_collection.count_documents(
        {
            "priority": "CRITICAL",
            "status": {
                "$ne": "RESOLVED"
            },
        }
    )

    return {
        "total": total,
        "new": new_count,
        "responding": responding_count,
        "resolved": resolved_count,
        "criticalActive": critical_count,
    }

@router.get("/{sos_id}")
async def get_sos(sos_id: str):
    document = await sos_collection.find_one(
        {"id": sos_id},
        {"_id": 0},
    )

    if document is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"SOS '{sos_id}' not found.",
        )

    return document

@router.patch("/{sos_id}/status")
async def update_sos_status(
    sos_id: str,
    update: SosStatusUpdate,
):
    now = datetime.now(timezone.utc)

    document = await sos_collection.find_one_and_update(
        {"id": sos_id},
        {
            "$set": {
                "status": update.status.value,
                "statusUpdatedAt": now,
            }
        },
        projection={"_id": 0},
        return_document=ReturnDocument.AFTER,
    )

    if document is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"SOS '{sos_id}' not found.",
        )

    return document

