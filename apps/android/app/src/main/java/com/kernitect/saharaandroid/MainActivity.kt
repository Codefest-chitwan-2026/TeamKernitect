package com.kernitect.saharaandroid

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

                Surface(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    SaharaScreen()
                }
            }
        }
    }
}

@Composable
fun SaharaScreen() {

    val context =
        LocalContext.current

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    var meshStatus by remember {

        mutableStateOf(
            "Waiting for device readiness"
        )
    }

    var locationStatus by remember {

        mutableStateOf(
            "Location not requested"
        )
    }

    var latitude by remember {
        mutableStateOf<Double?>(null)
    }

    var longitude by remember {
        mutableStateOf<Double?>(null)
    }

    var locationAccuracy by remember {
        mutableStateOf<Float?>(null)
    }

    var lastSentPacket by remember {

        mutableStateOf<RescuePacket?>(
            null
        )
    }

    var lastReceivedPacket by remember {

        mutableStateOf<RescuePacket?>(
            null
        )
    }

    var isGettingLocation by remember {
        mutableStateOf(false)
    }

    val locationProvider =
        remember {

            LocationProvider(
                context.applicationContext
            )
        }

    val meshEngine =
        remember {

            MeshEngine(
                context =
                    context.applicationContext,

                onStatusChanged = {
                    meshStatus = it
                },

                onPacketReceived = {
                    lastReceivedPacket = it
                },

                onPacketSent = {
                    lastSentPacket = it
                }
            )
        }

    DisposableEffect(Unit) {

        onDispose {

            meshEngine.stop()
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestMultiplePermissions()
        ) {

            refreshKey++
        }

    val bluetoothLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartActivityForResult()
        ) {

            refreshKey++
        }

    val locationSettingsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartActivityForResult()
        ) {

            refreshKey++
        }

    @Suppress("UNUSED_VARIABLE")
    val currentRefreshKey =
        refreshKey

    val supportsBle =
        AppRequirements
            .supportsBle(context)

    val permissionsGranted =
        AppRequirements
            .hasAllRuntimePermissions(
                context
            )

    val preciseLocationGranted =
        AppRequirements
            .hasPreciseLocationPermission(
                context
            )

    val bluetoothEnabled =
        AppRequirements
            .isBluetoothEnabled(
                context
            )

    val locationEnabled =
        AppRequirements
            .isLocationEnabled(
                context
            )

    val ready =
        supportsBle &&
                permissionsGranted &&
                preciseLocationGranted &&
                bluetoothEnabled &&
                locationEnabled

    /*
     * As soon as the device is ready, Sahara becomes
     * a RESCUEMESH receiver/relay automatically.
     */
    LaunchedEffect(ready) {

        if (ready) {

            meshEngine.start()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),

        verticalArrangement =
            Arrangement.Top
    ) {

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text = "SAHARA",

            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )

        Text(
            text =
                "RESCUEMESH",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        RequirementRow(
            "BLE supported",
            supportsBle
        )

        RequirementRow(
            "Permissions granted",
            permissionsGranted
        )

        RequirementRow(
            "Precise location",
            preciseLocationGranted
        )

        RequirementRow(
            "Bluetooth enabled",
            bluetoothEnabled
        )

        RequirementRow(
            "Location enabled",
            locationEnabled
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        if (!permissionsGranted) {

            Button(
                onClick = {

                    permissionLauncher.launch(
                        AppRequirements
                            .requiredRuntimePermissions()
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Grant Permissions"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )
        }

        if (!bluetoothEnabled) {

            Button(
                onClick = {

                    bluetoothLauncher.launch(
                        Intent(
                            BluetoothAdapter
                                .ACTION_REQUEST_ENABLE
                        )
                    )
                },

                enabled =
                    permissionsGranted,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Enable Bluetooth"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )
        }

        if (!locationEnabled) {

            Button(
                onClick = {

                    locationSettingsLauncher.launch(
                        Intent(
                            Settings
                                .ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Enable Location"
                )
            }
        }

        if (!ready) {

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                "Complete device setup before sending SOS."
            )

            return@Column
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text = "Mesh Status",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Text(
            text =
                meshStatus
        )

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        Button(
            onClick = {

                if (isGettingLocation) {
                    return@Button
                }

                isGettingLocation =
                    true

                locationStatus =
                    "Getting accurate GPS location..."

                locationProvider
                    .getCurrentLocation(

                        onSuccess = {
                                location ->

                            latitude =
                                location.latitude

                            longitude =
                                location.longitude

                            locationAccuracy =
                                location.accuracy

                            locationStatus =
                                "Location captured"

                            val packet =
                                meshEngine
                                    .originateSos(
                                        latitude =
                                            location.latitude,

                                        longitude =
                                            location.longitude
                                    )

                            lastSentPacket =
                                packet

                            isGettingLocation =
                                false
                        },

                        onError = {
                                error ->

                            locationStatus =
                                error

                            isGettingLocation =
                                false
                        }
                    )
            },

            enabled =
                !isGettingLocation,

            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {

            Text(
                text =
                    if (isGettingLocation) {
                        "GETTING LOCATION..."
                    } else {
                        "SEND SOS"
                    },

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                "Location: $locationStatus"
        )

        latitude?.let {

            Text(
                text =
                    "Latitude: $it"
            )
        }

        longitude?.let {

            Text(
                text =
                    "Longitude: $it"
            )
        }

        locationAccuracy?.let {

            Text(
                text =
                    "Accuracy: ${it.toInt()} meters"
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        lastSentPacket?.let { packet ->

            Text(
                text =
                    "Latest Outgoing SOS",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            PacketDetails(
                packet = packet
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )
        }

        lastReceivedPacket?.let { packet ->

            Text(
                text =
                    "Latest Received SOS",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            PacketDetails(
                packet = packet
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )
        }

        /*
         * Debug fallback.
         *
         * Useful if a relay scan timed out because
         * the next device wasn't available yet.
         */
        Button(
            onClick = {

                meshEngine
                    .retryPendingForward()
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Retry Pending Relay"
            )
        }

        Spacer(
            modifier =
                Modifier.height(40.dp)
        )
    }
}

@Composable
private fun PacketDetails(
    packet: RescuePacket
) {

    Text(
        text =
            "ID: ${packet.id}"
    )

    Text(
        text =
            "Type: ${packet.type}"
    )

    Text(
        text =
            "Latitude: ${packet.latitude}"
    )

    Text(
        text =
            "Longitude: ${packet.longitude}"
    )

    Text(
        text =
            "Timestamp: ${packet.timestamp}"
    )

    Text(
        text =
            "Hop: ${packet.hopCount}/${packet.ttl}"
    )
}

@Composable
private fun RequirementRow(
    name: String,
    satisfied: Boolean
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                )
    ) {

        Text(
            text =
                if (satisfied) {
                    "✓"
                } else {
                    "✗"
                },

            modifier =
                Modifier.padding(
                    end = 12.dp
                )
        )

        Text(
            text = name
        )
    }
}