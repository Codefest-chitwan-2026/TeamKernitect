package com.kernitect.sahararesponder.model

import com.kernitect.sahararesponder.location.ResponderLocation
import org.json.JSONObject
import java.util.UUID

data class RescueStatusPacket(
    override val id: String, override val incidentId: String,
    val responderId: String, val teamId: String, val teamName: String, val callsign: String,
    val deviceId: String, val district: String, val status: String,
    val latitude: Double, val longitude: Double, val timestamp: Long,
    val hopCount: Int = 0, val ttl: Int = DEFAULT_TTL, val priority: String,
    override val type: String = TYPE,
    val message: String = "RESCUE_STATUS|$incidentId|$status|$teamId|$teamName|$callsign",
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
    fun asEvent() = RescueStatusEvent(id, incidentId, responderId, teamId, teamName, callsign, status, latitude, longitude, timestamp)

    companion object {
        const val TYPE = "RESCUE_STATUS"
        const val DEFAULT_TTL = 5
        fun create(incident: ResponderIncident, lifecycle: RescueLifecycle, location: ResponderLocation?, profile: ResponderTeamProfile) = RescueStatusPacket(
            id = "STATUS-${UUID.randomUUID()}", incidentId = incident.id, responderId = profile.responderId,
            teamId = profile.teamId, teamName = profile.teamName, callsign = profile.callsign,
            deviceId = profile.deviceId, district = profile.district, status = lifecycle.name,
            latitude = location?.latitude ?: 0.0, longitude = location?.longitude ?: 0.0,
            timestamp = System.currentTimeMillis(), priority = incident.priority,
        )
        fun fromJson(raw: String): RescueStatusPacket? = runCatching {
            val j = JSONObject(raw)
            if (j.optString("type") != TYPE) return null
            RescueStatusPacket(
                id = j.getString("id"), incidentId = j.getString("incidentId"), responderId = j.getString("responderId"),
                teamId = j.getString("teamId"), teamName = j.getString("teamName"), callsign = j.getString("callsign"),
                deviceId = j.getString("deviceId"), district = j.getString("district"), status = j.getString("status"),
                latitude = j.optDouble("latitude", 0.0), longitude = j.optDouble("longitude", 0.0),
                timestamp = j.getLong("timestamp"), hopCount = j.optInt("hopCount", 0), ttl = j.optInt("ttl", DEFAULT_TTL),
                priority = j.optString("priority", "NORMAL"), message = j.optString("message").ifBlank {
                    "RESCUE_STATUS|${j.getString("incidentId")}|${j.getString("status")}|${j.getString("teamId")}|${j.getString("teamName")}|${j.getString("callsign")}"
                },
            ).takeIf { it.id.startsWith("STATUS-") && it.incidentId.isNotBlank() && it.responderId.isNotBlank() &&
                it.teamId.isNotBlank() && it.teamName.isNotBlank() && it.callsign.isNotBlank() && it.deviceId.isNotBlank() &&
                it.district.isNotBlank() && RescueLifecycle.parse(it.status) != null && it.status != RescueLifecycle.NEW.name &&
                it.hopCount >= 0 && it.ttl >= 0 }
        }.getOrNull()
    }
}

data class RescueStatusEvent(
    val packetId: String, val incidentId: String, val responderId: String, val teamId: String,
    val teamName: String, val callsign: String, val status: String,
    val latitude: Double, val longitude: Double, val timestamp: Long,
)

fun latestStatusByTeam(events: List<RescueStatusEvent>): Map<String, RescueStatusEvent> =
    events.groupBy { it.teamId }.mapValues { (_, teamEvents) ->
        teamEvents.maxWith(compareBy<RescueStatusEvent> { RescueLifecycle.parse(it.status)?.rank ?: -1 }.thenBy { it.timestamp })
    }
