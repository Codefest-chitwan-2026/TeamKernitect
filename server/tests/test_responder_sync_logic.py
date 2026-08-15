from app.routes.responders import has_same_event_semantics, is_forward_lifecycle


def test_lifecycle_progresses_forward():
    assert is_forward_lifecycle("ON_THE_WAY", "NEARBY")
    assert is_forward_lifecycle(None, "ACCEPTED")


def test_lifecycle_never_regresses_or_reapplies():
    assert not is_forward_lifecycle("ARRIVED", "ON_THE_WAY")
    assert not is_forward_lifecycle("ARRIVED", "ARRIVED")
    assert not is_forward_lifecycle("RESCUED", "ARRIVED")


def test_idempotency_requires_the_complete_event_semantics_to_match():
    original = {
        "eventId": "STATUS-1", "eventType": "RESCUE_STATUS", "incidentId": "SOS-1",
        "responderId": "RESP-1", "teamId": "TEAM-1", "status": "ARRIVED",
        "latitude": 27.5, "longitude": 84.4, "eventTimestamp": 100,
        "priority": "CRITICAL", "message": "Help", "victimLatitude": 27.6,
        "victimLongitude": 84.5,
    }
    assert has_same_event_semantics(original | {"receivedAt": "ignored"}, original)
    assert not has_same_event_semantics(original, original | {"status": "RESCUED"})
    assert not has_same_event_semantics(original, original | {"eventTimestamp": 101})
