package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.model.IncidentClaim
import com.kernitect.sahararesponder.model.IncidentOwnership
import com.kernitect.sahararesponder.model.ownershipOf
import com.kernitect.sahararesponder.model.canAcceptIncident
import com.kernitect.sahararesponder.model.appendClaimIfNew
import com.kernitect.sahararesponder.model.planClaimReceipt
import com.kernitect.sahararesponder.model.RescueClaimPacket
import org.junit.Assert.assertEquals
import org.junit.Test

class IncidentOwnershipTest {
    private fun claim(id: String, teamId: String) = IncidentClaim(
        packetId = id, incidentId = "SOS-1", responderId = "R-$teamId", teamId = teamId,
        teamName = "Team $teamId", callsign = teamId, deviceId = "D-$teamId", district = "District",
        latitude = 0.0, longitude = 0.0, timestamp = 1L,
    )

    @Test fun emptyClaimsAreUnclaimed() =
        assertEquals(IncidentOwnership.UNCLAIMED, ownershipOf(emptyList(), "TEAM-A"))

    @Test fun anotherDeviceOnSameTeamIsMine() =
        assertEquals(IncidentOwnership.CLAIMED_BY_ME, ownershipOf(listOf(claim("CLAIM-1", "TEAM-A")), "TEAM-A"))

    @Test fun aDifferentTeamOwnsTheIncident() =
        assertEquals(IncidentOwnership.CLAIMED_BY_OTHER, ownershipOf(listOf(claim("CLAIM-2", "TEAM-B")), "TEAM-A"))

    @Test fun distinctTeamsCreateConflictAndRetainClaims() {
        val claims = listOf(claim("CLAIM-1", "TEAM-A"), claim("CLAIM-2", "TEAM-B"))
        assertEquals(IncidentOwnership.CONFLICT, ownershipOf(claims, "TEAM-A"))
        assertEquals(2, claims.size)
    }

    @Test fun otherTeamOwnershipPreventsAcceptAtTheActionLayer() {
        val ownership = ownershipOf(listOf(claim("CLAIM-2", "TEAM-B")), "TEAM-A")
        assertEquals(false, canAcceptIncident("NEW", ownership))
        assertEquals(true, canAcceptIncident("NEW", IncidentOwnership.UNCLAIMED))
    }

    @Test fun duplicateClaimDoesNotDuplicateOwnershipOrHistory() {
        val first = claim("CLAIM-1", "TEAM-B")
        assertEquals(listOf(first), appendClaimIfNew(listOf(first), first))
    }

    @Test fun pendingUnknownClaimAppliesWhenSosBecomesKnown() {
        val pending = listOf(claim("CLAIM-1", "TEAM-B"))
        assertEquals(IncidentOwnership.CLAIMED_BY_OTHER, ownershipOf(pending, "TEAM-A"))
    }

    @Test fun claimRelayPreservesIdsAndIncrementsHopOnce() {
        val packet = claimPacket(hopCount = 1, ttl = 5)
        val relay = planClaimReceipt(packet, alreadySeen = false).relayPacket!!
        assertEquals(packet.id, relay.id)
        assertEquals(packet.incidentId, relay.incidentId)
        assertEquals(2, relay.hopCount)
        assertEquals(5, relay.ttl)
    }

    @Test fun duplicateAndExhaustedClaimsDoNotRelay() {
        assertEquals(null, planClaimReceipt(claimPacket(), alreadySeen = true).relayPacket)
        assertEquals(null, planClaimReceipt(claimPacket(hopCount = 5, ttl = 5), alreadySeen = false).relayPacket)
    }

    private fun claimPacket(hopCount: Int = 0, ttl: Int = 5) = RescueClaimPacket(
        id = "CLAIM-1", incidentId = "SOS-1", responderId = "RESP-B", teamId = "TEAM-B",
        teamName = "Team Gamma", callsign = "GAMMA-1", deviceId = "DEVICE-B", district = "Chitwan",
        latitude = 27.6, longitude = 84.4, timestamp = 10L, hopCount = hopCount, ttl = ttl, priority = "CRITICAL",
    )
}
