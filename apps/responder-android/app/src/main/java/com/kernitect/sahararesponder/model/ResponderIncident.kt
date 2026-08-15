package com.kernitect.sahararesponder.model

data class ResponderIncident(
    val id: String, val priority: String, val message: String,
    val latitude: Double, val longitude: Double, val timestamp: Long,
    val hopCount: Int, val ttl: Int = 5, val status: String = "NEW",
    val receivedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun fromPacket(packet: RescuePacket) = ResponderIncident(
            packet.id, packet.priority, packet.message, packet.latitude,
            longitude = packet.longitude,
            timestamp = packet.timestamp,
            hopCount = packet.hopCount,
            ttl = packet.ttl,
        )
    }
}
