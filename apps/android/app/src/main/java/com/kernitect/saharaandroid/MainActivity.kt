package com.kernitect.saharaandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kernitect.saharaandroid.ble.AppRequirements
import com.kernitect.saharaandroid.location.LocationProvider
import com.kernitect.saharaandroid.mesh.MeshEngine
import com.kernitect.saharaandroid.model.RescuePacket
import com.kernitect.saharaandroid.ui.theme.SaharaAndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContent {

            SaharaAndroidTheme {

                var status by remember {
                    mutableStateOf(
                        "Starting..."
                    )
                }

                var locationStatus by remember {
                    mutableStateOf(
                        "Location not requested"
                    )
                }

                var latitude by remember {
                    mutableStateOf<Double?>(
                        null
                    )
                }

                var longitude by remember {
                    mutableStateOf<Double?>(
                        null
                    )
                }

                var accuracy by remember {
                    mutableStateOf<Float?>(
                        null
                    )
                }

                var gettingLocation by remember {
                    mutableStateOf(
                        false
                    )
                }

                var lastReceivedPacket by remember {
                    mutableStateOf<RescuePacket?>(
                        null
                    )
                }

                var lastSentPacket by remember {
                    mutableStateOf<RescuePacket?>(
                        null
                    )
                }

                var permissionsGranted by remember {
                    mutableStateOf(
                        AppRequirements
                            .hasAllRuntimePermissions(
                                this
                            )
                    )
                }

                val locationProvider =
                    remember {

                        LocationProvider(
                            context = this
                        )
                    }

                val meshEngine =
                    remember {

                        MeshEngine(
                            context = this,

                            onStatusChanged = {
                                    newStatus ->

                                status =
                                    newStatus
                            },

                            onPacketReceived = {
                                    packet ->

                                lastReceivedPacket =
                                    packet
                            },

                            onPacketSent = {
                                    packet ->

                                lastSentPacket =
                                    packet
                            }
                        )
                    }

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        contract =
                            ActivityResultContracts
                                .RequestMultiplePermissions()
                    ) {

                        permissionsGranted =
                            AppRequirements
                                .hasAllRuntimePermissions(
                                    this
                                )
                    }

                /*
                 * Ask for required permissions.
                 */
                LaunchedEffect(Unit) {

                    if (!permissionsGranted) {

                        permissionLauncher.launch(
                            AppRequirements
                                .requiredRuntimePermissions()
                        )
                    }
                }

                /*
                 * Start BLE mesh once permissions
                 * are available.
                 */
                LaunchedEffect(
                    permissionsGranted
                ) {

                    if (permissionsGranted) {

                        if (
                            !AppRequirements
                                .hasPreciseLocation(
                                    this@MainActivity
                                )
                        ) {

                            status =
                                "Precise location permission required"

                        } else if (
                            !AppRequirements
                                .isLocationServicesEnabled(
                                    this@MainActivity
                                )
                        ) {

                            status =
                                "Turn on Location services"

                        } else {

                            meshEngine.start()
                        }

                    } else {

                        status =
                            "Waiting for permissions..."
                    }
                }

                DisposableEffect(Unit) {

                    onDispose {

                        meshEngine.stop()
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                24.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    Text(
                        text =
                            "SAHARA DEBUG",

                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )

                    Text(
                        text =
                            "Mesh Status:"
                    )

                    Text(
                        text =
                            status
                    )

                    Text(
                        text =
                            "Location:"
                    )

                    Text(
                        text =
                            locationStatus
                    )

                    /*
                     * REAL SOS BUTTON
                     */
                    Button(
                        enabled =
                            permissionsGranted &&
                                    !gettingLocation,

                        onClick = {

                            if (
                                !AppRequirements
                                    .hasPreciseLocation(
                                        this@MainActivity
                                    )
                            ) {

                                locationStatus =
                                    "Precise location permission required"

                                return@Button
                            }

                            if (
                                !AppRequirements
                                    .isLocationServicesEnabled(
                                        this@MainActivity
                                    )
                            ) {

                                locationStatus =
                                    "Turn on Location services"

                                return@Button
                            }

                            gettingLocation =
                                true

                            locationStatus =
                                "Getting accurate GPS location..."

                            /*
                             * Get fresh GPS FIRST.
                             *
                             * Only create the SOS packet
                             * after we have an acceptable fix.
                             */
                            locationProvider
                                .getCurrentLocation(

                                    onSuccess = {
                                            location ->

                                        gettingLocation =
                                            false

                                        latitude =
                                            location.latitude

                                        longitude =
                                            location.longitude

                                        accuracy =
                                            location.accuracy

                                        locationStatus =
                                            "GPS acquired: " +
                                                    "${location.accuracy.toInt()} m accuracy"

                                        /*
                                         * THIS is where the real
                                         * SOS packet is created.
                                         */
                                        meshEngine.originateSos(
                                            latitude =
                                                location.latitude,

                                            longitude =
                                                location.longitude
                                        )
                                    },

                                    onError = {
                                            error ->

                                        gettingLocation =
                                            false

                                        locationStatus =
                                            error
                                    }
                                )
                        }
                    ) {

                        Text(
                            if (gettingLocation) {
                                "GETTING LOCATION..."
                            } else {
                                "SEND SOS"
                            }
                        )
                    }

                    Button(
                        enabled =
                            permissionsGranted,

                        onClick = {

                            meshEngine
                                .retryPendingForward()
                        }
                    ) {

                        Text(
                            "RETRY PENDING"
                        )
                    }

                    /*
                     * Show real GPS data.
                     */
                    latitude?.let {
                            lat ->

                        Text(
                            text =
                                "Latitude: $lat"
                        )
                    }

                    longitude?.let {
                            lng ->

                        Text(
                            text =
                                "Longitude: $lng"
                        )
                    }

                    accuracy?.let {
                            acc ->

                        Text(
                            text =
                                "Accuracy: ${acc.toInt()} m"
                        )
                    }

                    lastReceivedPacket?.let {
                            packet ->

                        Text(
                            text =
                                "Received SOS:\n" +
                                        "ID: ${packet.id}\n" +
                                        "Hop: ${packet.hopCount}/${packet.ttl}\n" +
                                        "Latitude: ${packet.latitude}\n" +
                                        "Longitude: ${packet.longitude}"
                        )
                    }

                    lastSentPacket?.let {
                            packet ->

                        Text(
                            text =
                                "Sent SOS:\n" +
                                        "ID: ${packet.id}\n" +
                                        "Hop: ${packet.hopCount}/${packet.ttl}\n" +
                                        "Latitude: ${packet.latitude}\n" +
                                        "Longitude: ${packet.longitude}"
                        )
                    }
                }
            }
        }
    }
}