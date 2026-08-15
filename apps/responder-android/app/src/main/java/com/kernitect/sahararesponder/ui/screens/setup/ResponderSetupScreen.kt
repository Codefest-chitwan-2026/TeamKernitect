package com.kernitect.sahararesponder.ui.screens.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import com.kernitect.sahararesponder.ui.components.CriticalRed

@Composable
fun ResponderSetupScreen(onActivate: (ResponderTeamProfile) -> Unit, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<ResponderTeamProfile?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("SAHARA RESPONDER", color = CriticalRed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Responder Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Select the rescue team assigned to this responder device.",
                Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF3F4F6)) {
                Text("Offline responder identity • This profile remains available without internet.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        ResponderTeamProfile.prototypeTeams.forEach { team ->
            item(key = team.teamId) {
                val isSelected = selected?.teamId == team.teamId
                Card(
                    Modifier.fillMaxWidth().clickable { selected = team },
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder(isSelected),
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFFEEEE) else Color.White),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isSelected, onClick = { selected = team })
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(team.teamName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(team.teamId, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${team.callsign} • ${team.district}", color = CriticalRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { selected?.let(onActivate) },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            ) { Text("ACTIVATE RESPONDER") }
            Text(
                "Prototype local provisioning. Official team assignment will be provided by SAHARA services later.",
                Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
