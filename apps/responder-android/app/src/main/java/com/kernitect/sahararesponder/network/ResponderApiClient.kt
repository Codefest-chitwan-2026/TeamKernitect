package com.kernitect.sahararesponder.network

import android.os.Handler
import android.os.Looper
import com.kernitect.sahararesponder.model.ResponderRegistration
import com.kernitect.sahararesponder.model.ResponderRegistrationStatus
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ResponderApiClient {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun register(registration: ResponderRegistration, callback: (Result<ResponderRegistration>) -> Unit) {
        val body = JSONObject().apply {
            put("deviceId", registration.deviceId)
            put("teamName", registration.organization)
            put("callsign", registration.callsign)
            put("district", registration.district)
            put("leaderName", registration.operatorName)
            put("leaderPhone", registration.phone)
            put("leaderEmail", registration.email)
            put("password", registration.password)
        }.toString()
        request("/responders/register", "POST", body, registration.deviceId, callback)
    }

    fun login(email: String, password: String, deviceId: String, callback: (Result<ResponderRegistration>) -> Unit) {
        val body = JSONObject().put("leaderEmail", email).put("password", password).put("deviceId", deviceId).toString()
        request("/responders/login", "POST", body, deviceId, callback, loginResponse = true)
    }

    fun checkStatus(deviceId: String, callback: (Result<ResponderRegistration>) -> Unit) {
        request("/responders/device/${URLEncoder.encode(deviceId, Charsets.UTF_8.name())}", "GET", null, deviceId, callback)
    }

    private fun request(path: String, method: String, body: String?, deviceId: String, callback: (Result<ResponderRegistration>) -> Unit, loginResponse: Boolean = false) {
        Thread {
            val result = runCatching {
                val connection = (URL(ResponderApiConfig.BASE_URL + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Content-Type", "application/json")
                    if (body != null) {
                        doOutput = true
                        outputStream.bufferedWriter().use { it.write(body) }
                    }
                }
                try {
                    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                    val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (connection.responseCode !in 200..299) {
                        val detail = runCatching { JSONObject(response).optString("detail") }.getOrNull()
                        error(detail?.takeIf { it.isNotBlank() } ?: "Registration service returned ${connection.responseCode}.")
                    }
                    val json = JSONObject(response)
                    if (loginResponse) parseRegistration(json.getJSONObject("profile"), deviceId).copy(authToken = json.getString("accessToken"))
                    else parseRegistration(json, deviceId)
                } finally { connection.disconnect() }
            }
            mainHandler.post { callback(result) }
        }.start()
    }

    private fun parseRegistration(json: JSONObject, deviceId: String) = ResponderRegistration(
        deviceId = json.optString("deviceId", deviceId),
        status = runCatching { ResponderRegistrationStatus.valueOf(json.getString("status")) }.getOrElse { error("Malformed registration status.") },
        responderId = json.nullableString("responderId"),
        operatorName = json.optString("operatorName"),
        organization = json.optString("organization"),
        phone = json.optString("phone"),
        email = json.optString("email"),
        district = json.optString("district", "Chitwan"),
        teamId = json.nullableString("teamId"),
        teamName = json.nullableString("teamName"),
        callsign = json.nullableString("callsign"),
        rejectionReason = json.nullableString("rejectionReason"),
    )

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
