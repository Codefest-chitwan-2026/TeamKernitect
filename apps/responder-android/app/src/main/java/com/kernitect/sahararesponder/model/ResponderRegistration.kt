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
    val password: String = "",
    val authToken: String? = null,
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

data class RegistrationInput(
    val teamName: String, val callsign: String, val district: String, val leaderName: String,
    val leaderPhone: String, val leaderEmail: String, val password: String, val confirmPassword: String,
)

fun validateRegistration(input: RegistrationInput): String? = when {
    input.teamName.isBlank() -> "Team name is required."
    input.callsign.isBlank() -> "Callsign is required."
    input.district.isBlank() -> "District is required."
    input.leaderName.isBlank() -> "Leader name is required."
    input.leaderPhone.isBlank() -> "Phone is required."
    !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(input.leaderEmail.trim()) -> "Enter a valid email address."
    input.password.length < 8 -> "Password must be at least 8 characters."
    input.password != input.confirmPassword -> "Passwords do not match."
    else -> null
}
