package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.model.IncidentOwnership
import com.kernitect.sahararesponder.model.RescueLifecycle
import com.kernitect.sahararesponder.model.RescueStatusEvent
import com.kernitect.sahararesponder.model.canControlLifecycle
import com.kernitect.sahararesponder.model.latestStatusByTeam
import com.kernitect.sahararesponder.model.RescueLifecycleEvent
import com.kernitect.sahararesponder.model.recordLifecycleEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RescueLifecycleTest {
    private fun lifecycleEvent(status: RescueLifecycle, timestamp: Long, packetId: String = "STATUS-1") =
        RescueLifecycleEvent(status, timestamp, "TEAM-A", "Team Alpha", "A1", packetId)
    @Test fun normalProgressionIsOrdered() {
        assertEquals(RescueLifecycle.ON_THE_WAY, RescueLifecycle.ACCEPTED.next())
        assertEquals(RescueLifecycle.NEARBY, RescueLifecycle.ON_THE_WAY.next())
        assertEquals(RescueLifecycle.ARRIVED, RescueLifecycle.NEARBY.next())
        assertEquals(RescueLifecycle.RESCUED, RescueLifecycle.ARRIVED.next())
        assertEquals(null, RescueLifecycle.RESCUED.next())
    }

    @Test fun regressionIsRejected() {
        assertTrue(RescueLifecycle.progresses("ON_THE_WAY", "ARRIVED"))
        assertFalse(RescueLifecycle.progresses("ARRIVED", "ON_THE_WAY"))
        assertFalse(RescueLifecycle.progresses("ARRIVED", "ARRIVED"))
    }

    @Test fun latestTeamStatusUsesRankInsteadOfClockAlone() {
        fun event(id: String, status: String, time: Long) = RescueStatusEvent(id, "SOS-1", "R-1", "TEAM-A", "Alpha", "A1", status, 0.0, 0.0, time)
        val result = latestStatusByTeam(listOf(event("STATUS-1", "ARRIVED", 10), event("STATUS-2", "ON_THE_WAY", 20)))
        assertEquals("ARRIVED", result.getValue("TEAM-A").status)
    }

    @Test fun onlyMyTeamOrConflictCanControlLifecycle() {
        assertTrue(canControlLifecycle(IncidentOwnership.CLAIMED_BY_ME))
        assertTrue(canControlLifecycle(IncidentOwnership.CONFLICT))
        assertFalse(canControlLifecycle(IncidentOwnership.CLAIMED_BY_OTHER))
        assertFalse(canControlLifecycle(IncidentOwnership.UNCLAIMED))
    }

    @Test fun eventIsRecordedOnlyOnceAndRetryDoesNotDuplicateIt() {
        val first = recordLifecycleEvent(emptyList(), lifecycleEvent(RescueLifecycle.ON_THE_WAY, 100))
        val retried = recordLifecycleEvent(first, lifecycleEvent(RescueLifecycle.ON_THE_WAY, 100))
        assertEquals(1, retried.size)
    }

    @Test fun lifecycleRegressionDoesNotCreateEvent() {
        val arrived = listOf(lifecycleEvent(RescueLifecycle.ARRIVED, 200))
        assertEquals(arrived, recordLifecycleEvent(arrived, lifecycleEvent(RescueLifecycle.NEARBY, 300, "STATUS-2")))
    }

    @Test fun eventsRemainChronologicalAndPreserveIncomingTimestamp() {
        val nearby = lifecycleEvent(RescueLifecycle.NEARBY, 200, "STATUS-2")
        val events = recordLifecycleEvent(listOf(lifecycleEvent(RescueLifecycle.ON_THE_WAY, 100)), nearby)
        assertEquals(listOf(100L, 200L), events.map { it.timestamp })
        assertEquals(200L, events.last().timestamp)
    }
}
