package com.kernitect.sahararesponder.model

import com.kernitect.sahararesponder.location.ResponderLocation
import org.json.JSONObject
import java.util.UUID

data class RescueClaimPacket(
    override val id: String,
    override val incidentId: String,
    val responderId: String,
    val teamId: String,
    val teamName: String,
    val callsign: String,
    val deviceId: String,
    val district: String,
    val status: String = "CLAIMED",
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val hopCount: Int = 0,
    val ttl: Int = 5,
    val priority: String,
    override val type: String = TYPE,
    val message: String = "RESCUE_CLAIM|$incidentId|$teamId|$teamName|$callsign",
) : MeshOutgoingPacket {
    override fun toJson() = JSONObject().apply {
        put("id", id); put("type", type); put("incidentId", incidentId)
        put("responderId", responderId); put("teamId", teamId); put("teamName", teamName)
        put("callsign", callsign); put("deviceId", deviceId); put("district", district)
        put("status", status); put("latitude", latitude); put("longitude", longitude)
        put("timestamp", timestamp); put("hopCount", hopCount); put("ttl", ttl)
        put("priority", priority); put("message", message)
    }.toString()

    fun canRelay() = hopCount < ttl
    fun nextHop() = copy(hopCount = hopCount + 1)
    fun asClaim() = IncidentClaim(id, incidentId, responderId, teamId, teamName, callsign, deviceId, district, latitude, longitude, timestamp)

    companion object {
        const val TYPE = "RESCUE_CLAIM"
        fun create(incident: ResponderIncident, location: ResponderLocation?, profile: ResponderTeamProfile) = RescueClaimPacket(
            id = "CLAIM-${UUID.randomUUID()}", incidentId = incident.id,
            responderId = profile.responderId, teamId = profile.teamId, teamName = profile.teamName,
            callsign = profile.callsign, deviceId = profile.deviceId, district = profile.district,
            latitude = location?.latitude ?: 0.0, longitude = location?.longitude ?: 0.0,
            timestamp = System.currentTimeMillis(), priority = incident.priority,
        )
        fun fromJson(raw: String): RescueClaimPacket? = runCatching {
            val j = JSONObject(raw)
            if (j.optString("type") != TYPE) return null
            RescueClaimPacket(
                id = j.getString("id"), incidentId = j.getString("incidentId"),
                responderId = j.getString("responderId"), teamId = j.getString("teamId"),
                teamName = j.getString("teamName"), callsign = j.getString("callsign"),
                deviceId = j.getString("deviceId"), district = j.getString("district"),
                status = j.optString("status", "CLAIMED"), latitude = j.optDouble("latitude", 0.0),
                longitude = j.optDouble("longitude", 0.0), timestamp = j.getLong("timestamp"),
                hopCount = j.optInt("hopCount", 0), ttl = j.optInt("ttl", 5),
                priority = j.optString("priority", "NORMAL"),
                message = j.optString("message").ifBlank {
                    "RESCUE_CLAIM|${j.getString("incidentId")}|${j.getString("teamId")}|${j.getString("teamName")}|${j.getString("callsign")}"
                },
            ).takeIf {
                it.id.startsWith("CLAIM-") && it.incidentId.isNotBlank() && it.responderId.isNotBlank() &&
                    it.teamId.isNotBlank() && it.teamName.isNotBlank() && it.callsign.isNotBlank() &&
                    it.deviceId.isNotBlank() && it.district.isNotBlank() && it.status == "CLAIMED" &&
                    it.hopCount >= 0 && it.ttl >= 0
            }
        }.getOrNull()
    }
}

data class IncidentClaim(
    val packetId: String, val incidentId: String, val responderId: String, val teamId: String,
    val teamName: String, val callsign: String, val deviceId: String, val district: String,
    val latitude: Double, val longitude: Double, val timestamp: Long,
)

enum class IncidentOwnership { UNCLAIMED, CLAIMED_BY_ME, CLAIMED_BY_OTHER, CONFLICT }

fun ownershipOf(claims: List<IncidentClaim>, localTeamId: String): IncidentOwnership {
    val teams = claims.map { it.teamId }.toSet()
    val mine = localTeamId in teams
    val other = teams.any { it != localTeamId }
    return when { mine && other -> IncidentOwnership.CONFLICT; mine -> IncidentOwnership.CLAIMED_BY_ME; other -> IncidentOwnership.CLAIMED_BY_OTHER; else -> IncidentOwnership.UNCLAIMED }
}

fun canAcceptIncident(status: String, ownership: IncidentOwnership): Boolean =
    status == RescueLifecycle.NEW.name && ownership == IncidentOwnership.UNCLAIMED

data class ClaimReceiptPlan(val shouldProcess: Boolean, val relayPacket: RescueClaimPacket?)

fun planClaimReceipt(packet: RescueClaimPacket, alreadySeen: Boolean): ClaimReceiptPlan = when {
    alreadySeen -> ClaimReceiptPlan(shouldProcess = false, relayPacket = null)
    packet.canRelay() -> ClaimReceiptPlan(shouldProcess = true, relayPacket = packet.nextHop())
    else -> ClaimReceiptPlan(shouldProcess = true, relayPacket = null)
}

fun appendClaimIfNew(existing: List<IncidentClaim>, claim: IncidentClaim): List<IncidentClaim> =
    if (existing.any { it.packetId == claim.packetId }) existing else existing + claim
