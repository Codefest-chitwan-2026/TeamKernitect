package com.kernitect.sahararesponder.location

data class ResponderLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long,
)
