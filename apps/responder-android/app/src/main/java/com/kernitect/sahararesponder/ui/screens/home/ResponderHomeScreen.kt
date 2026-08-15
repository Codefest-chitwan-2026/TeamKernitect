package com.kernitect.sahararesponder.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.ui.theme.SaharaResponderTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun ResponderHomeScreen(meshStatus: String, incidents: List<ResponderIncident>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(20.dp))
            Text("SAHARA Responder", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Emergency response console", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionTitle("RESCUEMESH status")
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(meshStatus, Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
            }
        }
        item { SectionTitle("Incoming SOS") }
        if (incidents.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No SOS received", fontWeight = FontWeight.SemiBold)
                        Text("Nearby citizen alerts will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else items(incidents, key = { it.id }) { IncidentCard(it) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable private fun IncidentCard(incident: ResponderIncident) {
    val critical = incident.priority == "CRITICAL"
    val accent = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val container = if (critical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(incident.priority, color = accent, fontWeight = FontWeight.ExtraBold)
                Text(incident.status, Modifier.background(accent, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(incident.message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Latitude: ${incident.latitude}")
            Text("Longitude: ${incident.longitude}")
            Text("Packet time: ${DateFormat.getDateTimeInstance().format(Date(incident.timestamp))}")
            Text("Hop count: ${incident.hopCount}")
        }
    }
}

@Preview(showBackground = true) @Composable private fun ResponderHomePreview() {
    SaharaResponderTheme {
        ResponderHomeScreen("Ready for SOS", listOf(ResponderIncident("preview", "CRITICAL", "Critical emergency SOS", 27.7172, 85.3240, 0L, 1)))
    }
}
