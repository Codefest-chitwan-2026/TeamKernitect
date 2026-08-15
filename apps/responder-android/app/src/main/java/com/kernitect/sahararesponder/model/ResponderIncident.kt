package com.kernitect.sahararesponder.model

data class ResponderIncident(
    val id: String, val priority: String, val message: String,
    val latitude: Double, val longitude: Double, val timestamp: Long,
    val hopCount: Int, val status: String = "NEW",
) {
    companion object {
        fun fromPacket(packet: RescuePacket) = ResponderIncident(
            packet.id, packet.priority, packet.message, packet.latitude,
            packet.longitude, packet.timestamp, packet.hopCount,
        )
    }
}
