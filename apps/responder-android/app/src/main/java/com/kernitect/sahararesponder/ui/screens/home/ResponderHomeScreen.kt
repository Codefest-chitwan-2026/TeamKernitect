package com.kernitect.sahararesponder.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import com.kernitect.sahararesponder.model.IncidentClaim
import com.kernitect.sahararesponder.model.IncidentOwnership
import com.kernitect.sahararesponder.model.ownershipOf
import com.kernitect.sahararesponder.model.RescueLifecycleEvent
import com.kernitect.sahararesponder.ui.components.*
import com.kernitect.sahararesponder.sync.CloudSyncSummary

private val activeStatuses = setOf("ACCEPTED", "ON_THE_WAY", "NEARBY", "ARRIVED")

@Composable
fun ResponderHomeScreen(
    meshStatus: String,
    incidents: List<ResponderIncident>,
    onViewDetails: (ResponderIncident) -> Unit,
    responderLocated: Boolean,
    onOpenMap: () -> Unit,
    teamProfile: ResponderTeamProfile,
    claimsByIncident: Map<String, List<IncidentClaim>>,
    lifecycleEventsByIncident: Map<String, List<RescueLifecycleEvent>>,
    cloudSyncSummary: CloudSyncSummary,
    onSyncNow: () -> Unit,
    onOpenNotifications: () -> Unit,
    unreadNotificationCount: Int,
    modifier: Modifier = Modifier,
) {
    val newest = incidents.sortedByDescending { it.receivedAt }
    val priorityIncident = incidents.maxWithOrNull(
        compareBy<ResponderIncident> { priorityRank(it.priority) }.thenBy { it.receivedAt },
    )
    val newCount = incidents.count { it.status == "NEW" }
    val activeCount = incidents.count { it.status in activeStatuses }
    val completedCount = incidents.count { it.status == "RESCUED" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ResponderTopBar(unreadNotificationCount, onOpenNotifications) }
        item { Box(Modifier.padding(horizontal = 20.dp)) { TeamIdentityCard(teamProfile) } }
        item { Box(Modifier.padding(horizontal = 20.dp)) { MeshStatusCard(meshStatus) } }
        item { Box(Modifier.padding(horizontal = 20.dp)) { CloudSyncCard(cloudSyncSummary, onSyncNow) } }
        item { Box(Modifier.padding(horizontal = 20.dp)) { IncidentSummaryRow(newCount, activeCount, completedCount) } }
        item { SectionHeader("Priority Alert") }
        item { Box(Modifier.padding(horizontal = 20.dp)) { PriorityAlertCard(priorityIncident, onViewDetails) } }
        item { SectionHeader("Situation Overview") }
        item { Box(Modifier.padding(horizontal = 20.dp)) { SituationMapCard(incidents.size, responderLocated, onOpenMap) } }
        item { SectionHeader("Recent Reports") }
        if (newest.isEmpty()) {
            item {
                Text(
                    "No incoming SOS reports yet.",
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(newest, key = { it.id }) { incident ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    val incidentClaims = claimsByIncident[incident.id].orEmpty()
                    val ownership = ownershipOf(incidentClaims, teamProfile.teamId)
                    val latest = lifecycleEventsByIncident[incident.id].orEmpty().maxByOrNull { it.timestamp }
                    val progress = latest?.let { "${it.status.displayName} • ${formatLifecycleTime(it.timestamp)}" }
                    CompactIncidentCard(incident, assignmentLabel = listOfNotNull(ownership.label(incidentClaims, teamProfile.teamId), progress).joinToString(" • ")) { onViewDetails(incident) }
                }
            }
        }
    }
}

@Composable
private fun CloudSyncCard(summary: CloudSyncSummary, onSyncNow: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CLOUD SYNC", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(when {
                    summary.syncing > 0 -> "Syncing • ${summary.syncing} updates"
                    summary.failed > 0 -> "${summary.failed} sync failed • Retry"
                    summary.pending > 0 -> "Offline or waiting • ${summary.pending} updates pending"
                    else -> "Synced"
                }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!summary.synced) OutlinedButton(onClick = onSyncNow) { Text("SYNC NOW") }
        }
    }
}

private fun IncidentOwnership.label(claims: List<IncidentClaim>, localTeamId: String) = when (this) {
    IncidentOwnership.UNCLAIMED -> "Unclaimed"
    IncidentOwnership.CLAIMED_BY_ME -> "Claimed by your team"
    IncidentOwnership.CLAIMED_BY_OTHER -> claims.firstOrNull { it.teamId != localTeamId }
        ?.let { "Assigned to ${it.teamName} • ${it.callsign}" } ?: "Claimed by another team"
    IncidentOwnership.CONFLICT -> "Claim conflict"
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}
