package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.model.RescuePacket
import com.kernitect.sahararesponder.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SosRelayTest {
    private fun sos(hopCount: Int = 0, ttl: Int = 5) = RescuePacket(
        id = "SOS-123", type = RescuePacket.TYPE_SOS, latitude = 27.68, longitude = 84.43,
        timestamp = 123456L, hopCount = hopCount, ttl = ttl, priority = "CRITICAL", message = "Help",
    )

    @Test fun newSosWaitsForDecisionAndDoesNotAutoRelay() {
        val plan = planSosReceipt(alreadySeen = false)
        assertTrue(plan.shouldProcess)
    }

    @Test fun busyPassPreservesPayloadAndIncrementsOnlyHop() {
        val original = ResponderIncident.fromPacket(sos())
        val relay = prepareSosHandoff(original, null).relayPacket!!
        assertEquals(original.id, relay.id)
        assertEquals(original.latitude, relay.latitude, 0.0)
        assertEquals(original.longitude, relay.longitude, 0.0)
        assertEquals(original.timestamp, relay.timestamp)
        assertEquals(original.priority, relay.priority)
        assertEquals(original.message, relay.message)
        assertEquals(1, relay.hopCount)
        assertEquals(original.ttl, relay.ttl)
    }

    @Test fun duplicateOrRestoredHistoricalSosIsNotProcessedOrRelayed() {
        val plan = planSosReceipt(alreadySeen = true)
        assertFalse(plan.shouldProcess)
    }

    @Test fun ttlExhaustionStoresAwarenessWithoutRelaying() {
        val record = prepareSosHandoff(ResponderIncident.fromPacket(sos(hopCount = 5, ttl = 5)), null)
        assertEquals(SosHandoffState.TTL_EXHAUSTED, record.state)
        assertNull(record.relayPacket)
    }

    @Test fun relayPacketCannotCreateClaimOrLifecycleState() {
        val relay = prepareSosHandoff(ResponderIncident.fromPacket(sos()), null).relayPacket!!
        assertEquals(RescuePacket.TYPE_SOS, relay.type)
        assertEquals(relay.id, relay.incidentId)
    }

    @Test fun failedRetryReusesSameRelayPacketAndPassedLocksAccept() {
        val initial = prepareSosHandoff(ResponderIncident.fromPacket(sos()), null)
        val failed = initial.copy(state = SosHandoffState.FAILED, failureReason = "No peer")
        val retry = prepareSosHandoff(ResponderIncident.fromPacket(sos()), failed)
        assertEquals(initial.relayPacket, retry.relayPacket)
        assertFalse(handoffLocksAccept(failed))
        assertTrue(handoffLocksAccept(retry))
        assertTrue(handoffLocksAccept(retry.copy(state = SosHandoffState.PASSED)))
    }
}
