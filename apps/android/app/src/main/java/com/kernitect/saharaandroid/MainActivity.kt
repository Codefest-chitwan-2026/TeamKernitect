package com.kernitect.saharaandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kernitect.saharaandroid.ble.AppRequirements
import com.kernitect.saharaandroid.ble.BleAdvertiser
import com.kernitect.saharaandroid.ble.BleGattClient
import com.kernitect.saharaandroid.ble.BleGattServer
import com.kernitect.saharaandroid.ble.BleScanner
import com.kernitect.saharaandroid.model.RescuePacket
import com.kernitect.saharaandroid.ui.theme.SaharaAndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SaharaAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RescueMeshTestScreen()
                }
            }
        }
    }
}

@Composable
fun RescueMeshTestScreen() {

    val context =
        LocalContext.current

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    var advertiserStatus by remember {
        mutableStateOf(
            "Not advertising"
        )
    }

    var scannerStatus by remember {
        mutableStateOf(
            "Not scanning"
        )
    }

    var gattServerStatus by remember {
        mutableStateOf(
            "GATT server stopped"
        )
    }

    var gattClientStatus by remember {
        mutableStateOf(
            "GATT client idle"
        )
    }

    var foundDevice by remember {
        mutableStateOf<BluetoothDevice?>(
            null
        )
    }

    var foundRssi by remember {
        mutableStateOf<Int?>(
            null
        )
    }

    var receivedMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var outgoingMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    val advertiser =
        remember {
            BleAdvertiser(
                context =
                    context.applicationContext,

                onStatusChanged = {
                    advertiserStatus = it
                }
            )
        }

    val scanner =
        remember {
            BleScanner(
                context =
                    context.applicationContext,

                onDeviceFound = {
                        device,
                        rssi ->

                    foundDevice = device
                    foundRssi = rssi

                    scannerStatus =
                        "RESCUEMESH device found"
                },

                onStatusChanged = {
                    scannerStatus = it
                }
            )
        }

    val gattServer =
        remember {
            BleGattServer(
                context =
                    context.applicationContext,

                onMessageReceived = {
                    receivedMessage = it
                },

                onStatusChanged = {
                    gattServerStatus = it
                }
            )
        }

    val gattClient =
        remember {
            BleGattClient(
                context =
                    context.applicationContext,

                onStatusChanged = {
                    gattClientStatus = it
                }
            )
        }

    DisposableEffect(Unit) {

        onDispose {

            advertiser.stopAdvertising()

            scanner.stopScanning()

            gattServer.stopServer()

            gattClient.close()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "SAHARA",
            style =
                MaterialTheme
                    .typography
                    .headlineLarge
        )

        Text(
            text =
                "RESCUEMESH GATT Test",

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
                Modifier.height(20.dp)
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

                    locationSettingsLauncher
                        .launch(
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
            return@Column
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text = "RECEIVER CONTROLS",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {
                gattServer.startServer()
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Start GATT Server"
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {
                advertiser.startAdvertising()
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Start Advertising"
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Server: $gattServerStatus"
        )

        Text(
            text =
                "Advertiser: $advertiserStatus"
        )

        receivedMessage?.let {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "RECEIVED JSON:"
            )

            Text(
                text = it
            )
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )

        Text(
            text =
                "SENDER CONTROLS",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {

                foundDevice = null
                foundRssi = null

                scanner.startScanning()
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Start Scanning"
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Button(
            onClick = {
                scanner.stopScanning()
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Stop Scanning"
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Scanner: $scannerStatus"
        )

        foundDevice?.let { device ->

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Device: ${device.address}"
            )

            Text(
                text =
                    "RSSI: ${foundRssi ?: "?"} dBm"
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Button(
                onClick = {

                    scanner.stopScanning()

                    val packet =
                        RescuePacket.createSos(
                            latitude = 27.6812,
                            longitude = 84.4321
                        )

                    val json =
                        packet.toJson()

                    outgoingMessage =
                        json

                    gattClient.connectAndSend(
                        device,
                        json
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Send Test SOS"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                "Client: $gattClientStatus"
        )

        outgoingMessage?.let {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "SENT JSON:"
            )

            Text(
                text = it
            )
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )
    }
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
                if (satisfied)
                    "✓"
                else
                    "✗",

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