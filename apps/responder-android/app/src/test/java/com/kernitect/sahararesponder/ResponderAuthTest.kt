package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.model.*
import org.junit.Assert.*
import org.junit.Test

class ResponderAuthTest {
    private val valid = RegistrationInput("River Rescue", "RIVER-1", "Chitwan", "Asha", "9800000000", "leader@example.com", "secret123", "secret123")

    @Test fun registrationValidationAcceptsCompleteInput() = assertNull(validateRegistration(valid))
    @Test fun passwordMustHaveEightCharacters() = assertEquals("Password must be at least 8 characters.", validateRegistration(valid.copy(password = "short", confirmPassword = "short")))
    @Test fun passwordConfirmationMustMatch() = assertEquals("Passwords do not match.", validateRegistration(valid.copy(confirmPassword = "different")))
    @Test fun requiredTeamAndLeaderFieldsAreValidated() {
        assertEquals("Team name is required.", validateRegistration(valid.copy(teamName = "")))
        assertEquals("Callsign is required.", validateRegistration(valid.copy(callsign = "")))
        assertEquals("Leader name is required.", validateRegistration(valid.copy(leaderName = "")))
    }
    @Test fun approvedProfileSupportsOfflineStartupOnlyWithCompleteIdentity() {
        val approved = ResponderRegistration("DEVICE-12345", ResponderRegistrationStatus.APPROVED, "RESP-1", district = "Chitwan", teamId = "TEAM-1", teamName = "River Rescue", callsign = "RIVER-1", authToken = "signed")
        assertNotNull(approved.approvedProfile())
        assertNull(approved.copy(status = ResponderRegistrationStatus.PENDING).approvedProfile())
        assertNull(approved.copy(status = ResponderRegistrationStatus.REJECTED).approvedProfile())
    }
}
