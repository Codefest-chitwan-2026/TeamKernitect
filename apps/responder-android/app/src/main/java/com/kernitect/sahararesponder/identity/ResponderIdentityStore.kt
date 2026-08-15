package com.kernitect.sahararesponder.identity

import android.content.Context
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import java.util.UUID

class ResponderIdentityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadProfile(): ResponderTeamProfile? {
        val teamId = preferences.getString(KEY_TEAM_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val teamName = preferences.getString(KEY_TEAM_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val callsign = preferences.getString(KEY_CALLSIGN, null)?.takeIf { it.isNotBlank() } ?: return null
        val district = preferences.getString(KEY_DISTRICT, null)?.takeIf { it.isNotBlank() } ?: return null
        val deviceId = preferences.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return ResponderTeamProfile(teamId, teamName, callsign, district, deviceId)
    }

    fun activate(team: ResponderTeamProfile): ResponderTeamProfile {
        val configured = team.copy(deviceId = getOrCreateDeviceId())
        preferences.edit()
            .putString(KEY_TEAM_ID, configured.teamId)
            .putString(KEY_TEAM_NAME, configured.teamName)
            .putString(KEY_CALLSIGN, configured.callsign)
            .putString(KEY_DISTRICT, configured.district)
            .putString(KEY_DEVICE_ID, configured.deviceId)
            .apply()
        return configured
    }

    private fun getOrCreateDeviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        return "DEVICE-${UUID.randomUUID()}".also {
            preferences.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "sahara_responder_identity"
        const val KEY_TEAM_ID = "team_id"
        const val KEY_TEAM_NAME = "team_name"
        const val KEY_CALLSIGN = "callsign"
        const val KEY_DISTRICT = "district"
        const val KEY_DEVICE_ID = "device_id"
    }
}
