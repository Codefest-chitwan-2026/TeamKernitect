package com.kernitect.sahararesponder.model

import com.kernitect.sahararesponder.location.ResponderLocation
import org.json.JSONObject
import java.util.UUID

data class RescueAckPacket(
    override val id: String,
    override val incidentId: String,
    val responderId: String,
    val teamId: String,
    val teamName: String,
    val callsign: String,
    val deviceId: String,
    val district: String,
    val timestamp: Long,
    val priority: String,
    val latitude: Double,
    val longitude: Double,
    val hopCount: Int = 0,
    val ttl: Int = 5,
    override val type: String = "SOS_ACK",
    val status: String = "RESCUE_RECEIVED",
    val message: String = "RESCUE_RECEIVED|$incidentId|$teamId|$teamName|$callsign",
) : MeshOutgoingPacket {
    override fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("incidentId", incidentId)
        put("responderId", responderId)
        put("teamId", teamId)
        put("teamName", teamName)
        put("callsign", callsign)
        put("deviceId", deviceId)
        put("district", district)
        put("status", status)
        put("latitude", latitude)
        put("longitude", longitude)
        put("timestamp", timestamp)
        put("hopCount", hopCount)
        put("ttl", ttl)
        put("priority", priority)
        put("message", message)
    }.toString()

    companion object {
        fun create(
            incident: ResponderIncident,
            responderLocation: ResponderLocation?,
            teamProfile: ResponderTeamProfile,
        ) = RescueAckPacket(
            id = "ACK-${UUID.randomUUID()}",
            incidentId = incident.id,
            responderId = teamProfile.responderId,
            teamId = teamProfile.teamId,
            teamName = teamProfile.teamName,
            callsign = teamProfile.callsign,
            deviceId = teamProfile.deviceId,
            district = teamProfile.district,
            timestamp = System.currentTimeMillis(),
            priority = incident.priority,
            latitude = responderLocation?.latitude ?: 0.0,
            longitude = responderLocation?.longitude ?: 0.0,
        )
    }
}
