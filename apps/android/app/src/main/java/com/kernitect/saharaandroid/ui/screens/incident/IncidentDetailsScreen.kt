package com.kernitect.saharaandroid.ui.screens.incident

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.view.MotionEvent
import androidx.compose.ui.draw.clip
import com.kernitect.saharaandroid.R
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.model.WitnessReport
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun IncidentDetailsScreen(
    alert: ReceivedAlert,

    witnessReports: List<WitnessReport>,

    onBackClick: () -> Unit,

    onOpenFullMap: () -> Unit,

    onAddDetails: (
        disasterType: String,
        peopleCount: String,
        explanation: String
    ) -> Unit
) {

    var showAddDetailsDialog by remember {
        mutableStateOf(false)
    }

    /*
     * Witness form.
     */
    var disasterType by remember {
        mutableStateOf("")
    }

    var peopleCount by remember {
        mutableStateOf("")
    }

    var explanation by remember {
        mutableStateOf("")
    }

    var mapIsBeingTouched by remember {
        mutableStateOf(false)
    }

    val screenScrollState =
        rememberScrollState()

    val packet =
        alert.packet

    val critical =
        packet.priority ==
                "CRITICAL"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                state = screenScrollState,
                enabled = !mapIsBeingTouched
            )
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        /*
         * Header
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {

            IconButton(
                onClick =
                    onBackClick,

                modifier =
                    Modifier.align(
                        Alignment.CenterStart
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,

                    contentDescription =
                        "Back",

                    tint =
                        Color.Black
                )
            }

            Text(
                text =
                    "Incident Details",

                modifier =
                    Modifier.align(
                        Alignment.Center
                    ),

                fontSize =
                    19.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color.Black
            )
        }

        /*
         * Incident information
         */
        Card(
            modifier =
                Modifier.fillMaxWidth(),

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
                            Color(0xFFFFF5E8)
                        }
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
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
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        packet.message,

                    modifier =
                        Modifier.padding(
                            top = 8.dp
                        ),

                    color =
                        Color.Black,

                    fontSize =
                        13.sp
                )

                Text(
                    text =
                        "Received: ${formatTime(alert.receivedAt)}",

                    modifier =
                        Modifier.padding(
                            top = 10.dp
                        ),

                    color =
                        Color(0xFF666666),

                    fontSize =
                        11.sp
                )

                Text(
                    text =
                        "Relay hop: ${packet.hopCount}/${packet.ttl}",

                    modifier =
                        Modifier.padding(
                            top = 3.dp
                        ),

                    color =
                        Color(0xFF666666),

                    fontSize =
                        11.sp
                )
            }
        }

        /*
         * Incident map
         */
        Text(
            text = "Location",

            fontSize = 15.sp,

            fontWeight =
                FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                )
        ) {

            IncidentMap(
                alert = alert,

                modifier =
                    Modifier.fillMaxSize(),

                onTouchChanged = {
                        touching ->

                    mapIsBeingTouched =
                        touching
                }
            )
        }

        Button(
            onClick =
                onOpenFullMap,

            modifier =
                Modifier.align(
                    Alignment.End
                ),

            shape =
                RoundedCornerShape(
                    50
                ),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFFFFE5E5),

                    contentColor =
                        Color.Black
                )
        ) {

            Text(
                text =
                    "Open Full Map",

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }

        /*
         * Witness section
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "Nearby Witness Reports",

                modifier =
                    Modifier.weight(
                        1f
                    ),

                fontSize =
                    15.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Button(
                onClick = {

                    showAddDetailsDialog =
                        true
                },

                shape =
                    RoundedCornerShape(
                        50
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFFFFE5E5),

                        contentColor =
                            Color.Black
                    )
            ) {

                Text(
                    text =
                        "Add Details",

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        if (
            witnessReports.isEmpty()
        ) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFF5F5F5)
                    )
            ) {

                Text(
                    text =
                        "No nearby witness reports yet.",

                    modifier =
                        Modifier.padding(
                            16.dp
                        ),

                    color =
                        Color(0xFF777777),

                    fontSize =
                        12.sp
                )
            }

        } else {

            witnessReports.forEach {
                    report ->

                WitnessReportCard(
                    report =
                        report
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    30.dp
                )
        )
    }

    /*
     * ============================
     * WITNESS REPORT FORM
     * ============================
     */
    if (
        showAddDetailsDialog
    ) {

        AlertDialog(
            onDismissRequest = {

                showAddDetailsDialog =
                    false
            },

            title = {

                Text(
                    text =
                        "Add Incident Details",

                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            "Share what you can see near this incident.",

                        fontSize =
                            12.sp,

                        color =
                            Color(0xFF666666)
                    )

                    IncidentDropdown(
                        modifier =
                            Modifier.padding(
                                top = 14.dp
                            ),

                        placeholder =
                            "Type of disaster",

                        selectedValue =
                            disasterType,

                        options =
                            listOf(
                                "Earthquake",
                                "Flood",
                                "Fire",
                                "Landslide",
                                "Medical",
                                "Other"
                            ),

                        onSelected = {

                            disasterType =
                                it
                        }
                    )

                    IncidentDropdown(
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            ),

                        placeholder =
                            "Number of people",

                        selectedValue =
                            peopleCount,

                        options =
                            listOf(
                                "1",
                                "2 - 5",
                                "6 - 10",
                                "11 - 20",
                                "20+"
                            ),

                        onSelected = {

                            peopleCount =
                                it
                        }
                    )

                    Text(
                        text =
                            "Explain (Optional)",

                        modifier =
                            Modifier.padding(
                                top = 14.dp,
                                bottom = 7.dp
                            ),

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    TextField(
                        value =
                            explanation,

                        onValueChange = {

                            explanation =
                                it
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),

                        placeholder = {

                            Text(
                                text =
                                    "Example: Building partially collapsed..."
                            )
                        },

                        shape =
                            RoundedCornerShape(
                                7.dp
                            ),

                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor =
                                    Color(0xFFE4E4E4),

                                unfocusedContainerColor =
                                    Color(0xFFE4E4E4),

                                focusedIndicatorColor =
                                    Color.Transparent,

                                unfocusedIndicatorColor =
                                    Color.Transparent
                            )
                    )
                }
            },

            confirmButton = {

                TextButton(
                    enabled =
                        disasterType
                            .isNotBlank() &&
                                peopleCount
                                    .isNotBlank(),

                    onClick = {

                        onAddDetails(
                            disasterType,
                            peopleCount,
                            explanation.trim()
                        )

                        /*
                         * Reset form.
                         */
                        disasterType =
                            ""

                        peopleCount =
                            ""

                        explanation =
                            ""

                        showAddDetailsDialog =
                            false
                    }
                ) {

                    Text(
                        text =
                            "Submit Details",

                        color =
                            Color(0xFFE60000),

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        showAddDetailsDialog =
                            false
                    }
                ) {

                    Text(
                        text =
                            "Cancel",

                        color =
                            Color.Black
                    )
                }
            }
        )
    }
}

