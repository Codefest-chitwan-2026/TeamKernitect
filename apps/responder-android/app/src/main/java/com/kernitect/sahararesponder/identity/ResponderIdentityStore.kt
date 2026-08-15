package com.kernitect.sahararesponder.identity

import android.content.Context
import com.kernitect.sahararesponder.model.ResponderRegistration
import com.kernitect.sahararesponder.model.ResponderRegistrationStatus
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import java.util.UUID

class ResponderIdentityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getOrCreateDeviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        return "DEVICE-${UUID.randomUUID()}".also { preferences.edit().putString(KEY_DEVICE_ID, it).apply() }
    }

    fun loadApprovedProfile(): ResponderTeamProfile? {
        if (preferences.getString(KEY_APPROVAL_STATUS, null) != ResponderRegistrationStatus.APPROVED.name) return null
        if (preferences.getString(KEY_AUTH_TOKEN, null).isNullOrBlank()) return null
        val registration = loadRegistration()
        return registration?.approvedProfile()
    }

    fun loadRegistration(): ResponderRegistration? {
        val deviceId = getOrCreateDeviceId()
        val statusValue = preferences.getString(KEY_APPROVAL_STATUS, null) ?: return null
        val status = runCatching { ResponderRegistrationStatus.valueOf(statusValue) }.getOrNull() ?: return null
        return ResponderRegistration(
            deviceId = deviceId,
            status = status,
            responderId = preferences.getString(KEY_RESPONDER_ID, null),
            operatorName = preferences.getString(KEY_OPERATOR_NAME, "").orEmpty(),
            organization = preferences.getString(KEY_ORGANIZATION, "").orEmpty(),
            phone = preferences.getString(KEY_PHONE, "").orEmpty(),
            email = preferences.getString(KEY_EMAIL, "").orEmpty(),
            district = preferences.getString(KEY_DISTRICT, "Chitwan").orEmpty(),
            teamId = preferences.getString(KEY_TEAM_ID, null),
            teamName = preferences.getString(KEY_TEAM_NAME, null),
            callsign = preferences.getString(KEY_CALLSIGN, null),
            rejectionReason = preferences.getString(KEY_REJECTION_REASON, null),
            authToken = preferences.getString(KEY_AUTH_TOKEN, null),
        )
    }

    fun saveRegistration(registration: ResponderRegistration) {
        preferences.edit()
            .putString(KEY_DEVICE_ID, registration.deviceId)
            .putString(KEY_APPROVAL_STATUS, registration.status.name)
            .putString(KEY_RESPONDER_ID, registration.responderId)
            .putString(KEY_OPERATOR_NAME, registration.operatorName)
            .putString(KEY_ORGANIZATION, registration.organization)
            .putString(KEY_PHONE, registration.phone)
            .putString(KEY_EMAIL, registration.email)
            .putString(KEY_DISTRICT, registration.district)
            .putString(KEY_TEAM_ID, registration.teamId)
            .putString(KEY_TEAM_NAME, registration.teamName)
            .putString(KEY_CALLSIGN, registration.callsign)
            .putString(KEY_REJECTION_REASON, registration.rejectionReason)
            .putString(KEY_AUTH_TOKEN, registration.authToken)
            .apply()
    }

    fun hasOfflinePin() = !preferences.getString(KEY_PIN_SALT, null).isNullOrBlank() && !preferences.getString(KEY_PIN_VERIFIER, null).isNullOrBlank()
    fun isLocked() = preferences.getBoolean(KEY_LOCKED, false)
    fun configureOfflinePin(pin: String) { val record = OfflinePinSecurity.create(pin); preferences.edit().putString(KEY_PIN_SALT, record.salt).putString(KEY_PIN_VERIFIER, record.verifier).putInt(KEY_PIN_ITERATIONS, record.iterations).apply() }
    fun verifyOfflinePin(pin: String): Boolean {
        val salt = preferences.getString(KEY_PIN_SALT, null) ?: return false
        val verifier = preferences.getString(KEY_PIN_VERIFIER, null) ?: return false
        return OfflinePinSecurity.verify(pin, OfflinePinRecord(salt, verifier, preferences.getInt(KEY_PIN_ITERATIONS, OfflinePinRecord.ITERATIONS)))
    }
    fun lockApp(): Boolean { if (loadApprovedProfile() == null || !hasOfflinePin()) return false; preferences.edit().putBoolean(KEY_LOCKED, true).apply(); return true }
    fun unlockApp() { preferences.edit().putBoolean(KEY_LOCKED, false).apply() }
    fun removeAccount() {
        val deviceId = getOrCreateDeviceId()
        preferences.edit().clear().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "sahara_responder_identity"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_APPROVAL_STATUS = "approval_status"
        const val KEY_RESPONDER_ID = "responder_id"
        const val KEY_OPERATOR_NAME = "operator_name"
        const val KEY_ORGANIZATION = "organization"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
        const val KEY_DISTRICT = "district"
        const val KEY_TEAM_ID = "team_id"
        const val KEY_TEAM_NAME = "team_name"
        const val KEY_CALLSIGN = "callsign"
        const val KEY_REJECTION_REASON = "rejection_reason"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_PIN_SALT = "offline_pin_salt"
        const val KEY_PIN_VERIFIER = "offline_pin_verifier"
        const val KEY_PIN_ITERATIONS = "offline_pin_iterations"
        const val KEY_LOCKED = "auth_locked"
    }
}
