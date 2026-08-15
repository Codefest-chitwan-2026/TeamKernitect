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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kernitect.saharaandroid.R
import com.kernitect.saharaandroid.data.local.entity.PublicAlertEntity
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.ui.components.SaharaTopBar
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(

    /*
     * SOS/help packets received through RESCUEMESH.
     */
    alerts: List<ReceivedAlert> =
        emptyList(),

    /*
     * Specific SOS/help request to focus.
     */
    focusedAlert: ReceivedAlert? =
        null,

    /*
     * Active public emergency alert for
     * this citizen's current area.
     */
    publicAlert: PublicAlertEntity? =
        null,

    /*
     * Public alert the camera should focus on.
     *
     * Usually set when the user presses MAP
     * on the Home emergency card.
     */
    focusedPublicAlert: PublicAlertEntity? =
        null,

    unreadNotificationCount: Int =
        0,

    onNotificationClick: () -> Unit =
        {},

    onBackClick: (() -> Unit)? =
        null
) {

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        /*
         * =====================================
         * TOP BAR
         * =====================================
         */
        SaharaTopBar(
            title =
                "Map",

            unreadCount =
                unreadNotificationCount,

            onNotificationClick =
                onNotificationClick,

            onBackClick =
                onBackClick
        )


        /*
         * =====================================
         * FIXED MAP VIEWPORT
         * =====================================
         *
         * The Box itself NEVER moves.
         *
         * The OpenStreetMap underneath can still:
         *
         * - drag
         * - pan
         * - pinch zoom
         *
         * Anything outside this rounded viewport
         * is clipped/hidden.
         */
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(
                        1f
                    )
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = 12.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            20.dp
                        )
                    )
        ) {

            /*
             * Map completely fills the fixed
             * viewport above.
             */
            OnlineMap(
                alerts =
                    alerts,

                focusedAlert =
                    focusedAlert,

                publicAlert =
                    publicAlert,

                focusedPublicAlert =
                    focusedPublicAlert
            )


            /*
             * =================================
             * FOCUSED INFORMATION CARD
             * =================================
             *
             * Card also remains inside the
             * clipped map viewport.
             */
            when {

                focusedPublicAlert != null -> {

                    FocusedPublicAlertCard(
                        alert =
                            focusedPublicAlert,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.TopCenter
                                )
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                )
                    )
                }


                focusedAlert != null -> {

                    FocusedAlertCard(
                        alert =
                            focusedAlert,

                        modifier =
                            Modifier
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
}


/*
 * ===========================================
 * OPENSTREETMAP
 * ===========================================
 */

@Composable
private fun OnlineMap(

    alerts: List<ReceivedAlert>,

    focusedAlert: ReceivedAlert?,

    publicAlert: PublicAlertEntity?,

    focusedPublicAlert: PublicAlertEntity?

) {

    AndroidView(

        /*
         * IMPORTANT:
         *
         * The Android MapView fills only the
         * fixed Box created in MapScreen.
         *
         * It does NOT decide its own height.
         */
        modifier =
            Modifier.fillMaxSize(),

        factory = {
                context ->

            MapView(
                context
            ).apply {

                /*
                 * Existing OpenStreetMap setup.
                 */
                setTileSource(
                    TileSourceFactory.MAPNIK
                )


                setUseDataConnection(
                    true
                )


                /*
                 * KEEP MAP INTERACTIVE.
                 *
                 * Dragging + pinch zoom remain
                 * enabled inside the fixed viewport.
                 */
                setMultiTouchControls(
                    true
                )


                /*
                 * Default Bharatpur position.
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
                 * IMPORTANT:
                 *
                 * Keep existing lifecycle behavior.
                 */
                onResume()
            }
        },


        update = {
                mapView ->

            updateMap(
                mapView =
                    mapView,

                alerts =
                    alerts,

                focusedAlert =
                    focusedAlert,

                publicAlert =
                    publicAlert,

                focusedPublicAlert =
                    focusedPublicAlert
            )
        },


        /*
         * IMPORTANT:
         *
         * Only shut the map down when the
         * AndroidView actually leaves Compose.
         */
        onRelease = {
                mapView ->

            mapView.onPause()

            mapView.onDetach()
        }
    )
}


/*
 * ===========================================
 * UPDATE MAP CONTENT
 * ===========================================
 */

private fun updateMap(

    mapView: MapView,

    alerts: List<ReceivedAlert>,

    focusedAlert: ReceivedAlert?,

    publicAlert: PublicAlertEntity?,

    focusedPublicAlert: PublicAlertEntity?

) {

    /*
     * =======================================
     * RESCUEMESH INCIDENTS
     * =======================================
     */

    val alertsToShow =
        alerts.toMutableList()


    /*
     * Always include focused SOS even if it
     * isn't currently in the supplied list.
     */
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
     * =======================================
     * MAP CONTENT SIGNATURE
     * =======================================
     *
     * Prevent unnecessary marker rebuilding.
     */

    val signature =

        alertsToShow
            .joinToString(
                "|"
            ) {

                it.packet.id
            } +

                "::incident=" +

                (
                        focusedAlert
                            ?.packet
                            ?.id
                            ?: ""
                        ) +

                "::public=" +

                (
                        publicAlert
                            ?.id
                            ?: ""
                        ) +

                "::focusedPublic=" +

                (
                        focusedPublicAlert
                            ?.id
                            ?: ""
                        )


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
     * =======================================
     * PUBLIC EMERGENCY ALERT MARKER
     * =======================================
     */

    val publicAlertToShow =

        focusedPublicAlert
            ?: publicAlert


    if (
        publicAlertToShow != null
    ) {

        val point =
            GeoPoint(
                publicAlertToShow.latitude,
                publicAlertToShow.longitude
            )


        val marker =
            Marker(
                mapView
            )


        marker.position =
            point


        /*
         * Leave the default OSM marker icon
         * here so it remains visually different
         * from SOS/help markers.
         */
        marker.setAnchor(
            Marker.ANCHOR_CENTER,
            Marker.ANCHOR_BOTTOM
        )


        marker.title =
            publicAlertToShow.title


        marker.snippet =

            "${
                publicAlertToShow.disasterType
                    .replace(
                        "_",
                        " "
                    )
            } · ${
                publicAlertToShow.severity
                    .replace(
                        "_",
                        " "
                    )
            }"


        mapView.overlays.add(
            marker
        )
    }


    /*
     * =======================================
     * RECEIVED SOS / HELP MARKERS
     * =======================================
     */

    alertsToShow.forEach {
            alert ->

        val packet =
            alert.packet


        val point =
            GeoPoint(
                packet.latitude,
                packet.longitude
            )


        val marker =
            Marker(
                mapView
            )


        marker.position =
            point


        val critical =
            packet.priority ==
                    "CRITICAL"


        marker.icon =
            ContextCompat.getDrawable(
                mapView.context,

                if (
                    critical
                ) {

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
                critical
            ) {

                "Critical SOS"

            } else {

                "Help Request"
            }


        marker.snippet =
            packet.message


        mapView.overlays.add(
            marker
        )
    }


    /*
     * =======================================
     * CAMERA FOCUS
     * =======================================
     */


    /*
     * Public emergency opened from Home.
     */
    if (
        focusedPublicAlert != null
    ) {

        val point =
            GeoPoint(
                focusedPublicAlert.latitude,
                focusedPublicAlert.longitude
            )


        mapView.controller
            .setZoom(
                14.0
            )


        mapView.controller
            .animateTo(
                point
            )
    }


    /*
     * Otherwise focus a selected SOS/help request.
     */
    else if (
        focusedAlert != null
    ) {

        val point =
            GeoPoint(
                focusedAlert.packet.latitude,
                focusedAlert.packet.longitude
            )


        mapView.controller
            .setZoom(
                17.0
            )


        mapView.controller
            .animateTo(
                point
            )
    }


    mapView.invalidate()
}


/*
 * ===========================================
 * FOCUSED PUBLIC ALERT CARD
 * ===========================================
 */

@Composable
private fun FocusedPublicAlertCard(

    alert: PublicAlertEntity,

    modifier: Modifier =
        Modifier

) {

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
                    Color(0xFFFFEEEE)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    6.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Text(
                text =
                    alert.title,

                color =
                    Color(0xFFE60000),

                fontSize =
                    15.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    "${
                        alert.disasterType
                            .replace(
                                "_",
                                " "
                            )
                    } · ${
                        alert.severity
                            .replace(
                                "_",
                                " "
                            )
                    }",

                modifier =
                    Modifier.padding(
                        top = 4.dp
                    ),

                color =
                    Color(0xFF444444),

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    "${alert.municipality}, ${alert.district}",

                modifier =
                    Modifier.padding(
                        top = 3.dp
                    ),

                color =
                    Color(0xFF666666),

                fontSize =
                    12.sp
            )


            Text(
                text =
                    alert.message,

                modifier =
                    Modifier.padding(
                        top = 6.dp
                    ),

                color =
                    Color(0xFF555555),

                fontSize =
                    12.sp,

                maxLines =
                    3
            )
        }
    }
}


/*
 * ===========================================
 * FOCUSED RESCUEMESH INCIDENT CARD
 * ===========================================
 */

@Composable
private fun FocusedAlertCard(

    alert: ReceivedAlert,

    modifier: Modifier =
        Modifier

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

                    if (
                        critical
                    ) {

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
                Modifier.padding(
                    14.dp
                )
        ) {

            Text(
                text =

                    if (
                        critical
                    ) {

                        "CRITICAL SOS"

                    } else {

                        "HELP REQUEST"
                    },

                color =

                    if (
                        critical
                    ) {

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