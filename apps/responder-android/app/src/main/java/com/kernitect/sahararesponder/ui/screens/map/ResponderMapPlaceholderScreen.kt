package com.kernitect.sahararesponder.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResponderMapPlaceholderScreen(incidentCount: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Map, "Situation map", Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("Situation Map", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("$incidentCount reports ready for mapping", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Available in Checkpoint 3", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
