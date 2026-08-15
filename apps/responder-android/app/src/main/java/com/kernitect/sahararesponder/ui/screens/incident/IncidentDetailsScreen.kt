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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.location.RescueNavigationCalculator
import com.kernitect.sahararesponder.location.ResponderLocation
import com.kernitect.sahararesponder.ui.components.*

@Composable
fun IncidentDetailsScreen(
    incident: ResponderIncident,
    responderLocation: ResponderLocation?,
    locationStatus: String,
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validVictim = RescueNavigationCalculator.isValidCoordinate(incident.latitude, incident.longitude)
    val distance = responderLocation?.let { RescueNavigationCalculator.distanceMeters(it, incident.latitude, incident.longitude) }
    val bearing = responderLocation?.let { RescueNavigationCalculator.bearingDegrees(it, incident.latitude, incident.longitude) }
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
                Text("No responder has accepted this incident yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
        }
        Surface(shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("ACCEPT RESCUE")
                }
                Text("Available after rescue-link setup", Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
