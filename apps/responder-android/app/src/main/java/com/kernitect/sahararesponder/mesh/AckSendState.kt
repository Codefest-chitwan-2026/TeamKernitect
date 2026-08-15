package com.kernitect.sahararesponder.mesh

import com.kernitect.sahararesponder.model.RescueAckPacket
import com.kernitect.sahararesponder.model.RescueClaimPacket

enum class AckSendState(val message: String) {
    IDLE("Acknowledgement ready"),
    SEARCHING("Searching for nearby RESCUEMESH relay…"),
    CONNECTING("Connecting to nearby relay…"),
    SENDING("Sending acknowledgement…"),
    SENT_TO_MESH("Acknowledgement sent into RESCUEMESH"),
    FAILED("Could not send acknowledgement"),
}

data class AckRecord(
    val packet: RescueAckPacket,
    val state: AckSendState = AckSendState.IDLE,
    val failureReason: String? = null,
)

data class ClaimRecord(
    val packet: RescueClaimPacket,
    val state: AckSendState = AckSendState.IDLE,
    val failureReason: String? = null,
)
