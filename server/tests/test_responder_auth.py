import asyncio

import pytest
from fastapi import HTTPException

from app.auth import verify_password
from app.models import ResponderApprovalRequest, ResponderLoginRequest, ResponderRegistrationRequest, ResponderRejectionRequest
from app.routes import responders as routes


class FakeCollection:
    def __init__(self): self.items = []
    def _match(self, item, query):
        return all((item.get(key) != value["$ne"] if isinstance(value, dict) and "$ne" in value else item.get(key) == value) for key, value in query.items())
    async def find_one(self, query): return next((item.copy() for item in self.items if self._match(item, query)), None)
    async def insert_one(self, document): self.items.append(document.copy())
    async def find_one_and_update(self, query, update, **_kwargs):
        for index, item in enumerate(self.items):
            if self._match(item, query):
                changed = item | update.get("$set", {})
                self.items[index] = changed
                return changed.copy()
        return None
    async def update_one(self, query, update, upsert=False):
        found = await self.find_one(query)
        if found is None and upsert: self.items.append(query | update.get("$setOnInsert", {}))


def run(value): return asyncio.run(value)


@pytest.fixture
def collections(monkeypatch):
    responders, teams = FakeCollection(), FakeCollection()
    monkeypatch.setattr(routes, "responders_collection", responders)
    monkeypatch.setattr(routes, "rescue_teams_collection", teams)
    return responders, teams


def request(email="leader@example.com", device="DEVICE-12345"):
    return ResponderRegistrationRequest(deviceId=device, teamName="River Rescue", callsign="RIVER-1", district="Chitwan", leaderName="Asha", leaderPhone="9800000000", leaderEmail=email, password="secret123")


def test_registration_is_pending_hashed_and_public_profile_is_safe(collections):
    responders, _ = collections
    profile = run(routes.register_responder(request()))
    stored = responders.items[0]
    assert profile["status"] == "PENDING"
    assert stored["passwordHash"] != "secret123" and verify_password("secret123", stored["passwordHash"])
    assert "passwordHash" not in profile and "password" not in profile
    assert profile["teamId"].startswith("TEAM-")


def test_duplicate_normalized_email_is_rejected(collections):
    run(routes.register_responder(request(" Leader@Example.com ")))
    with pytest.raises(HTTPException) as error: run(routes.register_responder(request("leader@example.com", "DEVICE-99999")))
    assert error.value.status_code == 409


def test_wrong_password_and_pending_login_are_rejected(collections):
    run(routes.register_responder(request()))
    with pytest.raises(HTTPException) as wrong: run(routes.login_responder(ResponderLoginRequest(leaderEmail="leader@example.com", password="wrong", deviceId="DEVICE-12345")))
    assert wrong.value.status_code == 401 and wrong.value.detail == "Invalid email or password."
    with pytest.raises(HTTPException) as pending: run(routes.login_responder(ResponderLoginRequest(leaderEmail="leader@example.com", password="secret123", deviceId="DEVICE-12345")))
    assert pending.value.status_code == 403 and "awaiting" in pending.value.detail


def test_rejected_login_is_denied_and_device_lookup_survives(collections):
    run(routes.register_responder(request()))
    responder_id = collections[0].items[0]["responderId"]
    run(routes.reject_responder(responder_id, ResponderRejectionRequest()))
    with pytest.raises(HTTPException) as rejected: run(routes.login_responder(ResponderLoginRequest(leaderEmail="leader@example.com", password="secret123", deviceId="DEVICE-12345")))
    assert rejected.value.status_code == 403 and "rejected" in rejected.value.detail
    assert run(routes.get_responder_by_device("DEVICE-12345"))["status"] == "REJECTED"


def test_approved_login_returns_token_without_hash_and_keeps_team_id(collections):
    profile = run(routes.register_responder(request()))
    original_team_id = profile["teamId"]
    approved = run(routes.approve_responder(profile["responderId"], ResponderApprovalRequest()))
    assert approved["teamId"] == original_team_id
    login = run(routes.login_responder(ResponderLoginRequest(leaderEmail="LEADER@example.com", password="secret123", deviceId="DEVICE-12345")))
    assert login["accessToken"] and login["profile"]["status"] == "APPROVED"
    assert "passwordHash" not in login["profile"] and login["profile"]["teamId"] == original_team_id
