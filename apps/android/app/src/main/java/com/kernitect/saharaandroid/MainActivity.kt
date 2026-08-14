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
import com.kernitect.saharaandroid.ble.BleScanner
import com.kernitect.saharaandroid.ui.theme.SaharaAndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SaharaAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RequirementsScreen()
                }
            }
        }
    }
}

@Composable
fun RequirementsScreen() {

    val context = LocalContext.current

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    var advertiserStatus by remember {
        mutableStateOf("Not advertising")
    }

    var scannerStatus by remember {
        mutableStateOf("Not scanning")
    }

    var foundDevice by remember {
        mutableStateOf<String?>(null)
    }

    var foundRssi by remember {
        mutableStateOf<Int?>(null)
    }

    val advertiser = remember {
        BleAdvertiser(
            context = context.applicationContext,
            onStatusChanged = { status ->
                advertiserStatus = status
            }
        )
    }

    val scanner = remember {
        BleScanner(
            context = context.applicationContext,

            onDeviceFound = { address, rssi ->
                foundDevice = address
                foundRssi = rssi

                scannerStatus =
                    "RESCUEMESH device found"
            },

            onStatusChanged = { status ->
                scannerStatus = status
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            advertiser.stopAdvertising()
            scanner.stopScanning()
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            refreshKey++
        }

    val bluetoothLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            refreshKey++
        }

    val locationSettingsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            refreshKey++
        }

    @Suppress("UNUSED_VARIABLE")
    val currentRefreshKey = refreshKey

    val supportsBle =
        AppRequirements.supportsBle(context)

    val permissionsGranted =
        AppRequirements.hasAllRuntimePermissions(context)

    val preciseLocationGranted =
        AppRequirements.hasPreciseLocationPermission(context)

    val bluetoothEnabled =
        AppRequirements.isBluetoothEnabled(context)

    val locationEnabled =
        AppRequirements.isLocationEnabled(context)

    val ready =
        supportsBle &&
                permissionsGranted &&
                preciseLocationGranted &&
                bluetoothEnabled &&
                locationEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "SAHARA",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "RESCUEMESH BLE Test",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        RequirementRow(
            name = "BLE supported",
            satisfied = supportsBle
        )

        RequirementRow(
            name = "Permissions granted",
            satisfied = permissionsGranted
        )

        RequirementRow(
            name = "Precise location",
            satisfied = preciseLocationGranted
        )

        RequirementRow(
            name = "Bluetooth enabled",
            satisfied = bluetoothEnabled
        )

        RequirementRow(
            name = "Location enabled",
            satisfied = locationEnabled
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (!permissionsGranted) {

            Button(
                onClick = {
                    permissionLauncher.launch(
                        AppRequirements.requiredRuntimePermissions()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permissions")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        if (!bluetoothEnabled) {

            Button(
                onClick = {
                    val intent =
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

                    bluetoothLauncher.launch(intent)
                },
                enabled = permissionsGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Bluetooth")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        if (!locationEnabled) {

            Button(
                onClick = {
                    val intent =
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                    locationSettingsLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Location")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        if (ready) {

            Text(
                text = "Ready for RESCUEMESH",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = {
                    advertiser.startAdvertising()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Advertising")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    advertiser.stopAdvertising()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Advertising")
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Advertiser: $advertiserStatus"
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = {
                    foundDevice = null
                    foundRssi = null

                    scanner.startScanning()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Scanning")
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    scanner.stopScanning()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Scanning")
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Scanner: $scannerStatus"
            )

            foundDevice?.let { address ->

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Found RESCUEMESH device"
                )

                Text(
                    text = "Address: $address"
                )

                Text(
                    text = "RSSI: ${foundRssi ?: "?"} dBm"
                )
            }
        }
    }
}

@Composable
private fun RequirementRow(
    name: String,
    satisfied: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {

        Text(
            text = if (satisfied) "✓" else "✗",
            modifier = Modifier.padding(end = 12.dp)
        )

        Text(
            text = name
        )
    }
}