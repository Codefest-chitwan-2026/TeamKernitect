package com.kernitect.sahararesponder.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.model.fullReportHistory
import com.kernitect.sahararesponder.ui.components.CompactIncidentCard
import com.kernitect.sahararesponder.ui.components.CriticalRed

@Composable
fun ReportHistoryScreen(reports: List<ResponderIncident>, onBack: () -> Unit, onOpenReport: (ResponderIncident) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Home") }
                Column { Text("SAHARA", color = CriticalRed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black); Text("Recent Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            }
        }
        val history = fullReportHistory(reports)
        if (history.isEmpty()) item { Text("No rescue reports yet.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else items(history, key = { it.id }) { report -> Box(Modifier.padding(horizontal = 20.dp)) { CompactIncidentCard(report) { onOpenReport(report) } } }
    }
}
