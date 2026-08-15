package com.kernitect.sahararesponder.model

enum class ResponderRegistrationStatus { UNREGISTERED, PENDING, APPROVED, REJECTED }

data class ResponderRegistration(
    val deviceId: String,
    val status: ResponderRegistrationStatus = ResponderRegistrationStatus.UNREGISTERED,
    val responderId: String? = null,
    val operatorName: String = "",
    val organization: String = "",
    val phone: String = "",
    val email: String = "",
    val district: String = "Chitwan",
    val teamId: String? = null,
    val teamName: String? = null,
    val callsign: String? = null,
    val rejectionReason: String? = null,
) {
    fun approvedProfile(): ResponderTeamProfile? {
        if (status != ResponderRegistrationStatus.APPROVED) return null
        return ResponderTeamProfile(
            responderId = responderId?.takeIf { it.isNotBlank() } ?: return null,
            teamId = teamId?.takeIf { it.isNotBlank() } ?: return null,
            teamName = teamName?.takeIf { it.isNotBlank() } ?: return null,
            callsign = callsign?.takeIf { it.isNotBlank() } ?: return null,
            district = district,
            deviceId = deviceId,
        )
    }
}
