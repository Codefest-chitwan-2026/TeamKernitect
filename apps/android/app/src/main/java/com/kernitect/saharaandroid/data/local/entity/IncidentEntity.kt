package com.kernitect.saharaandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(

    @PrimaryKey
    val id: String,

    /*
     * Store the complete RESCUEMESH packet.
     *
     * This is useful because if RescuePacket gets
     * extra fields later, we can still preserve them.
     */
    val packetJson: String,

    /*
     * Time THIS phone received the packet.
     */
    val receivedAt: Long,

    /*
     * Used for the notification bell.
     */
    val isRead: Boolean = false
)