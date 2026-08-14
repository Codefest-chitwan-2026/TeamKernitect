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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kernitect.saharaandroid.ble.AppRequirements
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

    // Forces these checks to re-run after returning from a system dialog.
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
            text = "RESCUEMESH Device Setup",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(32.dp)
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
            modifier = Modifier.height(32.dp)
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
                modifier = Modifier.height(12.dp)
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
                modifier = Modifier.height(12.dp)
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
                modifier = Modifier.height(12.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (ready) {
            Text(
                text = "Ready for RESCUEMESH",
                style = MaterialTheme.typography.headlineSmall
            )
        } else {
            Text(
                text = "Complete the requirements above.",
                style = MaterialTheme.typography.bodyLarge
            )
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
            .padding(vertical = 6.dp)
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