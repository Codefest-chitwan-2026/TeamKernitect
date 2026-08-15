package com.kernitect.saharaandroid

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kernitect.saharaandroid.ble.AppRequirements
import com.kernitect.saharaandroid.data.local.SaharaDatabase
import com.kernitect.saharaandroid.disaster.DisasterAlertMatcher
import com.kernitect.saharaandroid.location.LocationProvider
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.model.RescuePacket
import com.kernitect.saharaandroid.service.MeshServiceState
import com.kernitect.saharaandroid.service.RescueMeshService
import com.kernitect.saharaandroid.ui.SaharaApp
import com.kernitect.saharaandroid.ui.components.SendProgressDialog
import com.kernitect.saharaandroid.ui.components.SendProgressState
import com.kernitect.saharaandroid.ui.theme.SaharaAndroidTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * =============================
         * OSM CONFIGURATION
         * =============================
         */
        Configuration
            .getInstance()
            .load(
                applicationContext,

                PreferenceManager
                    .getDefaultSharedPreferences(
                        applicationContext
                    )
            )


        Configuration
            .getInstance()
            .userAgentValue =
            packageName


        setContent {

            SaharaAndroidTheme {


                /*
                 * =============================
                 * ROOM
                 * =============================
                 */

                val database =
                    remember {

                        SaharaDatabase
                            .getInstance(
                                applicationContext
                            )
                    }


                val incidentDao =
                    remember(
                        database
                    ) {

                        database.incidentDao()
                    }


                val publicAlertDao =
                    remember(
                        database
                    ) {

                        database.publicAlertDao()
                    }


                val trackingEventDao =
                    remember(database) {
                        database.trackingEventDao()
                    }


                val coroutineScope =
                    rememberCoroutineScope()


                /*
                 * =============================
                 * STORED INCIDENTS
                 * =============================
                 */

                val storedIncidents by
                incidentDao
                    .observeAllIncidents()
                    .collectAsState(
                        initial =
                            emptyList()
                    )


                val unreadNotificationCount by
                incidentDao
                    .observeUnreadCount()
                    .collectAsState(
                        initial =
                            0
                    )


                val localIncidents by
                incidentDao
                    .observeLocalIncidents()
                    .collectAsState(initial = emptyList())


                val trackingEvents by
                trackingEventDao
                    .observeAllEvents()
                    .collectAsState(initial = emptyList())


                val responderDistances by
                MeshServiceState
                    .responderDistances
                    .collectAsState()


                val receivedAlerts =

                    remember(
                        storedIncidents
                    ) {

                        storedIncidents
                            .mapNotNull {
                                    incident ->


                                val packet =
                                    RescuePacket
                                        .fromJson(
                                            incident.packetJson
                                        )


                                if (
                                    packet != null
                                ) {

                                    ReceivedAlert(

                                        packet =
                                            packet,

                                        receivedAt =
                                            incident.receivedAt
                                    )

                                } else {

                                    null
                                }
                            }
                    }


                /*
                 * =============================
                 * PUBLIC ALERTS
                 * =============================
                 */

                val storedPublicAlerts by
                publicAlertDao
                    .observeAllAlerts()
                    .collectAsState(
                        initial =
                            emptyList()
                    )


                var publicAlertLatitude by remember {

                    mutableStateOf<Double?>(
                        null
                    )
                }


                var publicAlertLongitude by remember {

                    mutableStateOf<Double?>(
                        null
                    )
                }


                /*
                 * =============================
                 * UI STATE
                 * =============================
                 */

                var status by remember {

                    mutableStateOf(
                        "Starting..."
                    )
                }


                var gettingLocation by remember {

                    mutableStateOf(
                        false
                    )
                }


                var activeIncomingAlert by remember {

                    mutableStateOf<ReceivedAlert?>(
                        null
                    )
                }


                var sendDialogVisible by remember {

                    mutableStateOf(
                        false
                    )
                }


                var sendProgressState by remember {

                    mutableStateOf(
                        SendProgressState.LOCATING
                    )
                }


                var sendProgressMessage by remember {

                    mutableStateOf(
                        ""
                    )
                }


                var activeRequestIsCritical by remember {

                    mutableStateOf(
                        true
                    )
                }


                var requestGeneration by remember {

                    mutableStateOf(
                        0
                    )
                }


                var permissionsGranted by remember {

                    mutableStateOf(

                        AppRequirements
                            .hasAllRuntimePermissions(
                                this@MainActivity
                            )
                    )
                }


                /*
                 * =============================
                 * LOCATION
                 * =============================
                 */

                val locationProvider =
                    remember {

                        LocationProvider(
                            context =
                                this@MainActivity
                        )
                    }


                /*
                 * =============================
                 * BACKGROUND SERVICE STATUS
                 * =============================
                 */

                val meshServiceStatus by
                MeshServiceState
                    .status
                    .collectAsState()


                /*
                 * Convert service status into
                 * existing send-progress UI.
                 */
                LaunchedEffect(
                    meshServiceStatus
                ) {

                    status =
                        meshServiceStatus


                    if (
                        sendDialogVisible
                    ) {

                        when {

                            meshServiceStatus.contains(
                                "Looking for nearby Android relay",
                                ignoreCase = true
                            ) -> {

                                sendProgressState =
                                    SendProgressState.SEARCHING

                                sendProgressMessage =
                                    "Finding a nearby rescue mesh device..."
                            }


                            meshServiceStatus.contains(
                                "Looking for RESCUEMESH gateway",
                                ignoreCase = true
                            ) -> {

                                sendProgressState =
                                    SendProgressState.SEARCHING

                                sendProgressMessage =
                                    "Looking for a gateway..."
                            }


                            meshServiceStatus.contains(
                                "found. Sending hop",
                                ignoreCase = true
                            ) -> {

                                sendProgressState =
                                    SendProgressState.SENDING


                                sendProgressMessage =

                                    if (
                                        activeRequestIsCritical
                                    ) {

                                        "Sending critical SOS..."

                                    } else {

                                        "Sending help request..."
                                    }
                            }


                            meshServiceStatus.contains(
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
                }


                /*
                 * =================================
                 * IN-APP HEADS-UP ALERT
                 * =================================
                 *
                 * System notifications are handled
                 * directly by RescueMeshService.
                 */
                LaunchedEffect(
                    Unit
                ) {

                    MeshServiceState
                        .incomingAlerts
                        .collect {
                                alert ->

                            activeIncomingAlert =
                                alert
                        }
                }


                /*
                 * =================================
                 * SEND SUCCESS
                 * =================================
                 */
                LaunchedEffect(
                    Unit
                ) {

                    MeshServiceState
                        .sentPackets
                        .collect {
                                packet ->


                            status =
                                "${packet.priority} request sent - " +
                                        "Hop ${packet.hopCount}/${packet.ttl}"


                            if (
                                sendDialogVisible
                            ) {

                                sendProgressState =
                                    SendProgressState.SUCCESS


                                sendProgressMessage =

                                    if (
                                        packet.priority ==
                                        RescuePacket.PRIORITY_CRITICAL
                                    ) {

                                        "SOS sent successfully."

                                    } else {

                                        "Help request sent successfully."
                                    }
                            }
                        }
                }


                /*
                 * =============================
                 * PERMISSIONS
                 * =============================
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


                LaunchedEffect(
                    Unit
                ) {

                    if (
                        !permissionsGranted ||
                        !AppRequirements
                            .hasNotificationPermission(
                                this@MainActivity
                            )
                    ) {

                        permissionLauncher.launch(

                            AppRequirements
                                .requiredRuntimePermissions()
                        )
                    }
                }


                /*
                 * =============================
                 * START BACKGROUND RESCUEMESH
                 * =============================
                 *
                 * IMPORTANT:
                 *
                 * We DO NOT stop this when the
                 * Activity leaves the screen.
                 */
                LaunchedEffect(
                    permissionsGranted
                ) {

                    if (
                        permissionsGranted
                    ) {

                        RescueMeshService.start(
                            this@MainActivity
                        )

                    } else {

                        status =
                            "Waiting for permissions..."
                    }
                }


                /*
                 * =============================
                 * LOCAL PUBLIC ALERT GPS
                 * =============================
                 */

                LaunchedEffect(
                    permissionsGranted,
                    storedPublicAlerts.isNotEmpty()
                ) {

                    if (
                        permissionsGranted &&
                        storedPublicAlerts.isNotEmpty() &&
                        AppRequirements
                            .hasPreciseLocation(
                                this@MainActivity
                            ) &&
                        AppRequirements
                            .isLocationServicesEnabled(
                                this@MainActivity
                            )
                    ) {

                        locationProvider
                            .getCurrentLocation(

                                onSuccess = {
                                        location ->


                                    publicAlertLatitude =
                                        location.latitude


                                    publicAlertLongitude =
                                        location.longitude
                                },


                                onError = {

                                    /*
                                     * Home simply shows
                                     * no matching alert.
                                     */
                                }
                            )
                    }
                }


                val alertLatitude =
                    publicAlertLatitude


                val alertLongitude =
                    publicAlertLongitude


                val matchedPublicAlert =

                    remember(
                        storedPublicAlerts,
                        alertLatitude,
                        alertLongitude
                    ) {

                        if (
                            alertLatitude != null &&
                            alertLongitude != null
                        ) {

                            DisasterAlertMatcher
                                .findPrimaryAlert(

                                    latitude =
                                        alertLatitude,

                                    longitude =
                                        alertLongitude,

                                    alerts =
                                        storedPublicAlerts
                                )
                                ?.alert

                        } else {

                            null
                        }
                    }


                /*
                 * Brief success dialog.
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


                /*
                 * =============================
                 * UI
                 * =============================
                 */

                SaharaApp(

                    publicAlert =
                        matchedPublicAlert,


                    receivedAlerts =
                        receivedAlerts,


                    unreadNotificationCount =
                        unreadNotificationCount,


                    localIncidents =
                        localIncidents,


                    trackingEvents =
                        trackingEvents,


                    responderDistances =
                        responderDistances,


                    incomingAlert =
                        activeIncomingAlert,


                    onIncomingAlertDismissed = {

                        activeIncomingAlert =
                            null
                    },


                    onNotificationsOpened = {

                        coroutineScope.launch {

                            incidentDao
                                .markAllAsRead()
                        }
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

                            if (
                                !permissionsGranted
                            ) {

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

                                requestGeneration +=
                                    1


                                val thisRequest =
                                    requestGeneration


                                val requestCreatedAt =
                                    System.currentTimeMillis()


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


                                                publicAlertLatitude =
                                                    location.latitude


                                                publicAlertLongitude =
                                                    location.longitude


                                                val matchedAlert =

                                                    DisasterAlertMatcher
                                                        .findPrimaryAlert(

                                                            latitude =
                                                                location.latitude,

                                                            longitude =
                                                                location.longitude,

                                                            alerts =
                                                                storedPublicAlerts
                                                        )
                                                        ?.alert


                                                /*
                                                 * Send command to SERVICE.
                                                 *
                                                 * MainActivity no longer owns
                                                 * MeshEngine.
                                                 */
                                                RescueMeshService
                                                    .sendCriticalSos(

                                                        context =
                                                            this@MainActivity,

                                                        latitude =
                                                            location.latitude,

                                                        longitude =
                                                            location.longitude,

                                                        likelyDisaster =
                                                            matchedAlert
                                                                ?.disasterType
                                                                ?: RescuePacket.DISASTER_UNKNOWN,

                                                        areaSeverity =
                                                            matchedAlert
                                                                ?.severity
                                                                ?: RescuePacket.SEVERITY_UNKNOWN,

                                                        requestCreatedAt =
                                                            requestCreatedAt,

                                                        locationAttachedAt =
                                                            System.currentTimeMillis()
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

                            if (
                                !permissionsGranted
                            ) {

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

                                requestGeneration +=
                                    1


                                val thisRequest =
                                    requestGeneration


                                val requestCreatedAt =
                                    System.currentTimeMillis()


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


                                                RescueMeshService
                                                    .sendHelpRequest(

                                                        context =
                                                            this@MainActivity,

                                                        latitude =
                                                            location.latitude,

                                                        longitude =
                                                            location.longitude,

                                                        disasterType =
                                                            disasterType,

                                                        peopleCount =
                                                            peopleCount,

                                                        explanation =
                                                            explanation,

                                                        requestCreatedAt =
                                                            requestCreatedAt,

                                                        locationAttachedAt =
                                                            System.currentTimeMillis()
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
                 * =============================
                 * SEND DIALOG
                 * =============================
                 */

                if (
                    sendDialogVisible
                ) {

                    SendProgressDialog(

                        title =

                            if (
                                activeRequestIsCritical
                            ) {

                                "Emergency SOS"

                            } else {

                                "Help Request"
                            },


                        message =
                            sendProgressMessage,


                        state =
                            sendProgressState,


                        onCancel = {

                            requestGeneration +=
                                1


                            gettingLocation =
                                false


                            RescueMeshService
                                .cancelPending(
                                    this@MainActivity
                                )


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
