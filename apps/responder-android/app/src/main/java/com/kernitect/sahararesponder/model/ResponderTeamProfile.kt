package com.kernitect.sahararesponder.model

data class ResponderTeamProfile(
    val teamId: String,
    val teamName: String,
    val callsign: String,
    val district: String,
    val deviceId: String,
) {
    companion object {
        val prototypeTeams = listOf(
            ResponderTeamProfile("BAGMATI-ALPHA-01", "Team Alpha", "ALPHA-1", "Chitwan", ""),
            ResponderTeamProfile("BAGMATI-BETA-01", "Team Beta", "BETA-1", "Chitwan", ""),
            ResponderTeamProfile("BAGMATI-GAMMA-01", "Team Gamma", "GAMMA-1", "Chitwan", ""),
        )
    }
}
