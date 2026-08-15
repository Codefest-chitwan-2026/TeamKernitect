package com.kernitect.sahararesponder.model

data class ResponderTeamProfile(
    val responderId: String,
    val teamId: String,
    val teamName: String,
    val callsign: String,
    val district: String,
    val deviceId: String,
    val approvalStatus: String = "APPROVED",
)
