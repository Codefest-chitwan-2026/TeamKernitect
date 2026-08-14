package com.kernitect.saharaandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.kernitect.saharaandroid.ble.AppRequirements
import com.kernitect.saharaandroid.location.LocationProvider
import com.kernitect.saharaandroid.mesh.MeshEngine
import com.kernitect.saharaandroid.ui.SaharaApp
import com.kernitect.saharaandroid.ui.theme.SaharaAndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            SaharaAndroidTheme {

                /*
                 * We keep these states here so later
                 * the real UI can display things like:
                 *
                 * - Getting location
                 * - SOS sent
                 * - SOS received
                 * - Mesh connected
                 * - Error
                 */
                var status by remember {
                    mutableStateOf("Starting...")
                }

                var gettingLocation by remember {
                    mutableStateOf(false)
                }

                var permissionsGranted by remember {
                    mutableStateOf(
                        AppRequirements.hasAllRuntimePermissions(
                            this@MainActivity
                        )
                    )
                }

                /*
                 * Existing GPS provider.
                 */
                val locationProvider = remember {

                    LocationProvider(
                        context = this@MainActivity
                    )
                }

                /*
                 * Existing working RESCUEMESH engine.
                 *
                 * BLE / GATT / relay logic is unchanged.
                 */
                val meshEngine = remember {

                    MeshEngine(
                        context = this@MainActivity,

                        onStatusChanged = { newStatus ->

                            status = newStatus
                        },

                        onPacketReceived = { packet ->

                            status =
                                "SOS received - Hop ${packet.hopCount}/${packet.ttl}"
                        },

                        onPacketSent = { packet ->

                            status =
                                "SOS sent - Hop ${packet.hopCount}/${packet.ttl}"
                        }
                    )
                }

                /*
                 * Permission launcher.
                 */
                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        contract =
                            ActivityResultContracts
                                .RequestMultiplePermissions()
                    ) {

                        permissionsGranted =
                            AppRequirements
                                .hasAllRuntimePermissions(
                                    this@MainActivity
                                )
                    }

                /*
                 * Ask for the required permissions
                 * when the app starts.
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
                 * Start the BLE mesh after all
                 * required permissions are available.
                 */
                LaunchedEffect(
                    permissionsGranted
                ) {

                    if (permissionsGranted) {

                        if (
                            !AppRequirements.hasPreciseLocation(
                                this@MainActivity
                            )
                        ) {

                            status =
                                "Precise location permission required"

                        } else if (
                            !AppRequirements.isLocationServicesEnabled(
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

                /*
                 * Shut down BLE resources when
                 * this Compose hierarchy is destroyed.
                 */
                DisposableEffect(Unit) {

                    onDispose {

                        meshEngine.stop()
                    }
                }

                /*
                 * NEW SAHARA UI
                 */
                SaharaApp(

                    /*
                     * BIG RED SOS BUTTON
                     *
                     * This is the CRITICAL emergency action.
                     */
                    onCriticalSos = {

                        /*
                         * Prevent multiple simultaneous
                         * GPS requests if SOS is tapped
                         * repeatedly.
                         */
                        if (!gettingLocation) {

                            if (
                                !permissionsGranted
                            ) {

                                status =
                                    "Required permissions not granted"

                            } else if (
                                !AppRequirements.hasPreciseLocation(
                                    this@MainActivity
                                )
                            ) {

                                status =
                                    "Precise location permission required"

                            } else if (
                                !AppRequirements.isLocationServicesEnabled(
                                    this@MainActivity
                                )
                            ) {

                                status =
                                    "Turn on Location services"

                            } else {

                                gettingLocation = true

                                status =
                                    "Getting accurate GPS location..."

                                /*
                                 * Same GPS logic that already worked
                                 * in the debug UI.
                                 */
                                locationProvider.getCurrentLocation(

                                    onSuccess = { location ->

                                        gettingLocation = false

                                        status =
                                            "GPS acquired: " +
                                                    "${location.accuracy.toInt()} m"

                                        /*
                                         * Same working SOS creation.
                                         *
                                         * GPS
                                         * ↓
                                         * MeshEngine
                                         * ↓
                                         * Android relay
                                         * ↓
                                         * Windows
                                         * ↓
                                         * FastAPI
                                         */
                                        meshEngine.originateSos(
                                            latitude =
                                                location.latitude,

                                            longitude =
                                                location.longitude
                                        )
                                    },

                                    onError = { error ->

                                        gettingLocation = false

                                        status = error
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}