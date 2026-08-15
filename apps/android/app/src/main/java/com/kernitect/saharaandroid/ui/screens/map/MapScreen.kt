package com.kernitect.saharaandroid.ui.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.ui.components.SaharaTopBar
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.content.ContextCompat
import com.kernitect.saharaandroid.R

@Composable
fun MapScreen(
    alerts: List<ReceivedAlert> = emptyList(),
    focusedAlert: ReceivedAlert? = null,
    unreadNotificationCount: Int = 0,
    onNotificationClick: () -> Unit = {}
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        SaharaTopBar(
            title = "Map",
            unreadCount =
                unreadNotificationCount,
            onNotificationClick =
                onNotificationClick
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            OnlineMap(
                alerts = alerts,
                focusedAlert = focusedAlert
            )

            focusedAlert?.let { alert ->

                FocusedAlertCard(
                    alert = alert,
                    modifier = Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 12.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun OnlineMap(
    alerts: List<ReceivedAlert>,
    focusedAlert: ReceivedAlert?
) {

    AndroidView(
        modifier = Modifier.fillMaxSize(),

        factory = { context ->

            MapView(context).apply {

                /*
                 * Online OpenStreetMap tiles.
                 */
                setTileSource(
                    TileSourceFactory.MAPNIK
                )

                setUseDataConnection(
                    true
                )

                setMultiTouchControls(
                    true
                )

                /*
                 * Initial position.
                 */
                controller.setZoom(
                    12.0
                )

                controller.setCenter(
                    GeoPoint(
                        27.68,
                        84.43
                    )
                )

                /*
                 * Start map lifecycle.
                 */
                onResume()
            }
        },

        update = { mapView ->

            updateMap(
                mapView = mapView,
                alerts = alerts,
                focusedAlert = focusedAlert
            )
        },

        /*
         * IMPORTANT:
         *
         * Only shut the map down when the
         * AndroidView actually leaves Compose.
         */
        onRelease = { mapView ->

            mapView.onPause()
            mapView.onDetach()
        }
    )
}
private fun updateMap(
    mapView: MapView,
    alerts: List<ReceivedAlert>,
    focusedAlert: ReceivedAlert?
) {

    /*
     * IMPORTANT:
     *
     * Always include focusedAlert even if
     * the alerts list wasn't passed correctly.
     */
    val alertsToShow =
        alerts.toMutableList()

    if (
        focusedAlert != null &&
        alertsToShow.none {
            it.packet.id ==
                    focusedAlert.packet.id
        }
    ) {

        alertsToShow.add(
            focusedAlert
        )
    }

    /*
     * Only rebuild when our alert data changes.
     */
    val signature =
        alertsToShow
            .joinToString("|") {
                it.packet.id
            } +
                "::" +
                (focusedAlert
                    ?.packet
                    ?.id
                    ?: "")

    if (
        mapView.tag ==
        signature
    ) {

        return
    }

    mapView.tag =
        signature

    mapView.overlays.clear()

    /*
     * Add markers.
     */
    alertsToShow.forEach { alert ->

        val packet =
            alert.packet

        val point =
            GeoPoint(
                packet.latitude,
                packet.longitude
            )

        val marker =
            Marker(mapView)

        marker.position =
            point

        val critical =
            packet.priority == "CRITICAL"

        marker.icon =
            ContextCompat.getDrawable(
                mapView.context,
                if (critical) {
                    R.drawable.sos_marker
                } else {
                    R.drawable.help_marker
                }
            )

        marker.setAnchor(
            Marker.ANCHOR_CENTER,
            Marker.ANCHOR_CENTER
        )

        marker.title =
            if (
                packet.priority ==
                "CRITICAL"
            ) {
                "Critical SOS"
            } else {
                "Help Request"
            }

        marker.snippet =
            packet.message

        /*
         * THIS is what actually puts
         * the marker onto the map.
         */
        mapView.overlays.add(
            marker
        )
    }

    /*
     * Focus camera on selected SOS.
     */
    if (focusedAlert != null) {

        val point =
            GeoPoint(
                focusedAlert.packet.latitude,
                focusedAlert.packet.longitude
            )

        mapView.controller
            .setZoom(17.0)

        mapView.controller
            .animateTo(point)
    }

    mapView.invalidate()
}

@Composable
private fun FocusedAlertCard(
    alert: ReceivedAlert,
    modifier: Modifier = Modifier
) {

    val packet =
        alert.packet

    val critical =
        packet.priority ==
                "CRITICAL"

    Card(
        modifier =
            modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                10.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (critical) {
                        Color(0xFFFFEEEE)
                    } else {
                        Color.White
                    }
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    6.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {

            Text(
                text =
                    if (critical) {
                        "CRITICAL SOS"
                    } else {
                        "HELP REQUEST"
                    },

                color =
                    if (critical) {
                        Color(0xFFE60000)
                    } else {
                        Color(0xFFDD7600)
                    },

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    packet.message,

                modifier =
                    Modifier.padding(
                        top = 5.dp
                    ),

                color =
                    Color(0xFF555555),

                fontSize =
                    12.sp,

                maxLines =
                    2
            )
        }
    }
}