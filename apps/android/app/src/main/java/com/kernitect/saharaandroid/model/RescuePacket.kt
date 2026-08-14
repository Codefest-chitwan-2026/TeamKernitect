package com.kernitect.saharaandroid.model

import org.json.JSONObject
import java.util.UUID

data class RescuePacket(
    val id: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val hopCount: Int,
    val ttl: Int
) {

    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("type", type)
            put("latitude", latitude)
            put("longitude", longitude)
            put("timestamp", timestamp)
            put("hopCount", hopCount)
            put("ttl", ttl)
        }.toString()
    }

    fun canRelay(): Boolean {
        return hopCount < ttl
    }

    fun nextHop(): RescuePacket {
        return copy(
            hopCount = hopCount + 1
        )
    }

    companion object {

        const val TYPE_SOS = "SOS"
        const val DEFAULT_TTL = 5

        fun createSos(
            latitude: Double,
            longitude: Double
        ): RescuePacket {

            return RescuePacket(
                id = UUID.randomUUID().toString(),
                type = TYPE_SOS,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                hopCount = 0,
                ttl = DEFAULT_TTL
            )
        }

        fun fromJson(
            json: String
        ): RescuePacket? {

            return try {

                val obj =
                    JSONObject(json)

                RescuePacket(
                    id = obj.getString("id"),
                    type = obj.getString("type"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    timestamp = obj.getLong("timestamp"),
                    hopCount = obj.getInt("hopCount"),
                    ttl = obj.getInt("ttl")
                )

            } catch (_: Exception) {

                null
            }
        }
    }
}