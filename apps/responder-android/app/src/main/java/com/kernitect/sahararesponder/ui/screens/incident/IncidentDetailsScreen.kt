package com.kernitect.sahararesponder.ui.screens.incident

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import com.kernitect.sahararesponder.location.RescueNavigationCalculator
import com.kernitect.sahararesponder.location.ResponderLocation
import com.kernitect.sahararesponder.mesh.AckRecord
import com.kernitect.sahararesponder.mesh.AckSendState
import com.kernitect.sahararesponder.mesh.ClaimRecord
import com.kernitect.sahararesponder.model.IncidentClaim
import com.kernitect.sahararesponder.model.IncidentOwnership
import com.kernitect.sahararesponder.model.ownershipOf
import com.kernitect.sahararesponder.model.canAcceptIncident
import com.kernitect.sahararesponder.model.SosHandoffRecord
import com.kernitect.sahararesponder.model.SosHandoffState
import com.kernitect.sahararesponder.model.handoffLocksAccept
import com.kernitect.sahararesponder.model.RescueLifecycle
import com.kernitect.sahararesponder.model.RescueStatusEvent
import com.kernitect.sahararesponder.model.latestStatusByTeam
import com.kernitect.sahararesponder.model.RescueLifecycleEvent
import com.kernitect.sahararesponder.mesh.StatusRecord
import com.kernitect.sahararesponder.ui.components.*

