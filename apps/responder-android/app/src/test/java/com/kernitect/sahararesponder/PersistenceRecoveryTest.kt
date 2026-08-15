package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.mesh.AckSendState
import com.kernitect.sahararesponder.persistence.OutgoingPacketEntity
import com.kernitect.sahararesponder.persistence.normalizeRecoveredOutgoing
import com.kernitect.sahararesponder.persistence.preserveCloudSyncState
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

    @Test fun meshRetryDoesNotResetCloudSyncState() {
        val synced = packet(AckSendState.SENT_TO_MESH).copy(
            backendSyncState = "SYNCED", backendLastAttemptAt = 123L,
        )
        val retried = packet(AckSendState.SEARCHING).copy(attemptCount = 2)

        val result = preserveCloudSyncState(synced, retried)

        assertEquals("SYNCED", result.backendSyncState)
        assertEquals(123L, result.backendLastAttemptAt)
        assertEquals(AckSendState.SEARCHING.name, result.sendState)
        assertEquals(2, result.attemptCount)
    }
}
