package com.kernitect.saharaandroid.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_events",
    indices = [
        Index(value = ["incidentId"]),
        Index(
            value = ["incidentId", "type"],
            unique = true
        )
    ]
)
data class TrackingEventEntity(

    @PrimaryKey
    val id: String,

    val incidentId: String,

    val type: String,

    val title: String,

    val description: String?,

    val timestamp: Long,

    val distanceMeters: Double? = null
)

enum class TrackingEventType {
    SOS_CREATED,
    LOCATION_ATTACHED,
    SOS_RELAYED,
    RESPONDER_RECEIVED,
    ON_THE_WAY,
    RESPONDER_NEARBY,
    ARRIVED,
    RESCUED
}
