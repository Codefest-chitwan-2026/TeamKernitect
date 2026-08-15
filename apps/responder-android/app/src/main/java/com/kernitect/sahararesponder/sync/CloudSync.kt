package com.kernitect.sahararesponder.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.kernitect.sahararesponder.identity.ResponderIdentityStore
import com.kernitect.sahararesponder.network.ResponderApiConfig
import com.kernitect.sahararesponder.persistence.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

enum class BackendSyncState { PENDING, SYNCING, SYNCED, FAILED }
data class CloudSyncSummary(val pending: Int, val syncing: Int, val failed: Int) { val synced get() = pending + syncing + failed == 0 }
data class SyncResult(val accepted: Set<String>, val rejected: Map<String, String>)

fun normalizeCloudState(state: String) = if (state == BackendSyncState.SYNCING.name) BackendSyncState.PENDING.name else state
fun selectPending(records: List<OutgoingPacketEntity>, limit: Int = 40) = records
    .filter { it.packetType in setOf("RESCUE_CLAIM", "RESCUE_STATUS") && normalizeCloudState(it.backendSyncState) in setOf("PENDING", "FAILED") }
    .sortedBy { it.createdAt }.take(limit)

class ResponderCloudSyncClient {
    suspend fun sync(profile: com.kernitect.sahararesponder.model.ResponderTeamProfile, events: JSONArray): SyncResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("responderId", profile.responderId).put("deviceId", profile.deviceId)
            .put("teamId", profile.teamId).put("events", events).toString()
        val connection = URL(ResponderApiConfig.BASE_URL + "/responders/sync").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 10_000; connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/json"); connection.doOutput = true
            connection.outputStream.bufferedWriter().use { it.write(body) }
            val responseText = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) error("Sync service returned ${connection.responseCode}")
            val json = JSONObject(responseText); val acceptedArray = json.getJSONArray("acceptedEventIds")
            val accepted = (0 until acceptedArray.length()).map { acceptedArray.getString(it) }.toSet()
            val rejectedArray = json.optJSONArray("rejected") ?: JSONArray()
            val rejected = (0 until rejectedArray.length()).associate { i -> rejectedArray.getJSONObject(i).let { it.getString("eventId") to it.optString("reason", "Rejected by server") } }
            SyncResult(accepted, rejected)
        } finally { connection.disconnect() }
    }
}

class ResponderCloudSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val profile = ResponderIdentityStore(applicationContext).loadApprovedProfile() ?: return Result.failure()
        val dao = ResponderDatabase.get(applicationContext).dao()
        val batch = dao.pendingCloud(40).map { if (it.backendSyncState == "SYNCING") it.copy(backendSyncState = "PENDING") else it }
        if (batch.isEmpty()) return Result.success()
        val ids = batch.map { it.packetId }; dao.updateCloud(ids, "SYNCING", null, System.currentTimeMillis())
        return try {
            val events = JSONArray()
            batch.forEach { record ->
                val packet = JSONObject(record.payloadJson); val incident = dao.incident(record.incidentId)
                events.put(JSONObject().put("eventId", record.packetId).put("eventType", record.packetType)
                    .put("incidentId", record.incidentId).put("responderId", profile.responderId).put("teamId", profile.teamId)
                    .put("status", packet.getString("status")).put("latitude", packet.optDouble("latitude", 0.0))
                    .put("longitude", packet.optDouble("longitude", 0.0)).put("eventTimestamp", packet.getLong("timestamp"))
                    .put("priority", incident?.priority ?: packet.optString("priority", "CRITICAL")).put("message", incident?.message)
                    .put("victimLatitude", incident?.latitude).put("victimLongitude", incident?.longitude))
            }
            val response = ResponderCloudSyncClient().sync(profile, events)
            if (response.accepted.isNotEmpty()) dao.updateCloud(response.accepted.toList(), "SYNCED", null, System.currentTimeMillis())
            response.rejected.forEach { (id, reason) -> dao.updateCloud(listOf(id), "FAILED", reason, System.currentTimeMillis()) }
            val unresolved = ids.toSet() - response.accepted - response.rejected.keys
            if (unresolved.isNotEmpty()) dao.updateCloud(unresolved.toList(), "FAILED", "Server did not acknowledge event", System.currentTimeMillis())
            Log.i(TAG, "Cloud sync accepted ${response.accepted.size} events")
            Result.success()
        } catch (error: Exception) {
            dao.updateCloud(ids, "PENDING", error.message, System.currentTimeMillis())
            Log.w(TAG, "Cloud sync retry scheduled", error); Result.retry()
        }
    }
    companion object { const val TAG = "SaharaResponder" }
}

object CloudSyncScheduler {
    const val UNIQUE_WORK = "sahara-responder-cloud-sync"
    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<ResponderCloudSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request)
    }
}
