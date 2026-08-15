package com.kernitect.saharaandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
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
    val isRead: Boolean = false,

    /*
     * True for an SOS/help request created by this phone.
     * Local requests belong in rescue tracking, but must not
     * appear as incoming notification alerts.
     */
    @ColumnInfo(defaultValue = "0")
    val isLocalOrigin: Boolean = false
)
