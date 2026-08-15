package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.map.ResponderMapMode
import com.kernitect.sahararesponder.map.selectMapMode
import com.kernitect.sahararesponder.map.activeMapIncidents
import com.kernitect.sahararesponder.model.ResponderIncident
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineMapLogicTest {
    @Test fun onlineNetworkUsesOnlineTiles() = assertEquals(ResponderMapMode.ONLINE, selectMapMode(true, false))
    @Test fun archiveWithCoverageIsUsedWithoutNetwork() = assertEquals(ResponderMapMode.OFFLINE, selectMapMode(false, true, true))
    @Test fun missingArchiveHasDistinctState() = assertEquals(ResponderMapMode.OFFLINE_PACK_MISSING, selectMapMode(false, false, false))
    @Test fun installedArchiveWithoutMatchingTileHasCoverageState() = assertEquals(ResponderMapMode.OFFLINE_COVERAGE_UNAVAILABLE, selectMapMode(false, true, false))

    @Test fun rescuedIncidentsAreExcludedButArrivedRemainOnActiveMap() {
        val arrived = incident("SOS-ARRIVED", "ARRIVED")
        val rescued = incident("SOS-RESCUED", "RESCUED")
        assertEquals(listOf(arrived), activeMapIncidents(listOf(arrived, rescued)))
    }

    private fun incident(id: String, status: String) = ResponderIncident(
        id = id, priority = "CRITICAL", message = "Help", latitude = 27.6, longitude = 84.4,
        timestamp = 1L, hopCount = 0, status = status,
    )
}
