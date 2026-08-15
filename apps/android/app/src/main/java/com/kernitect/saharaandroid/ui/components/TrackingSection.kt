package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernitect.saharaandroid.data.local.entity.IncidentEntity
import com.kernitect.saharaandroid.data.local.entity.TrackingEventEntity
import com.kernitect.saharaandroid.data.local.entity.TrackingEventType
import com.kernitect.saharaandroid.model.RescuePacket
import com.kernitect.saharaandroid.service.MeshServiceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackingSection(
    incidents: List<IncidentEntity> = emptyList(),
    events: List<TrackingEventEntity> = emptyList(),
    responderDistances: Map<String, MeshServiceState.ResponderDistance> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val eventsByIncident = events.groupBy { it.incidentId }
    val activeIncident = incidents.firstOrNull { incident ->
        eventsByIncident[incident.id].orEmpty().none {
            it.type == TrackingEventType.RESCUED.name
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Rescue Tracking",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (incidents.isEmpty()) {
            Text(
                text = "No rescue requests yet",
                modifier = Modifier.padding(vertical = 10.dp),
                fontSize = 11.sp,
                color = Color(0xFF777777)
            )
            return@Column
        }

        activeIncident?.let { incident ->
            ActiveRescueCard(
                incident = incident,
                events = eventsByIncident[incident.id].orEmpty(),
                responderDistance = responderDistances[incident.id]
            )
        }

        val previousIncidents = incidents.filter { it.id != activeIncident?.id }
        if (previousIncidents.isNotEmpty()) {
            PreviousRequests(previousIncidents, eventsByIncident)
        }
    }
}

@Composable
private fun ActiveRescueCard(
    incident: IncidentEntity,
    events: List<TrackingEventEntity>,
    responderDistance: MeshServiceState.ResponderDistance?
) {
    val packet = RescuePacket.fromJson(incident.packetJson)
    val orderedEvents = events.sortedBy { it.timestamp }
    val relayed = orderedEvents.any { it.type == TrackingEventType.SOS_RELAYED.name }
    val received = orderedEvents.any {
        it.type == TrackingEventType.RESPONDER_RECEIVED.name
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF7F7F7)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "YOUR ACTIVE RESCUE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB3261E)
            )
            Text(
                text = requestLabel(packet),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(incident.receivedAt, "dd MMM yyyy • h:mm a"),
                fontSize = 11.sp,
                color = Color(0xFF666666)
            )

            HorizontalDivider(color = Color(0xFFE1E1E1))

            Text("Current Status", fontSize = 10.sp, color = Color(0xFF666666))
            Text(
                text = "● ${currentStatus(orderedEvents)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF176B3A)
            )

            responderDistance?.let {
                Text(
                    text = "Responder  •  ${it.distanceMeters.toInt()} m away",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeliveryState(
                    label = "RESCUEMESH",
                    complete = relayed,
                    completeText = "Relayed",
                    modifier = Modifier.weight(1f)
                )
                DeliveryState(
                    label = "RESCUE TEAM",
                    complete = received,
                    completeText = "Received",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "ACTIVITY",
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF555555)
            )

            orderedEvents.forEachIndexed { index, event ->
                TimelineEvent(event, index == orderedEvents.lastIndex)
            }

            if (orderedEvents.none { it.type == TrackingEventType.ARRIVED.name }) {
                FutureMilestone("Responder Arrived")
            }
            if (orderedEvents.none { it.type == TrackingEventType.RESCUED.name }) {
                FutureMilestone("Rescue Completed")
            }
        }
    }
}

@Composable
private fun DeliveryState(
    label: String,
    complete: Boolean,
    completeText: String,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = Color.White
    ) {
        Column(Modifier.padding(9.dp)) {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666)
            )
            Text(
                text = if (complete) "✓ $completeText" else "○ Awaiting",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (complete) Color(0xFF176B3A) else Color(0xFF888888)
            )
        }
    }
}

@Composable
private fun TimelineEvent(event: TrackingEventEntity, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = if (isLast) "●" else "✓",
            modifier = Modifier.padding(end = 8.dp),
            fontSize = 11.sp,
            color = Color(0xFF176B3A)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = "${formatTime(event.timestamp, "h:mm:ss a")}  ${event.title}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            event.description?.let {
                Text(text = it, fontSize = 10.sp, color = Color(0xFF666666))
            }
        }
    }
}

@Composable
private fun FutureMilestone(label: String) {
    Text(text = "○  $label", fontSize = 11.sp, color = Color(0xFF9A9A9A))
}

@Composable
private fun PreviousRequests(
    incidents: List<IncidentEntity>,
    eventsByIncident: Map<String, List<TrackingEventEntity>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Previous Requests", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        incidents.forEach { incident ->
            val packet = RescuePacket.fromJson(incident.packetJson)
            val rescued = eventsByIncident[incident.id].orEmpty().any {
                it.type == TrackingEventType.RESCUED.name
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF7F7F7)
            ) {
                Row(
                    modifier = Modifier.padding(11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            formatTime(incident.receivedAt, "dd MMM yyyy"),
                            fontSize = 10.sp,
                            color = Color(0xFF666666)
                        )
                        Text(requestLabel(packet), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (rescued) "RESCUED" else "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rescued) Color(0xFF176B3A) else Color(0xFFB26A00)
                    )
                }
            }
        }
    }
}

private fun currentStatus(events: List<TrackingEventEntity>): String {
    val statuses = setOf(
        TrackingEventType.RESPONDER_RECEIVED.name,
        TrackingEventType.ON_THE_WAY.name,
        TrackingEventType.RESPONDER_NEARBY.name,
        TrackingEventType.ARRIVED.name,
        TrackingEventType.RESCUED.name
    )
    return when (events.lastOrNull { it.type in statuses }?.type) {
        TrackingEventType.RESPONDER_RECEIVED.name -> "Responder Received SOS"
        TrackingEventType.ON_THE_WAY.name -> "Rescue Team On The Way"
        TrackingEventType.RESPONDER_NEARBY.name -> "Responder Nearby"
        TrackingEventType.ARRIVED.name -> "Responder Arrived"
        TrackingEventType.RESCUED.name -> "Rescue Completed"
        else -> if (events.any { it.type == TrackingEventType.SOS_RELAYED.name }) {
            "Relayed — Awaiting Rescue Team"
        } else {
            "Searching for a Relay"
        }
    }
}

private fun requestLabel(packet: RescuePacket?): String =
    if (packet?.priority == RescuePacket.PRIORITY_CRITICAL) {
        "CRITICAL SOS"
    } else {
        "HELP REQUEST"
    }

private fun formatTime(timestamp: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
