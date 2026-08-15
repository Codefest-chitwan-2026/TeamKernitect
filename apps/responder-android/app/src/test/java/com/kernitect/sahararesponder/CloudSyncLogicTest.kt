package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.persistence.OutgoingPacketEntity
import com.kernitect.sahararesponder.sync.*
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudSyncLogicTest {
    private fun record(id: String, type: String, state: String, time: Long) = OutgoingPacketEntity(
        id, "SOS-1", type, "{\"id\":\"$id\"}", time, "IDLE", backendSyncState = state,
    )

    @Test fun syncingNormalizesAfterRestart() = assertEquals("PENDING", normalizeCloudState("SYNCING"))

    @Test fun selectionKeepsStableIdsOrdersAndExcludesSyncedAndAck() {
        val records = listOf(
            record("STATUS-2", "RESCUE_STATUS", "PENDING", 2),
            record("CLAIM-1", "RESCUE_CLAIM", "FAILED", 1),
            record("STATUS-3", "RESCUE_STATUS", "SYNCED", 3),
            record("ACK-1", "SOS_ACK", "PENDING", 0),
        )
        assertEquals(listOf("CLAIM-1", "STATUS-2"), selectPending(records).map { it.packetId })
    }

    @Test fun batchLimitIsRespected() {
        val records = (1..50).map { record("STATUS-$it", "RESCUE_STATUS", "PENDING", it.toLong()) }
        assertEquals(40, selectPending(records).size)
    }
}
