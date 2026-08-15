package com.kernitect.sahararesponder.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderNotification
import com.kernitect.sahararesponder.ui.components.CriticalRed
import com.kernitect.sahararesponder.ui.components.formatLifecycleDateTime

@Composable
fun ResponderNotificationsScreen(
    notifications: List<ResponderNotification>,
    onOpenIncident: (ResponderNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("SAHARA", color = CriticalRed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text("Rescue Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Stored rescue activity available offline", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (notifications.isEmpty()) {
            item { Text("No rescue notifications yet.", Modifier.padding(vertical = 28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(notifications, key = { it.stableId }) { notification ->
                Card(
                    Modifier.fillMaxWidth().clickable { onOpenIncident(notification) },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(notification.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(notification.priority, color = CriticalRed, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(notification.detail, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(notification.incidentId, style = MaterialTheme.typography.labelMedium)
                        Text(formatLifecycleDateTime(notification.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
