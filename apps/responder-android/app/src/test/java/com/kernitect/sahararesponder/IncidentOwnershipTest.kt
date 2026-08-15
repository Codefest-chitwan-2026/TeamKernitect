package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.model.IncidentClaim
import com.kernitect.sahararesponder.model.IncidentOwnership
import com.kernitect.sahararesponder.model.ownershipOf
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
}
