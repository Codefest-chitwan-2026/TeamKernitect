from enum import Enum

from pydantic import BaseModel, ConfigDict, Field, model_validator


class EmergencyType(str, Enum):
    EMERGENCY = "EMERGENCY"
    EARTHQUAKE = "EARTHQUAKE"
    FLOOD = "FLOOD"
    LANDSLIDE = "LANDSLIDE"
    MEDICAL = "MEDICAL"
    TRAPPED = "TRAPPED"
    FIRE = "FIRE"
    OTHER = "OTHER"


class SosPriority(str, Enum):
    NORMAL = "NORMAL"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"

class RescueStatus(str, Enum):
    NEW = "NEW"
    RESPONDING = "RESPONDING"
    RESOLVED = "RESOLVED"

class SosStatusUpdate(BaseModel):
    status: RescueStatus

class SosPacket(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str = Field(min_length=1, max_length=100)

    revision: int = Field(
        default=1,
        ge=1
    )

    type: EmergencyType = EmergencyType.EMERGENCY

    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)

    message: str | None = Field(
        default=None,
        max_length=300
    )

    timestamp: int

    hop_count: int = Field(
        default=0,
        ge=0,
        alias="hopCount"
    )

    max_hops: int = Field(
        default=10,
        ge=1,
        alias="maxHops"
    )

    priority: SosPriority = SosPriority.CRITICAL


class ResponderStatus(str, Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"


class ResponderRegistrationRequest(BaseModel):
    deviceId: str = Field(min_length=8, max_length=100)
    teamName: str = Field(min_length=1, max_length=150)
    callsign: str = Field(min_length=1, max_length=60)
    district: str = Field(min_length=1, max_length=100)
    leaderName: str = Field(min_length=1, max_length=100)
    leaderPhone: str = Field(min_length=1, max_length=40)
    leaderEmail: str = Field(min_length=3, max_length=150)
    password: str = Field(min_length=8, max_length=200)


class ResponderLoginRequest(BaseModel):
    leaderEmail: str = Field(min_length=3, max_length=150)
    password: str = Field(min_length=1, max_length=200)
    deviceId: str = Field(min_length=8, max_length=100)


class ResponderApprovalRequest(BaseModel):
    teamId: str | None = Field(default=None, min_length=1, max_length=100)


class ResponderRejectionRequest(BaseModel):
    reason: str | None = Field(default=None, max_length=300)

class ResponderRescueEventType(str, Enum):
    RESCUE_CLAIM = "RESCUE_CLAIM"
    RESCUE_STATUS = "RESCUE_STATUS"

class ResponderLifecycle(str, Enum):
    ACCEPTED = "ACCEPTED"
    ON_THE_WAY = "ON_THE_WAY"
    NEARBY = "NEARBY"
    ARRIVED = "ARRIVED"
    RESCUED = "RESCUED"

class ResponderRescueEvent(BaseModel):
    eventId: str = Field(min_length=1, max_length=120)
    eventType: ResponderRescueEventType
    incidentId: str = Field(min_length=1, max_length=120)
    responderId: str = Field(min_length=1, max_length=120)
    teamId: str = Field(min_length=1, max_length=120)
    status: str
    latitude: float | None = Field(default=None, ge=-90, le=90)
    longitude: float | None = Field(default=None, ge=-180, le=180)
    eventTimestamp: int = Field(gt=0)
    priority: SosPriority | None = None
    message: str | None = Field(default=None, max_length=300)
    victimLatitude: float | None = Field(default=None, ge=-90, le=90)
    victimLongitude: float | None = Field(default=None, ge=-180, le=180)

    @model_validator(mode="after")
    def validate_event(self):
        if self.eventType == ResponderRescueEventType.RESCUE_CLAIM and self.status != "CLAIMED":
            raise ValueError("RESCUE_CLAIM status must be CLAIMED")
        if self.eventType == ResponderRescueEventType.RESCUE_STATUS and self.status not in {item.value for item in ResponderLifecycle}:
            raise ValueError("Invalid rescue lifecycle status")
        return self

class ResponderRescueSyncRequest(BaseModel):
    responderId: str = Field(min_length=1, max_length=120)
    deviceId: str = Field(min_length=1, max_length=120)
    teamId: str = Field(min_length=1, max_length=120)
    events: list[ResponderRescueEvent] = Field(min_length=1, max_length=100)

class RejectedSyncEvent(BaseModel):
    eventId: str
    reason: str

class ResponderRescueSyncResponse(BaseModel):
    acceptedEventIds: list[str]
    rejected: list[RejectedSyncEvent]
    
