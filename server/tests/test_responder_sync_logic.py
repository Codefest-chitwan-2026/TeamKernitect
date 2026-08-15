from app.routes.responders import is_forward_lifecycle


def test_lifecycle_progresses_forward():
    assert is_forward_lifecycle("ON_THE_WAY", "NEARBY")
    assert is_forward_lifecycle(None, "ACCEPTED")


def test_lifecycle_never_regresses_or_reapplies():
    assert not is_forward_lifecycle("ARRIVED", "ON_THE_WAY")
    assert not is_forward_lifecycle("ARRIVED", "ARRIVED")
    assert not is_forward_lifecycle("RESCUED", "ARRIVED")
