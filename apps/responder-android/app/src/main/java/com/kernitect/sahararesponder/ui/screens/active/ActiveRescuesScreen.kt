package com.kernitect.sahararesponder.ui.screens.active

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import com.kernitect.sahararesponder.model.IncidentClaim
import com.kernitect.sahararesponder.model.IncidentOwnership
import com.kernitect.sahararesponder.model.ownershipOf
import com.kernitect.sahararesponder.model.RescueLifecycleEvent
import com.kernitect.sahararesponder.ui.components.CompactIncidentCard
import com.kernitect.sahararesponder.ui.components.formatLifecycleTime

@Composable
fun ActiveRescuesScreen(
    incidents: List<ResponderIncident>,
    teamProfile: ResponderTeamProfile,
    claimsByIncident: Map<String, List<IncidentClaim>>,
    lifecycleEventsByIncident: Map<String, List<RescueLifecycleEvent>>,
    onViewDetails: (ResponderIncident) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (incidents.isEmpty()) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.LocalFireDepartment, "Active rescues", Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(14.dp))
            Text("Active Rescues", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("No active rescues", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Active Rescues", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${incidents.size} accepted rescue${if (incidents.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(incidents, key = { it.id }) { incident ->
                val ownership = ownershipOf(claimsByIncident[incident.id].orEmpty(), teamProfile.teamId)
                val currentEvent = lifecycleEventsByIncident[incident.id].orEmpty()
                    .filter { it.teamId == teamProfile.teamId }.maxByOrNull { it.status.rank }
                val progress = currentEvent?.let { "${it.status.displayName.uppercase()} • since ${formatLifecycleTime(it.timestamp)}" }
                val assignment = if (ownership == IncidentOwnership.CONFLICT) "CLAIM CONFLICT • your team involved" else "Assigned to ${teamProfile.teamName} • ${teamProfile.callsign}"
                val label = listOfNotNull(assignment, progress).joinToString(" • ")
                CompactIncidentCard(incident, assignmentLabel = label) { onViewDetails(incident) }
            }
        }
    }
}
