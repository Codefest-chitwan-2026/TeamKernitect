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

@Composable
fun ActiveRescuesScreen(activeCount: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.LocalFireDepartment, "Active rescues", Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Active Rescues", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(if (activeCount == 0) "No active rescues" else "$activeCount active rescues", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Rescue actions are not enabled yet", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
