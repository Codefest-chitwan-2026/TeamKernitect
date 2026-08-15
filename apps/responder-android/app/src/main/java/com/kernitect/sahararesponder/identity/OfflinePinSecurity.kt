package com.kernitect.sahararesponder.identity

import com.kernitect.sahararesponder.model.ResponderRegistrationStatus
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class OfflinePinRecord(val salt: String, val verifier: String, val iterations: Int = ITERATIONS) {
    companion object { const val ITERATIONS = 120_000 }
}

object OfflinePinSecurity {
    fun create(pin: String): OfflinePinRecord {
        require(pin.matches(Regex("\\d{6}"))) { "Offline PIN must contain exactly 6 digits." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return OfflinePinRecord(Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(derive(pin, salt, OfflinePinRecord.ITERATIONS)))
    }

    fun verify(pin: String, record: OfflinePinRecord): Boolean = runCatching {
        val expected = Base64.getDecoder().decode(record.verifier)
        val actual = derive(pin, Base64.getDecoder().decode(record.salt), record.iterations)
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
}

data class TrustedDeviceState(val status: ResponderRegistrationStatus, val hasProfile: Boolean, val hasToken: Boolean, val hasPin: Boolean, val locked: Boolean)
fun TrustedDeviceState.canUnlockOffline() = status == ResponderRegistrationStatus.APPROVED && hasProfile && hasToken && hasPin && locked
fun TrustedDeviceState.lockedState() = if (status == ResponderRegistrationStatus.APPROVED && hasProfile && hasToken && hasPin) copy(locked = true) else this
fun TrustedDeviceState.unlockedState(correctPin: Boolean) = if (correctPin && canUnlockOffline()) copy(locked = false) else this
fun TrustedDeviceState.removedState() = TrustedDeviceState(ResponderRegistrationStatus.UNREGISTERED, false, false, false, false)
