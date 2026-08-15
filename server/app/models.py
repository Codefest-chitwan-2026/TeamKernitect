from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


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
    operatorName: str = Field(min_length=1, max_length=100)
    organization: str = Field(min_length=1, max_length=150)
    phone: str = Field(min_length=1, max_length=40)
    email: str | None = Field(default=None, max_length=150)
    district: str = Field(min_length=1, max_length=100)


class ResponderApprovalRequest(BaseModel):
    teamId: str = Field(min_length=1, max_length=100)


class ResponderRejectionRequest(BaseModel):
    reason: str | None = Field(default=None, max_length=300)
    
