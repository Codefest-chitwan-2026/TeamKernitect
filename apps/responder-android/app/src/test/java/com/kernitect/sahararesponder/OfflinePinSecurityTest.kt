package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.identity.*
import com.kernitect.sahararesponder.model.ResponderRegistrationStatus
import org.junit.Assert.*
import org.junit.Test

class OfflinePinSecurityTest {
    private val trusted = TrustedDeviceState(ResponderRegistrationStatus.APPROVED, hasProfile = true, hasToken = true, hasPin = true, locked = true)

    @Test fun approvedTrustedLockedDeviceCanUnlockOffline() = assertTrue(trusted.canUnlockOffline())
    @Test fun brandNewDeviceCannotUnlockOffline() = assertFalse(trusted.copy(status = ResponderRegistrationStatus.UNREGISTERED, hasProfile = false, hasToken = false).canUnlockOffline())
    @Test fun pendingAndRejectedDevicesCannotUnlockOffline() {
        assertFalse(trusted.copy(status = ResponderRegistrationStatus.PENDING).canUnlockOffline())
        assertFalse(trusted.copy(status = ResponderRegistrationStatus.REJECTED).canUnlockOffline())
    }
    @Test fun correctPinVerifiesAndIncorrectPinFails() {
        val record = OfflinePinSecurity.create("123456")
        assertTrue(OfflinePinSecurity.verify("123456", record))
        assertFalse(OfflinePinSecurity.verify("654321", record))
    }
    @Test fun recordNeverContainsPlaintextPin() {
        val record = OfflinePinSecurity.create("482951")
        assertFalse(record.salt.contains("482951")); assertFalse(record.verifier.contains("482951"))
    }
    @Test fun lockKeepsTrustedIdentityAndPinWhilePreventingHome() {
        val active = trusted.copy(locked = false)
        val locked = active.lockedState()
        assertTrue(locked.hasProfile); assertTrue(locked.hasToken); assertTrue(locked.hasPin); assertTrue(locked.locked)
    }
    @Test fun successfulOfflineUnlockRestoresHomeEligibility() {
        val unlocked = trusted.unlockedState(correctPin = true)
        assertFalse(unlocked.locked); assertTrue(unlocked.hasProfile); assertTrue(unlocked.hasPin)
    }
    @Test fun incorrectPinKeepsDeviceLocked() = assertTrue(trusted.unlockedState(correctPin = false).locked)
    @Test fun removeAccountClearsTrustPinAndOfflineEligibility() {
        val removed = trusted.removedState()
        assertFalse(removed.hasProfile); assertFalse(removed.hasToken); assertFalse(removed.hasPin); assertFalse(removed.locked); assertFalse(removed.canUnlockOffline())
    }
}