@Composable
fun IncidentDetailsScreen(
    incident: ResponderIncident,
    responderLocation: ResponderLocation?,
    locationStatus: String,
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    ackRecord: AckRecord?,
    claimRecord: ClaimRecord?,
    claims: List<IncidentClaim>,
    statusEvents: List<RescueStatusEvent>,
    statusRecord: StatusRecord?,
    lifecycleEvents: List<RescueLifecycleEvent>,
    onAcceptRescue: () -> Unit,
    handoffRecord: SosHandoffRecord?,
    acceptInProgress: Boolean,
    onPassRescue: () -> Unit,
    onRetryAck: () -> Unit,
    onRetryClaim: () -> Unit,
    onAdvanceLifecycle: (RescueLifecycle) -> Unit,
    onRetryStatus: () -> Unit,
    teamProfile: ResponderTeamProfile,
    identityError: String?,
    modifier: Modifier = Modifier,
) {
    var showAcceptDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var pendingTransition by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<RescueLifecycle?>(null) }
    val validVictim = RescueNavigationCalculator.isValidCoordinate(incident.latitude, incident.longitude)
    val distance = responderLocation?.let { RescueNavigationCalculator.distanceMeters(it, incident.latitude, incident.longitude) }
    val bearing = responderLocation?.let { RescueNavigationCalculator.bearingDegrees(it, incident.latitude, incident.longitude) }
    val ownership = ownershipOf(claims, teamProfile.teamId)
    val lifecycle = RescueLifecycle.parse(incident.status) ?: RescueLifecycle.NEW
    val canControl = ownership in setOf(IncidentOwnership.CLAIMED_BY_ME, IncidentOwnership.CONFLICT)
    val progressTeamId = if (lifecycleEvents.any { it.teamId == teamProfile.teamId }) teamProfile.teamId
        else lifecycleEvents.maxByOrNull { it.status.rank }?.teamId
    val progressEvents = lifecycleEvents.filter { it.teamId == progressTeamId }.associateBy { it.status }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back to responder home") }
            Column {
                Text("SAHARA", color = CriticalRed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text("Incident Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PriorityBadge(incident.priority)
                StatusBadge(incident.status)
            }
            Text(incident.message, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            DetailCard("Incident Information") {
                DetailRow("Incident time", formatIncidentTime(incident.timestamp))
                DetailRow("Received", formatIncidentTime(incident.receivedAt))
                DetailRow("Hop count", incident.hopCount.toString())
                DetailRow("Incident ID", incident.id)
            }
            DetailCard("Location") {
                DetailRow("Latitude", if (validVictim) incident.latitude.toString() else "Unavailable")
                DetailRow("Longitude", if (validVictim) incident.longitude.toString() else "Unavailable")
                DetailRow("Responder GPS", responderLocation?.let { "%.6f, %.6f".format(it.latitude, it.longitude) } ?: locationStatus)
                DetailRow("Straight-line distance", distance?.let { RescueNavigationCalculator.formatDistance(it) } ?: "Unavailable")
                DetailRow("Direction", bearing?.let { "${RescueNavigationCalculator.bearingLabel(it)} • ${it.toInt()}°" } ?: "Unavailable")
                Surface(Modifier.fillMaxWidth().height(140.dp), RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.LocationOn, "Incident location", tint = CriticalRed, modifier = Modifier.size(36.dp))
                        Text(if (validVictim) "Victim location ready" else "Victim coordinates unavailable", fontWeight = FontWeight.SemiBold)
                        Text(if (validVictim) "${incident.latitude}, ${incident.longitude}" else "Map marker cannot be placed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = onOpenMap, enabled = validVictim, modifier = Modifier.fillMaxWidth()) { Text("OPEN FULL MAP") }
            }
            DetailCard("Rescue Status") {
                DetailRow("Current status", incident.status)
                DetailRow("Ownership", when (ownership) {
                    IncidentOwnership.UNCLAIMED -> "Unclaimed"
                    IncidentOwnership.CLAIMED_BY_ME -> "Claimed by your team"
                    IncidentOwnership.CLAIMED_BY_OTHER -> "Claimed by another team"
                    IncidentOwnership.CONFLICT -> "CONFLICT"
                })
                claims.distinctBy { it.teamId }.forEach { claim ->
                    Text("${claim.teamName} • ${claim.callsign} (${claim.district})", fontWeight = FontWeight.SemiBold)
                }
                if (ownership == IncidentOwnership.CONFLICT) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text("Multiple responder teams claimed this rescue. Coordinate before proceeding.", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
                if (incident.status == "NEW") {
                    Text("This request has not been accepted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DetailRow("Responder", "${teamProfile.teamName} • ${teamProfile.callsign}")
                } else {
                    Text("Accepted by ${teamProfile.teamName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${teamProfile.callsign} • ${teamProfile.district}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DetailRow("Acknowledgement", ackRecord?.state?.message ?: "Preparing acknowledgement…")
                    DetailRow("Rescue claim", claimRecord?.state?.message ?: "Claim recorded from mesh")
                    claimRecord?.failureReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (claimRecord?.state == AckSendState.FAILED) {
                        OutlinedButton(onClick = onRetryClaim, modifier = Modifier.fillMaxWidth()) { Text("RETRY CLAIM") }
                    }
                    ackRecord?.failureReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (ackRecord?.state == AckSendState.FAILED) {
                        OutlinedButton(onClick = onRetryAck, modifier = Modifier.fillMaxWidth()) { Text("RETRY ACK") }
                    }
                }
                identityError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            }
            DetailCard("Rescue Progress") {
                RescueLifecycle.entries.filter { it != RescueLifecycle.NEW }.forEach { step ->
                    val marker = when { step.rank < lifecycle.rank -> "✓"; step == lifecycle -> "●"; else -> "○" }
                    Text("$marker ${step.displayName}", fontWeight = if (step == lifecycle) FontWeight.Bold else FontWeight.Normal)
                    progressEvents[step]?.let { event ->
                        Text(formatLifecycleDateTime(event.timestamp), Modifier.padding(start = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                latestStatusByTeam(statusEvents).values.forEach { event ->
                    Text("${event.teamName}: ${RescueLifecycle.parse(event.status)?.displayName ?: event.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                statusRecord?.let { record ->
                    DetailRow("Latest mesh status", statusSendMessage(record.state))
                    record.failureReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (record.state == AckSendState.FAILED) OutlinedButton(onClick = onRetryStatus, Modifier.fillMaxWidth()) { Text("RETRY STATUS") }
                }
                if (responderLocation == null) Text("GPS unavailable: this event uses protocol fallback coordinates, not a real location.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DetailCard("Rescue Activity") {
                if (lifecycleEvents.isEmpty()) Text("No lifecycle activity recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                lifecycleEvents.forEach { event ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(formatLifecycleTime(event.timestamp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp))
                        Column {
                            Text(event.activityText(), fontWeight = FontWeight.SemiBold)
                            Text(formatLifecycleDateTime(event.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Surface(shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        if (lifecycle == RescueLifecycle.NEW) showAcceptDialog = true else pendingTransition = lifecycle.next()
                    },
                    enabled = !acceptInProgress && ((canAcceptIncident(incident.status, ownership) && !handoffLocksAccept(handoffRecord)) ||
                        (lifecycle.next() != null && canControl)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(when {
                        lifecycle == RescueLifecycle.RESCUED -> "RESCUE COMPLETE"
                        lifecycle != RescueLifecycle.NEW -> when (lifecycle.next()) {
                            RescueLifecycle.ON_THE_WAY -> "MARK ON THE WAY"
                            RescueLifecycle.NEARBY -> "MARK NEARBY"
                            RescueLifecycle.ARRIVED -> "MARK ARRIVED"
                            RescueLifecycle.RESCUED -> "MARK RESCUED"
                            else -> "RESCUE COMPLETE"
                        }
                        ownership == IncidentOwnership.CLAIMED_BY_OTHER -> "CLAIMED BY ANOTHER TEAM"
                        ownership == IncidentOwnership.CLAIMED_BY_ME -> "CLAIMED BY YOUR TEAM"
                        ownership == IncidentOwnership.CONFLICT -> "CLAIM CONFLICT"
                        else -> "ACCEPT RESCUE"
                    })
                }
                if (lifecycle == RescueLifecycle.NEW && ownership == IncidentOwnership.UNCLAIMED) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onPassRescue,
                        enabled = !acceptInProgress && handoffRecord?.state !in setOf(SosHandoffState.PASSING, SosHandoffState.PASSED, SosHandoffState.TTL_EXHAUSTED),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(when (handoffRecord?.state) {
                            SosHandoffState.PASSING -> "PASSING…"
                            SosHandoffState.PASSED -> "PASSED TO NEXT TEAM"
                            SosHandoffState.FAILED -> "RETRY PASS"
                            SosHandoffState.TTL_EXHAUSTED -> "NO FURTHER MESH HOPS"
                            null -> "BUSY / PASS TO NEXT TEAM"
                        })
                    }
                    handoffRecord?.failureReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (handoffRecord?.state == SosHandoffState.PASSED) Text("SOS handed off to the next reachable rescue team.", color = MaterialTheme.colorScheme.primary)
                    if (handoffRecord?.state == SosHandoffState.TTL_EXHAUSTED) Text("No further mesh hops are available. You may still accept this rescue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    if (incident.status == "NEW") "Confirmation required" else "Local responder assignment active",
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptDialog = false },
            title = { Text("Accept this rescue request?") },
            text = { Text("You will take responsibility for this incident and SAHARA will send an acknowledgement through RESCUEMESH.") },
            dismissButton = { TextButton(onClick = { showAcceptDialog = false }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    showAcceptDialog = false
                    onAcceptRescue()
                }) { Text("Accept Rescue") }
            },
        )
    }
    pendingTransition?.let { next ->
        AlertDialog(
            onDismissRequest = { pendingTransition = null },
            title = { Text("Mark ${next.displayName}?") },
            text = { Text(if (next == RescueLifecycle.RESCUED) "Confirm that the victim has been reached and this rescue is complete." else "Confirm this operational rescue status change.") },
            dismissButton = { TextButton(onClick = { pendingTransition = null }) { Text("Cancel") } },
            confirmButton = { Button(onClick = { pendingTransition = null; onAdvanceLifecycle(next) }) { Text("Confirm") } },
        )
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.38f))
        Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.62f))
    }
}

private fun statusSendMessage(state: AckSendState) = when (state) {
    AckSendState.IDLE -> "Status ready"
    AckSendState.SEARCHING -> "Searching for nearby RESCUEMESH relay…"
    AckSendState.CONNECTING -> "Connecting to nearby relay…"
    AckSendState.SENDING -> "Sending lifecycle status…"
    AckSendState.SENT_TO_MESH -> "Status sent into RESCUEMESH"
    AckSendState.FAILED -> "Status not yet sent to mesh"
}

private fun RescueLifecycleEvent.activityText() = when (status) {
    RescueLifecycle.NEW -> "New rescue reported"
    RescueLifecycle.ACCEPTED -> "Rescue accepted by $teamName"
    RescueLifecycle.ON_THE_WAY -> "$teamName is on the way"
    RescueLifecycle.NEARBY -> "$teamName marked nearby"
    RescueLifecycle.ARRIVED -> "$teamName arrived"
    RescueLifecycle.RESCUED -> "Rescue completed by $teamName"
}
