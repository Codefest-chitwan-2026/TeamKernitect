package com.kernitect.sahararesponder.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val CriticalRed = Color(0xFFE60000)
val HighAmber = Color(0xFFE17800)
val NormalBlue = Color(0xFF42647A)

fun priorityRank(priority: String) = when (priority.uppercase()) {
    "CRITICAL" -> 3
    "HIGH" -> 2
    else -> 1
}

fun formatIncidentTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))

@Composable
fun ResponderTopBar(unreadCount: Int) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("SAHARA", color = CriticalRed, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text("Responder Command", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Notifications, contentDescription = "Incident notifications")
            }
            if (unreadCount > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).sizeIn(minWidth = 19.dp, minHeight = 19.dp),
                    shape = CircleShape,
                    color = CriticalRed,
                ) {
                    Box(Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(if (unreadCount > 99) "99+" else unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamIdentityCard(profile: ResponderTeamProfile) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFFFFEEEE)) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(profile.teamName, fontWeight = FontWeight.Bold)
                Text("${profile.callsign} • ${profile.district}", color = CriticalRed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("OFFLINE READY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MeshStatusCard(status: String) {
    val ready = status == "Ready for SOS" || status == "SOS received"
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), color = Color(0xFFF3F4F6)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(10.dp), CircleShape, color = if (ready) Color(0xFF168A48) else HighAmber) {}
            Spacer(Modifier.width(10.dp))
            Column {
                Text("RESCUEMESH", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun IncidentSummaryRow(newCount: Int, activeCount: Int, completedCount: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard("NEW REPORTS", newCount, CriticalRed, Modifier.weight(1f))
        SummaryCard("ACTIVE RESCUES", activeCount, HighAmber, Modifier.weight(1f))
        SummaryCard("COMPLETED", completedCount, Color(0xFF168A48), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, count: Int, accent: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F6))) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 13.dp)) {
            Text(count.toString(), color = accent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(label, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PriorityAlertCard(incident: ResponderIncident?, onViewDetails: (ResponderIncident) -> Unit) {
    if (incident == null) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F6))) {
            Column(Modifier.padding(20.dp)) {
                Text("Command center is clear", fontWeight = FontWeight.Bold)
                Text("Priority SOS reports will be surfaced here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    val accent = priorityColor(incident.priority)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (incident.priority == "CRITICAL") Color(0xFFFFE7E7) else Color(0xFFFFF3DD))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PriorityBadge(incident.priority)
                Text("Priority Alert", color = accent, fontWeight = FontWeight.Bold)
            }
            Text(incident.message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("${formatIncidentTime(incident.timestamp)}  •  Hop ${incident.hopCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { onViewDetails(incident) }, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text("View Details") }
        }
    }
}

@Composable
fun SituationMapCard(incidentCount: Int, responderLocated: Boolean, onOpenMap: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F5))) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), RoundedCornerShape(14.dp), color = Color.White) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.LocationOn, "Situation map", tint = CriticalRed) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Situation Map", fontWeight = FontWeight.Bold)
                Text(
                    if (incidentCount == 0) "Waiting for SOS locations" else "$incidentCount incident location${if (incidentCount == 1) "" else "s"} available",
                    style = MaterialTheme.typography.labelMedium,
                    color = CriticalRed,
                )
                Text(if (responderLocated) "Responder GPS available" else "Locating responder…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onOpenMap) { Text("Open Map") }
        }
    }
}

@Composable
fun CompactIncidentCard(incident: ResponderIncident, assignmentLabel: String? = null, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = CardDefaults.outlinedCardBorder()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PriorityBadge(incident.priority)
                StatusBadge(incident.status)
            }
            Text(incident.message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${formatIncidentTime(incident.timestamp)}  •  Hop ${incident.hopCount}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            assignmentLabel?.let { Text(it, color = CriticalRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
            Text("View Details", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = priorityColor(priority)
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.13f)) {
        Text(priority, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StatusBadge(status: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE9EAEC)) {
        Text(status, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = Color(0xFF40444A), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

fun priorityColor(priority: String) = when (priority.uppercase()) {
    "CRITICAL" -> CriticalRed
    "HIGH" -> HighAmber
    else -> NormalBlue
}
