package com.kernitect.saharaandroid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "public_alerts")
data class PublicAlertEntity(

    @PrimaryKey
    val id: String,

    val province: String,

    val district: String,

    val municipality: String,

    val disasterType: String,

    val title: String,

    val latitude: Double,

    val longitude: Double,

    val affectedRadiusMeters: Double,

    val severity: String,

    val message: String,

    val startsAt: Long,

    val expiresAt: Long,

    val isDemo: Boolean
)