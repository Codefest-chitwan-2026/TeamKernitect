from datetime import datetime, timezone
from uuid import uuid4

from fastapi import APIRouter, HTTPException, Query, status
from pymongo import ReturnDocument

from app.database import responders_collection, rescue_teams_collection
from app.models import (
    ResponderApprovalRequest,
    ResponderRegistrationRequest,
    ResponderRejectionRequest,
    ResponderStatus,
)

router = APIRouter(prefix="/responders", tags=["Responders"])


def public_profile(document: dict) -> dict:
    return {key: document.get(key) for key in (
        "responderId", "deviceId", "operatorName", "organization", "phone",
        "email", "district", "status", "teamId", "teamName", "callsign",
        "createdAt", "approvedAt", "rejectionReason",
    )}


@router.post("/register")
async def register_responder(request: ResponderRegistrationRequest):
    existing = await responders_collection.find_one({"deviceId": request.deviceId})
    if existing is not None:
        if existing.get("status") == ResponderStatus.REJECTED.value:
            existing = await responders_collection.find_one_and_update(
                {"deviceId": request.deviceId},
                {"$set": {
                    **request.model_dump(),
                    "status": ResponderStatus.PENDING.value,
                    "rejectionReason": None,
                    "createdAt": datetime.now(timezone.utc),
                }},
                return_document=ReturnDocument.AFTER,
            )
        return public_profile(existing)

    now = datetime.now(timezone.utc)
    document = request.model_dump()
    document.update({
        "responderId": f"RESP-{uuid4().hex[:8].upper()}",
        "status": ResponderStatus.PENDING.value,
        "teamId": None,
        "teamName": None,
        "callsign": None,
        "createdAt": now,
        "approvedAt": None,
        "rejectionReason": None,
    })
    await responders_collection.insert_one(document)
    return public_profile(document)


@router.get("/device/{device_id}")
async def get_responder_by_device(device_id: str):
    document = await responders_collection.find_one({"deviceId": device_id})
    if document is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Responder registration not found.")
    return public_profile(document)


@router.get("")
async def list_responders(status_filter: ResponderStatus | None = Query(default=None, alias="status")):
    query = {} if status_filter is None else {"status": status_filter.value}
    items = await responders_collection.find(query, {"_id": 0}).sort("createdAt", -1).to_list(length=200)
    return {"count": len(items), "items": items}


@router.get("/teams")
async def list_rescue_teams():
    items = await rescue_teams_collection.find({}, {"_id": 0}).sort("teamName", 1).to_list(length=100)
    return {"count": len(items), "items": items}


@router.patch("/{responder_id}/approve")
async def approve_responder(responder_id: str, request: ResponderApprovalRequest):
    team = await rescue_teams_collection.find_one({"teamId": request.teamId})
    if team is None:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Selected rescue team does not exist.")
    now = datetime.now(timezone.utc)
    document = await responders_collection.find_one_and_update(
        {"responderId": responder_id},
        {"$set": {
            "status": ResponderStatus.APPROVED.value,
            "teamId": team["teamId"],
            "teamName": team["teamName"],
            "callsign": team["callsign"],
            "district": team["district"],
            "approvedAt": now,
            "rejectionReason": None,
        }},
        projection={"_id": 0},
        return_document=ReturnDocument.AFTER,
    )
    if document is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Responder not found.")
    return document


@router.patch("/{responder_id}/reject")
async def reject_responder(responder_id: str, request: ResponderRejectionRequest):
    document = await responders_collection.find_one_and_update(
        {"responderId": responder_id},
        {"$set": {
            "status": ResponderStatus.REJECTED.value,
            "rejectionReason": request.reason or "Registration rejected by SAHARA administrator.",
            "teamId": None,
            "teamName": None,
            "callsign": None,
            "approvedAt": None,
        }},
        projection={"_id": 0},
        return_document=ReturnDocument.AFTER,
    )
    if document is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Responder not found.")
    return document
