package com.kernitect.sahararesponder.model

import org.json.JSONObject

data class RescuePacket(
    override val id: String, override val type: String, val latitude: Double, val longitude: Double,
    val timestamp: Long, val hopCount: Int, val ttl: Int, val priority: String, val message: String,
) : MeshOutgoingPacket {
    override val incidentId: String get() = id

    override fun toJson() = JSONObject().apply {
        put("id", id); put("type", type); put("latitude", latitude); put("longitude", longitude)
        put("timestamp", timestamp); put("hopCount", hopCount); put("ttl", ttl)
        put("priority", priority); put("message", message)
    }.toString()

    fun canRelay() = hopCount < ttl
    fun nextHop() = copy(hopCount = hopCount + 1)

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
                    latitude = value.getDouble("latitude"),
                    longitude = value.getDouble("longitude"),
                    timestamp = value.getLong("timestamp"),
                    hopCount = value.optInt("hopCount", 0),
                    ttl = value.optInt("ttl", 5),
                    priority = value.optString("priority", PRIORITY_CRITICAL).ifBlank { PRIORITY_CRITICAL }.uppercase(),
                    message = value.optString("message", "Critical emergency SOS").ifBlank { "Critical emergency SOS" },
                ).takeIf {
                    it.type == TYPE_SOS && it.timestamp > 0 && it.hopCount >= 0 && it.ttl >= 0 &&
                        it.latitude.isFinite() && it.longitude.isFinite() &&
                        it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
                }
            } catch (_: Exception) { null }
        }
    }
}

data class SosReceiptPlan(val shouldProcess: Boolean)

fun planSosReceipt(alreadySeen: Boolean): SosReceiptPlan = SosReceiptPlan(shouldProcess = !alreadySeen)

enum class SosHandoffState { PASSING, PASSED, FAILED, TTL_EXHAUSTED }

data class SosHandoffRecord(
    val relayPacket: RescuePacket?, val state: SosHandoffState, val failureReason: String? = null,
)

fun ResponderIncident.originalSosPacket() = RescuePacket(
    id = id, type = RescuePacket.TYPE_SOS, latitude = latitude, longitude = longitude,
    timestamp = timestamp, hopCount = hopCount, ttl = ttl, priority = priority, message = message,
)

fun prepareSosHandoff(incident: ResponderIncident, existing: SosHandoffRecord?): SosHandoffRecord =
    when (existing?.state) {
        SosHandoffState.PASSING, SosHandoffState.PASSED -> existing
        SosHandoffState.FAILED -> existing.copy(state = SosHandoffState.PASSING, failureReason = null)
        SosHandoffState.TTL_EXHAUSTED -> existing
        null -> incident.originalSosPacket().let { packet ->
            if (packet.canRelay()) SosHandoffRecord(packet.nextHop(), SosHandoffState.PASSING)
            else SosHandoffRecord(null, SosHandoffState.TTL_EXHAUSTED)
        }
    }

fun handoffLocksAccept(record: SosHandoffRecord?): Boolean =
    record?.state in setOf(SosHandoffState.PASSING, SosHandoffState.PASSED)
