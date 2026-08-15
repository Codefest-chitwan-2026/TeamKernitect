package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.ble.BleConstants
import com.kernitect.sahararesponder.ble.isResponderAdvertisement
import com.kernitect.sahararesponder.mesh.NearbyPeerCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyResponderCacheTest {
    @Test fun recentResponderIsAvailableForFastHandoff() {
        val cache = NearbyPeerCache<String>(freshnessMs = 15_000)
        cache.remember("Alpha", "AA:01", seenAt = 1_000)
        assertEquals("Alpha", cache.select(now = 10_000, excludedAddress = null)?.value)
    }

    @Test fun staleOrSourcePeerIsNotSelected() {
        val cache = NearbyPeerCache<String>(freshnessMs = 15_000)
        cache.remember("Alpha", "AA:01", seenAt = 1_000)
        assertNull(cache.select(now = 20_000, excludedAddress = null))
        assertNull(cache.select(now = 2_000, excludedAddress = "aa:01"))
    }

    @Test fun failedCachedPeerCanBeInvalidatedBeforeFallback() {
        val cache = NearbyPeerCache<String>(freshnessMs = 15_000)
        cache.remember("Alpha", "AA:01", seenAt = 1_000)
        cache.invalidate("aa:01")
        assertNull(cache.select(now = 2_000, excludedAddress = null))
    }

    @Test fun onlyResponderRoleMarkerIsEligible() {
        assertTrue(isResponderAdvertisement(BleConstants.RESPONDER_ROLE_MARKER.copyOf()))
        assertEquals(false, isResponderAdvertisement(null))
        assertEquals(false, isResponderAdvertisement(byteArrayOf(0x01)))
    }
}
