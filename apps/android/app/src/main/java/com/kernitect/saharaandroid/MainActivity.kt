package com.kernitect.saharaandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kernitect.saharaandroid.ble.AppRequirements
import com.kernitect.saharaandroid.location.LocationProvider
import com.kernitect.saharaandroid.mesh.MeshEngine
import com.kernitect.saharaandroid.ui.SaharaApp
import com.kernitect.saharaandroid.ui.components.SendProgressDialog
import com.kernitect.saharaandroid.ui.components.SendProgressState
import com.kernitect.saharaandroid.ui.theme.SaharaAndroidTheme
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateListOf
import com.kernitect.saharaandroid.model.ReceivedAlert

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            SaharaAndroidTheme {

                var status by remember {
                    mutableStateOf("Starting...")
                }

                var gettingLocation by remember {
                    mutableStateOf(false)
                }

                /*
                 * All SOS/help packets this phone
                 * has received from another mesh node.
                 */
                val receivedAlerts =
                    remember {
                        mutableStateListOf<ReceivedAlert>()
                    }

                /*
                 * IDs that haven't been viewed
                 * through the bell screen yet.
                 */
                val unreadAlertIds =
                    remember {
                        mutableStateListOf<String>()
                    }

                var activeIncomingAlert by remember {
                    mutableStateOf<ReceivedAlert?>(
                        null
                    )
                }

                /*
                 * Progress popup state.
                 */
                var sendDialogVisible by remember {
                    mutableStateOf(false)
                }

                var sendProgressState by remember {
                    mutableStateOf(
                        SendProgressState.LOCATING
                    )
                }

                var sendProgressMessage by remember {
                    mutableStateOf("")
                }

                var activeRequestIsCritical by remember {
                    mutableStateOf(true)
                }

                /*
                 * Every new request gets a generation number.
                 *
                 * Cancelling increments this number so an old
                 * GPS callback cannot accidentally send later.
                 */
                var requestGeneration by remember {
                    mutableStateOf(0)
                }

                var permissionsGranted by remember {
                    mutableStateOf(
                        AppRequirements
                            .hasAllRuntimePermissions(
                                this@MainActivity
                            )
                    )
                }

                val locationProvider =
                    remember {

                        LocationProvider(
                            context =
                                this@MainActivity
                        )
                    }

                val meshEngine =
                    remember {

                        MeshEngine(
                            context =
                                this@MainActivity,

                            onStatusChanged = {
                                    newStatus ->

                                status =
                                    newStatus

                                /*
                                 * Convert technical mesh status
                                 * into simple user-facing progress.
                                 */
                                if (sendDialogVisible) {

                                    when {

                                        newStatus.contains(
                                            "Looking for nearby Android relay",
                                            ignoreCase = true
                                        ) -> {

                                            sendProgressState =
                                                SendProgressState.SEARCHING

                                            sendProgressMessage =
                                                "Finding a nearby rescue mesh device..."
                                        }

                                        newStatus.contains(
                                            "Looking for RESCUEMESH gateway",
                                            ignoreCase = true
                                        ) -> {

                                            sendProgressState =
                                                SendProgressState.SEARCHING

                                            sendProgressMessage =
                                                "Looking for a gateway..."
                                        }

                                        newStatus.contains(
                                            "found. Sending hop",
                                            ignoreCase = true
                                        ) -> {

                                            /*
                                             * From here cancellation is hidden.
                                             */
                                            sendProgressState =
                                                SendProgressState.SENDING

                                            sendProgressMessage =
                                                if (activeRequestIsCritical) {
                                                    "Sending critical SOS..."
                                                } else {
                                                    "Sending help request..."
                                                }
                                        }

                                        newStatus.contains(
                                            "No next relay found",
                                            ignoreCase = true
                                        ) -> {

                                            sendProgressState =
                                                SendProgressState.ERROR

                                            sendProgressMessage =
                                                "No nearby relay or gateway was found."
                                        }
                                    }
                                }
                            },

                            onPacketReceived = {
                                    packet ->

                                status =
                                    "${packet.priority} request received - " +
                                            "Hop ${packet.hopCount}/${packet.ttl}"

                                val alert =
                                    ReceivedAlert(
                                        packet = packet
                                    )

                                /*
                                 * Permanent in-app notification list.
                                 */
                                receivedAlerts.add(
                                    index = 0,
                                    element = alert
                                )

                                /*
                                 * Unread bell counter.
                                 */
                                if (
                                    !unreadAlertIds.contains(
                                        packet.id
                                    )
                                ) {
                                    unreadAlertIds.add(
                                        packet.id
                                    )
                                }

                                /*
                                 * Temporary heads-up banner.
                                 */
                                activeIncomingAlert =
                                    alert
                            },

                            onPacketSent = {
                                    packet ->

                                status =
                                    "${packet.priority} request sent - " +
                                            "Hop ${packet.hopCount}/${packet.ttl}"

                                if (sendDialogVisible) {

                                    sendProgressState =
                                        SendProgressState.SUCCESS

                                    sendProgressMessage =
                                        if (packet.priority ==
                                            "CRITICAL"
                                        ) {
                                            "SOS sent successfully."
                                        } else {
                                            "Help request sent successfully."
                                        }
                                }
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
                                    this@MainActivity
                                )
                    }

                /*
                 * Permissions.
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
                 * Start mesh.
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

                /*
                 * Briefly show success, then close automatically.
                 */
                LaunchedEffect(
                    sendProgressState,
                    sendDialogVisible
                ) {

                    if (
                        sendDialogVisible &&
                        sendProgressState ==
                        SendProgressState.SUCCESS
                    ) {

                        delay(
                            1500
                        )

                        sendDialogVisible =
                            false
                    }
                }

                DisposableEffect(Unit) {

                    onDispose {

                        meshEngine.stop()
                    }
                }

                SaharaApp(

                    receivedAlerts =
                        receivedAlerts,

                    unreadNotificationCount =
                        unreadAlertIds.size,

                    incomingAlert =
                        activeIncomingAlert,

                    onIncomingAlertDismissed = {
                        activeIncomingAlert =
                            null
                    },

                    onNotificationsOpened = {

                        unreadAlertIds.clear()
                    },

                    /*
                     * =============================
                     * CRITICAL SOS
                     * =============================
                     */
                    onCriticalSos = {

                        if (
                            !sendDialogVisible &&
                            !gettingLocation
                        ) {

                            if (!permissionsGranted) {

                                status =
                                    "Required permissions not granted"

                            } else if (
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

                                /*
                                 * Start a new request generation.
                                 */
                                requestGeneration += 1

                                val thisRequest =
                                    requestGeneration

                                activeRequestIsCritical =
                                    true

                                gettingLocation =
                                    true

                                sendDialogVisible =
                                    true

                                sendProgressState =
                                    SendProgressState.LOCATING

                                sendProgressMessage =
                                    "Getting your location..."

                                locationProvider
                                    .getCurrentLocation(

                                        onSuccess = {
                                                location ->

                                            /*
                                             * Ignore this result if
                                             * the user cancelled.
                                             */
                                            if (
                                                thisRequest ==
                                                requestGeneration
                                            ) {

                                                gettingLocation =
                                                    false

                                                sendProgressState =
                                                    SendProgressState.SEARCHING

                                                sendProgressMessage =
                                                    "Finding a nearby rescue mesh device..."

                                                meshEngine
                                                    .originateSos(
                                                        latitude =
                                                            location.latitude,

                                                        longitude =
                                                            location.longitude
                                                    )
                                            }
                                        },

                                        onError = {
                                                error ->

                                            if (
                                                thisRequest ==
                                                requestGeneration
                                            ) {

                                                gettingLocation =
                                                    false

                                                sendProgressState =
                                                    SendProgressState.ERROR

                                                sendProgressMessage =
                                                    error
                                            }
                                        }
                                    )
                            }
                        }
                    },

                    /*
                     * =============================
                     * NORMAL HELP REQUEST
                     * =============================
                     */
                    onNonEmergencyRequest = {
                            disasterType,
                            peopleCount,
                            explanation ->

                        if (
                            !sendDialogVisible &&
                            !gettingLocation
                        ) {

                            if (!permissionsGranted) {

                                status =
                                    "Required permissions not granted"

                            } else if (
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

                                requestGeneration += 1

                                val thisRequest =
                                    requestGeneration

                                activeRequestIsCritical =
                                    false

                                gettingLocation =
                                    true

                                sendDialogVisible =
                                    true

                                sendProgressState =
                                    SendProgressState.LOCATING

                                sendProgressMessage =
                                    "Getting your location..."

                                locationProvider
                                    .getCurrentLocation(

                                        onSuccess = {
                                                location ->

                                            if (
                                                thisRequest ==
                                                requestGeneration
                                            ) {

                                                gettingLocation =
                                                    false

                                                sendProgressState =
                                                    SendProgressState.SEARCHING

                                                sendProgressMessage =
                                                    "Finding a nearby rescue mesh device..."

                                                meshEngine
                                                    .originateHelpRequest(
                                                        latitude =
                                                            location.latitude,

                                                        longitude =
                                                            location.longitude,

                                                        disasterType =
                                                            disasterType,

                                                        peopleCount =
                                                            peopleCount,

                                                        explanation =
                                                            explanation
                                                    )
                                            }
                                        },

                                        onError = {
                                                error ->

                                            if (
                                                thisRequest ==
                                                requestGeneration
                                            ) {

                                                gettingLocation =
                                                    false

                                                sendProgressState =
                                                    SendProgressState.ERROR

                                                sendProgressMessage =
                                                    error
                                            }
                                        }
                                    )
                            }
                        }
                    }
                )

                /*
                 * Progress popup sits above the entire app.
                 */
                if (sendDialogVisible) {

                    SendProgressDialog(
                        title =
                            if (activeRequestIsCritical) {
                                "Emergency SOS"
                            } else {
                                "Help Request"
                            },

                        message =
                            sendProgressMessage,

                        state =
                            sendProgressState,

                        onCancel = {

                            /*
                             * Invalidate any unfinished
                             * GPS callback.
                             */
                            requestGeneration += 1

                            gettingLocation =
                                false

                            /*
                             * Stop scanning / drop the
                             * pending mesh packet.
                             */
                            meshEngine
                                .cancelPendingForward()

                            status =
                                "Request cancelled"

                            sendDialogVisible =
                                false
                        },

                        onClose = {

                            sendDialogVisible =
                                false
                        }
                    )
                }
            }
        }
    }
}