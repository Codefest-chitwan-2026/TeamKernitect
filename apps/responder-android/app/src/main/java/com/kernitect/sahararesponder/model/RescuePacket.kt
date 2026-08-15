package com.kernitect.sahararesponder.model

import org.json.JSONObject

data class RescuePacket(
    val id: String, val type: String, val latitude: Double, val longitude: Double,
    val timestamp: Long, val hopCount: Int, val ttl: Int, val priority: String, val message: String,
) {
    companion object {
        const val TYPE_SOS = "SOS"
        const val PRIORITY_CRITICAL = "CRITICAL"

        fun fromJson(json: String): RescuePacket? {
            return try {
                val value = JSONObject(json)
                val id = value.optString("id").trim()
                val type = value.optString("type").trim()
                if (id.isEmpty() || type.isEmpty()) return null
                RescuePacket(
                    id = id,
                    type = type,
                    latitude = value.optDouble("latitude", 0.0),
                    longitude = value.optDouble("longitude", 0.0),
                    timestamp = value.optLong("timestamp", System.currentTimeMillis()),
                    hopCount = value.optInt("hopCount", 0).coerceAtLeast(0),
                    ttl = value.optInt("ttl", 5).coerceAtLeast(0),
                    priority = value.optString("priority", PRIORITY_CRITICAL).ifBlank { PRIORITY_CRITICAL }.uppercase(),
                    message = value.optString("message", "Critical emergency SOS").ifBlank { "Critical emergency SOS" },
                )
            } catch (_: Exception) { null }
        }
    }
}
