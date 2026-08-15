package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.mesh.AckSendState
import com.kernitect.sahararesponder.persistence.OutgoingPacketEntity
import com.kernitect.sahararesponder.persistence.normalizeRecoveredOutgoing
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistenceRecoveryTest {
    private fun packet(state: AckSendState) = OutgoingPacketEntity(
        packetId = "STATUS-ABC", incidentId = "SOS-1", packetType = "RESCUE_STATUS",
        payloadJson = "{payload}", createdAt = 10L, sendState = state.name, attemptCount = 1,
    )

    @Test fun transientSendStatesBecomeRetryableAfterRestart() {
        listOf(AckSendState.SEARCHING, AckSendState.CONNECTING, AckSendState.SENDING).forEach {
            val restored = normalizeRecoveredOutgoing(packet(it))
            assertEquals(AckSendState.FAILED.name, restored.sendState)
            assertEquals("STATUS-ABC", restored.packetId)
            assertEquals("{payload}", restored.payloadJson)
        }
    }

    @Test fun sentAndFailedStatesRemainStable() {
        assertEquals(AckSendState.SENT_TO_MESH.name, normalizeRecoveredOutgoing(packet(AckSendState.SENT_TO_MESH)).sendState)
        assertEquals(AckSendState.FAILED.name, normalizeRecoveredOutgoing(packet(AckSendState.FAILED)).sendState)
    }
}
