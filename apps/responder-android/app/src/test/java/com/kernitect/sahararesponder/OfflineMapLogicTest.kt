package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.map.ResponderMapMode
import com.kernitect.sahararesponder.map.selectMapMode
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineMapLogicTest {
    @Test fun onlineNetworkUsesOnlineTiles() = assertEquals(ResponderMapMode.ONLINE, selectMapMode(true, false))
    @Test fun archiveIsUsedWithoutNetwork() = assertEquals(ResponderMapMode.OFFLINE, selectMapMode(false, true))
    @Test fun missingArchiveShowsCoverageUnavailable() = assertEquals(ResponderMapMode.OFFLINE_COVERAGE_UNAVAILABLE, selectMapMode(false, false))
}
