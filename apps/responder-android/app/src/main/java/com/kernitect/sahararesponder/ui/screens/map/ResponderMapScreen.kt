package com.kernitect.sahararesponder.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kernitect.sahararesponder.R
import com.kernitect.sahararesponder.location.RescueNavigationCalculator
import com.kernitect.sahararesponder.location.ResponderLocation
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.ui.components.CriticalRed
import com.kernitect.sahararesponder.ui.components.PriorityBadge
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun ResponderMapScreen(
    incidents: List<ResponderIncident>,
    responderLocation: ResponderLocation?,
    locationStatus: String,
    focusedIncident: ResponderIncident? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val validIncidents = incidents.filter { RescueNavigationCalculator.isValidCoordinate(it.latitude, it.longitude) }
    val distance = focusedIncident?.let { responderLocation?.let { location -> RescueNavigationCalculator.distanceMeters(location, it.latitude, it.longitude) } }
    val bearing = focusedIncident?.let { responderLocation?.let { location -> RescueNavigationCalculator.bearingDegrees(location, it.latitude, it.longitude) } }

    Column(modifier.fillMaxSize()) {
        MapHeader(focused = focusedIncident != null, onBack = onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            ResponderMapView(
                incidents = validIncidents,
                responderLocation = responderLocation,
                focusedIncident = focusedIncident,
                onMapAvailable = { mapView = it },
            )
            if (validIncidents.isEmpty() && focusedIncident == null) {
                Surface(
                    Modifier.align(Alignment.TopCenter).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 3.dp,
                ) { Text("No SOS locations received yet", Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold) }
            }
            if (focusedIncident != null) {
                FocusedMapCard(
                    incident = focusedIncident,
                    responderLocation = responderLocation,
                    locationStatus = locationStatus,
                    distance = distance,
                    bearing = bearing,
                    onCenterVictim = {
                        if (RescueNavigationCalculator.isValidCoordinate(focusedIncident.latitude, focusedIncident.longitude)) {
                            mapView?.controller?.animateTo(GeoPoint(focusedIncident.latitude, focusedIncident.longitude))
                            mapView?.controller?.setZoom(17.0)
                        }
                    },
                    onCenterResponder = {
                        responderLocation?.takeIf { RescueNavigationCalculator.isValidCoordinate(it.latitude, it.longitude) }?.let {
                            mapView?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                            mapView?.controller?.setZoom(17.0)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                )
            } else {
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 3.dp,
                ) {
                    Text(
                        if (responderLocation == null) locationStatus else "${validIncidents.size} SOS location${if (validIncidents.size == 1) "" else "s"} • Responder located",
                        Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MapHeader(focused: Boolean, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (focused) IconButton(onClick = { onBack?.invoke() }) { Icon(Icons.Filled.ArrowBack, "Back to incident details") }
        else Spacer(Modifier.width(12.dp))
        Column {
            Text("SAHARA", color = CriticalRed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(if (focused) "Incident Map" else "Situation Map", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResponderMapView(
    incidents: List<ResponderIncident>,
    responderLocation: ResponderLocation?,
    focusedIncident: ResponderIncident?,
    onMapAvailable: (MapView?) -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setUseDataConnection(true)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(27.68, 84.43))
                onResume()
                onMapAvailable(this)
            }
        },
        update = { map -> updateMap(map, incidents, responderLocation, focusedIncident) },
        onRelease = { map ->
            onMapAvailable(null)
            map.onPause()
            map.onDetach()
        },
    )
}

private data class MapRuntimeState(val signature: String, val cameraInitialized: Boolean)

private fun updateMap(map: MapView, incidents: List<ResponderIncident>, responder: ResponderLocation?, focused: ResponderIncident?) {
    val allIncidents = incidents.toMutableList().apply {
        if (focused != null && none { it.id == focused.id } && RescueNavigationCalculator.isValidCoordinate(focused.latitude, focused.longitude)) add(focused)
    }
    val signature = buildString {
        allIncidents.forEach { append("${it.id}:${it.latitude}:${it.longitude}:${it.priority}|") }
        append("responder:${responder?.latitude}:${responder?.longitude}|focus:${focused?.id}")
    }
    val prior = map.tag as? MapRuntimeState
    if (prior?.signature == signature) return
    map.overlays.clear()
    allIncidents.forEach { incident ->
        Marker(map).apply {
            position = GeoPoint(incident.latitude, incident.longitude)
            icon = ContextCompat.getDrawable(map.context, markerResource(incident.priority))
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "${incident.priority} SOS"
            snippet = "${incident.message}\nStatus: ${incident.status}"
            relatedObject = incident.id
            map.overlays.add(this)
        }
    }
    responder?.takeIf { RescueNavigationCalculator.isValidCoordinate(it.latitude, it.longitude) }?.let { location ->
        Marker(map).apply {
            position = GeoPoint(location.latitude, location.longitude)
            icon = ContextCompat.getDrawable(map.context, R.drawable.marker_responder)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Responder"
            snippet = "You • Accuracy ${location.accuracyMeters.toInt()} m"
            map.overlays.add(this)
        }
    }
    val cameraInitialized = prior?.cameraInitialized == true
    if (!cameraInitialized) {
        val initial = focused?.takeIf { RescueNavigationCalculator.isValidCoordinate(it.latitude, it.longitude) }?.let { GeoPoint(it.latitude, it.longitude) }
            ?: allIncidents.maxByOrNull { it.receivedAt }?.let { GeoPoint(it.latitude, it.longitude) }
            ?: responder?.takeIf { RescueNavigationCalculator.isValidCoordinate(it.latitude, it.longitude) }?.let { GeoPoint(it.latitude, it.longitude) }
        if (initial != null) {
            map.controller.setCenter(initial)
            map.controller.setZoom(if (focused != null) 17.0 else 13.0)
        }
    }
    map.tag = MapRuntimeState(signature, true)
    map.invalidate()
}

private fun markerResource(priority: String) = when (priority.uppercase()) {
    "CRITICAL" -> R.drawable.marker_victim_critical
    "HIGH" -> R.drawable.marker_victim_high
    else -> R.drawable.marker_victim_normal
}

@Composable
private fun FocusedMapCard(
    incident: ResponderIncident,
    responderLocation: ResponderLocation?,
    locationStatus: String,
    distance: Float?,
    bearing: Float?,
    onCenterVictim: () -> Unit,
    onCenterResponder: () -> Unit,
    modifier: Modifier,
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PriorityBadge(incident.priority)
                Text(if (distance == null) "Distance unavailable" else RescueNavigationCalculator.formatDistance(distance), fontWeight = FontWeight.Bold)
            }
            Text(incident.message, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (bearing == null) locationStatus else "Direction ${RescueNavigationCalculator.bearingLabel(bearing)} • ${bearing.toInt()}° (straight line)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Victim: %.6f, %.6f".format(incident.latitude, incident.longitude), style = MaterialTheme.typography.bodySmall)
            Text(
                responderLocation?.let { "Responder: %.6f, %.6f".format(it.latitude, it.longitude) } ?: "Responder: $locationStatus",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCenterVictim, modifier = Modifier.weight(1f)) { Text("CENTER VICTIM") }
                OutlinedButton(onClick = onCenterResponder, enabled = responderLocation != null, modifier = Modifier.weight(1f)) { Text("CENTER ME") }
            }
        }
    }
}
