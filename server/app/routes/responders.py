from datetime import datetime, timezone
from uuid import uuid4

from fastapi import APIRouter, HTTPException, Query, status
from pymongo import ReturnDocument
from pymongo.errors import DuplicateKeyError

from app.database import responders_collection, rescue_teams_collection, responder_rescue_events_collection, sos_collection
from app.models import (
    ResponderApprovalRequest,
    ResponderRegistrationRequest,
    ResponderRejectionRequest,
    ResponderStatus,
    ResponderRescueEventType,
    ResponderRescueSyncRequest,
    ResponderRescueSyncResponse,
)

router = APIRouter(prefix="/responders", tags=["Responders"])

LIFECYCLE_RANK = {"ACCEPTED": 1, "ON_THE_WAY": 2, "NEARBY": 3, "ARRIVED": 4, "RESCUED": 5}
STATUS_TIME_FIELD = {"ACCEPTED": "acceptedAt", "ON_THE_WAY": "onTheWayAt", "NEARBY": "nearbyAt", "ARRIVED": "arrivedAt", "RESCUED": "rescuedAt"}
EVENT_SEMANTIC_FIELDS = (
    "eventId", "eventType", "incidentId", "responderId", "teamId", "status",
    "latitude", "longitude", "eventTimestamp", "priority", "message",
    "victimLatitude", "victimLongitude",
)

def is_forward_lifecycle(current: str | None, candidate: str) -> bool:
    return LIFECYCLE_RANK.get(candidate, 0) > LIFECYCLE_RANK.get(current or "", 0)


def has_same_event_semantics(stored: dict, candidate: dict) -> bool:
    return all(stored.get(field) == candidate.get(field) for field in EVENT_SEMANTIC_FIELDS)


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

@router.post("/sync", response_model=ResponderRescueSyncResponse)
async def sync_responder_rescue(request: ResponderRescueSyncRequest):
    responder = await responders_collection.find_one({"responderId": request.responderId, "deviceId": request.deviceId})
    if responder is None or responder.get("status") != ResponderStatus.APPROVED.value:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Approved responder installation not found.")
    if responder.get("teamId") != request.teamId:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Responder team does not match the approved assignment.")

    accepted, rejected = [], []
    for event in sorted(request.events, key=lambda value: value.eventTimestamp):
        if event.responderId != request.responderId or event.teamId != request.teamId:
            rejected.append({"eventId": event.eventId, "reason": "Event identity does not match the approved installation."})
            continue
        existing_event = await responder_rescue_events_collection.find_one({"eventId": event.eventId})
        if existing_event is not None:
            if has_same_event_semantics(existing_event, event.model_dump(mode="json")):
                accepted.append(event.eventId)
            else:
                rejected.append({"eventId": event.eventId, "reason": "eventId is already used by a different event."})
            continue

        now = datetime.now(timezone.utc)
        event_document = event.model_dump(mode="json") | {
            "teamName": responder.get("teamName"), "callsign": responder.get("callsign"), "receivedAt": now,
        }
        try:
            await responder_rescue_events_collection.insert_one(event_document)
        except DuplicateKeyError:
            raced_event = await responder_rescue_events_collection.find_one({"eventId": event.eventId})
            if raced_event and has_same_event_semantics(raced_event, event.model_dump(mode="json")):
                accepted.append(event.eventId)
            else:
                rejected.append({"eventId": event.eventId, "reason": "eventId conflicted with another upload."})
            continue
        base = {"id": event.incidentId, "revision": 1, "type": "EMERGENCY", "priority": (event.priority.value if event.priority else "CRITICAL"),
                "message": event.message, "latitude": event.victimLatitude, "longitude": event.victimLongitude,
                "timestamp": event.eventTimestamp, "hopCount": 0, "maxHops": 10, "status": "NEW", "receivedAt": now}
        await sos_collection.update_one(
            {"id": event.incidentId},
            {"$setOnInsert": base, "$set": {"updatedAt": now, "lastResponderSyncAt": now}},
            upsert=True,
        )

        if event.eventType == ResponderRescueEventType.RESCUE_CLAIM:
            incident = await sos_collection.find_one({"id": event.incidentId})
            update = {"$addToSet": {"claimTeamIds": request.teamId}, "$set": {"updatedAt": now}}
            if not incident.get("assignedTeamId"):
                update["$set"].update({"assignedTeamId": request.teamId, "assignedTeamName": responder.get("teamName"),
                                       "assignedResponderId": request.responderId, "callsign": responder.get("callsign")})
                if is_forward_lifecycle(incident.get("currentLifecycleStatus"), "ACCEPTED"):
                    update["$set"].update({"currentLifecycleStatus": "ACCEPTED", "acceptedAt": event.eventTimestamp})
            await sos_collection.update_one({"id": event.incidentId}, update)
            refreshed = await sos_collection.find_one({"id": event.incidentId})
            await sos_collection.update_one({"id": event.incidentId}, {"$set": {"claimConflict": len(refreshed.get("claimTeamIds", [])) > 1}})
        else:
            incident = await sos_collection.find_one({"id": event.incidentId})
            team_current = (incident.get("teamLifecycle") or {}).get(request.teamId)
            fields = {"lastResponderLatitude": event.latitude, "lastResponderLongitude": event.longitude, "updatedAt": now}
            if is_forward_lifecycle(team_current, event.status):
                fields[f"teamLifecycle.{request.teamId}"] = event.status
            if incident.get("assignedTeamId") == request.teamId and is_forward_lifecycle(incident.get("currentLifecycleStatus"), event.status):
                fields.update({"currentLifecycleStatus": event.status, STATUS_TIME_FIELD[event.status]: event.eventTimestamp})
            await sos_collection.update_one({"id": event.incidentId}, {"$set": fields})
        accepted.append(event.eventId)
    return {"acceptedEventIds": accepted, "rejected": rejected}


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
