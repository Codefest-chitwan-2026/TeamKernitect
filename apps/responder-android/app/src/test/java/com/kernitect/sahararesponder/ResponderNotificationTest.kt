package com.kernitect.sahararesponder

import com.kernitect.sahararesponder.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ResponderNotificationTest {
    private val incident = ResponderIncident(
        id = "SOS-123", priority = "HIGH", message = "Medical help", latitude = 27.6,
        longitude = 84.4, timestamp = 100L, hopCount = 0, receivedAt = 200L,
    )

    @Test fun notificationsKeepOriginalIncidentIdAndSortNewestFirst() {
        val arrived = RescueLifecycleEvent(RescueLifecycle.ARRIVED, 400L, "TEAM-A", "Team Alpha", "ALPHA-1", "STATUS-1")
        val accepted = RescueLifecycleEvent(RescueLifecycle.ACCEPTED, 300L, "TEAM-A", "Team Alpha", "ALPHA-1", "CLAIM-1")
        val notifications = responderNotifications(listOf(incident), mapOf(incident.id to listOf(accepted, arrived)))
        assertEquals(listOf(400L, 300L, 200L), notifications.map { it.timestamp })
        assertEquals(setOf(incident.id), notifications.map { it.incidentId }.toSet())
        assertEquals(ResponderNotificationType.STATUS, notifications.first().type)
    }

    @Test fun notificationRouteResolvesTheCorrectPersistedIncident() {
        val other = incident.copy(id = "SOS-OTHER")
        val notification = responderNotifications(listOf(incident), emptyMap()).single()
        assertEquals(incident, resolveNotificationIncident(notification, listOf(other, incident)))
    }

    @Test fun unreadCountTracksSeenSnapshotWithoutDeletingHistory() {
        val incidents = (1..10).map { incident.copy(id = "SOS-$it", receivedAt = it.toLong()) }
        val initial = responderNotifications(incidents, emptyMap())
        assertEquals(10, unreadNotificationCount(initial, emptySet()))

        val seen = markCurrentNotificationsSeen(emptySet(), initial)
        assertEquals(0, unreadNotificationCount(initial, seen))
        assertEquals(10, initial.size)

        val later = responderNotifications(incidents + incident.copy(id = "SOS-11", receivedAt = 11L), emptyMap())
        assertEquals(1, unreadNotificationCount(later, seen))
        assertEquals(11, later.size)
    }

    @Test fun stableIdsSurviveRecreatingDerivedNotifications() {
        val first = responderNotifications(listOf(incident), emptyMap())
        val recreated = responderNotifications(listOf(incident.copy()), emptyMap())
        assertEquals(first.map { it.stableId }, recreated.map { it.stableId })
    }
}
