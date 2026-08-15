package com.kernitect.sahararesponder.model

import android.content.Context

enum class ResponderNotificationType { NEW_SOS, ASSIGNED, STATUS, RESCUED }

data class ResponderNotification(
    val stableId: String,
    val incidentId: String,
    val type: ResponderNotificationType,
    val title: String,
    val detail: String,
    val timestamp: Long,
    val priority: String,
)

fun responderNotifications(
    incidents: List<ResponderIncident>,
    eventsByIncident: Map<String, List<RescueLifecycleEvent>>,
): List<ResponderNotification> = incidents.flatMap { incident ->
    val received = ResponderNotification(
        stableId = "${incident.id}|NEW_SOS|${incident.receivedAt}",
        incidentId = incident.id,
        type = ResponderNotificationType.NEW_SOS,
        title = "New rescue request received",
        detail = incident.message,
        timestamp = incident.receivedAt,
        priority = incident.priority,
    )
    listOf(received) + eventsByIncident[incident.id].orEmpty().map { event ->
        val type = when (event.status) {
            RescueLifecycle.ACCEPTED -> ResponderNotificationType.ASSIGNED
            RescueLifecycle.RESCUED -> ResponderNotificationType.RESCUED
            else -> ResponderNotificationType.STATUS
        }
        ResponderNotification(
            stableId = "${incident.id}|${type.name}|${event.timestamp}",
            incidentId = incident.id,
            type = type,
            title = when (event.status) {
                RescueLifecycle.ACCEPTED -> "Rescue assigned to ${event.teamName}"
                RescueLifecycle.RESCUED -> "Rescue completed"
                else -> "Rescue marked ${event.status.displayName.uppercase()}"
            },
            detail = "${event.teamName} • ${event.callsign}",
            timestamp = event.timestamp,
            priority = incident.priority,
        )
    }
}.sortedByDescending { it.timestamp }

fun resolveNotificationIncident(notification: ResponderNotification, incidents: List<ResponderIncident>): ResponderIncident? =
    incidents.firstOrNull { it.id == notification.incidentId }

fun unreadNotificationCount(notifications: List<ResponderNotification>, seenIds: Set<String>): Int =
    notifications.count { it.stableId !in seenIds }

fun markCurrentNotificationsSeen(seenIds: Set<String>, notifications: List<ResponderNotification>): Set<String> =
    seenIds + notifications.map { it.stableId }

class ResponderNotificationSeenStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("responder_notification_seen", Context.MODE_PRIVATE)
    fun load(): Set<String> = preferences.getStringSet(KEY, emptySet()).orEmpty().toSet()
    fun save(ids: Set<String>) { preferences.edit().putStringSet(KEY, ids.toSet()).apply() }
    private companion object { const val KEY = "seen_notification_ids" }
}