@Composable
private fun IncidentDropdown(
    placeholder: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            modifier.fillMaxWidth()
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clickable {

                    expanded =
                        true
                },

            shape =
                RoundedCornerShape(
                    7.dp
                ),

            color =
                Color(0xFFE4E4E4)
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        horizontal = 13.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        if (
                            selectedValue
                                .isBlank()
                        ) {
                            placeholder
                        } else {
                            selectedValue
                        },

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    fontSize =
                        12.sp,

                    color =
                        if (
                            selectedValue
                                .isBlank()
                        ) {
                            Color(0xFF777777)
                        } else {
                            Color.Black
                        }
                )

                Text(
                    text = "▼",
                    fontSize = 9.sp
                )
            }
        }

        DropdownMenu(
            expanded =
                expanded,

            onDismissRequest = {

                expanded =
                    false
            }
        ) {

            options.forEach {
                    option ->

                DropdownMenuItem(
                    text = {

                        Text(
                            text =
                                option
                        )
                    },

                    onClick = {

                        onSelected(
                            option
                        )

                        expanded =
                            false
                    }
                )
            }
        }
    }
}

@Composable
private fun IncidentMap(
    alert: ReceivedAlert,
    modifier: Modifier = Modifier,
    onTouchChanged: (Boolean) -> Unit = {}
) {

    val packet =
        alert.packet

    AndroidView(
        modifier =
            modifier,

        factory = {
                context ->

            val point =
                GeoPoint(
                    packet.latitude,
                    packet.longitude
                )

            MapView(
                context
            ).apply {

                setTileSource(
                    TileSourceFactory.MAPNIK
                )

                setUseDataConnection(
                    true
                )

                setMultiTouchControls(
                    true
                )

                controller.setZoom(
                    16.5
                )

                controller.setCenter(
                    point
                )

                /*
                 * IMPORTANT:
                 *
                 * When the user touches this map,
                 * stop the outer Incident Details
                 * screen from scrolling.
                 */
                setOnTouchListener {
                        view,
                        event ->

                    when (
                        event.actionMasked
                    ) {

                        MotionEvent.ACTION_DOWN -> {

                            view.parent
                                ?.requestDisallowInterceptTouchEvent(
                                    true
                                )

                            onTouchChanged(
                                true
                            )
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {

                            view.parent
                                ?.requestDisallowInterceptTouchEvent(
                                    false
                                )

                            onTouchChanged(
                                false
                            )
                        }
                    }

                    /*
                     * false means the MapView itself
                     * still receives the touch event.
                     *
                     * So zoom/pan continues working.
                     */
                    false
                }

                val marker =
                    Marker(
                        this
                    )

                marker.position =
                    point

                val drawable =
                    ContextCompat.getDrawable(
                        context,

                        if (
                            packet.priority ==
                            "CRITICAL"
                        ) {
                            R.drawable.sos_marker
                        } else {
                            R.drawable.help_marker
                        }
                    )

                if (
                    drawable != null
                ) {

                    marker.icon =
                        drawable

                } else {

                    marker.setTextIcon(
                        "SOS"
                    )
                }

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

                overlays.add(
                    marker
                )

                onResume()
            }
        },

        onRelease = {
                mapView ->

            /*
             * Re-enable page scrolling
             * in case the map disappears while
             * being touched.
             */
            onTouchChanged(
                false
            )

            mapView.setOnTouchListener(
                null
            )

            mapView.onPause()

            mapView.onDetach()
        }
    )
}

@Composable
private fun WitnessReportCard(
    report: WitnessReport
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                8.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFF4F4F4)
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
                    report.disasterType,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    13.sp
            )

            Text(
                text =
                    "People observed: ${report.peopleCount}",

                modifier =
                    Modifier.padding(
                        top = 4.dp
                    ),

                color =
                    Color(0xFF555555),

                fontSize =
                    11.sp
            )

            if (
                report.message
                    .isNotBlank()
            ) {

                Text(
                    text =
                        report.message,

                    modifier =
                        Modifier.padding(
                            top = 7.dp
                        ),

                    color =
                        Color.Black,

                    fontSize =
                        12.sp
                )
            }

            Text(
                text =
                    formatTime(
                        report.createdAt
                    ),

                modifier =
                    Modifier.padding(
                        top = 7.dp
                    ),

                color =
                    Color(0xFF777777),

                fontSize =
                    10.sp
            )
        }
    }
}

private fun formatTime(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        )

    return formatter.format(
        Date(timestamp)
    )
}