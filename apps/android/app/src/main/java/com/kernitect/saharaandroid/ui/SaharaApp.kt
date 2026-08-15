package com.kernitect.saharaandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.model.WitnessReport
import com.kernitect.saharaandroid.ui.components.IncomingAlertBanner
import com.kernitect.saharaandroid.ui.navigation.AppDestination
import com.kernitect.saharaandroid.ui.navigation.BottomNavigationBar
import com.kernitect.saharaandroid.ui.screens.help.HelpScreen
import com.kernitect.saharaandroid.ui.screens.home.HomeScreen
import com.kernitect.saharaandroid.ui.screens.incident.IncidentDetailsScreen
import com.kernitect.saharaandroid.ui.screens.map.MapScreen
import com.kernitect.saharaandroid.ui.screens.notifications.NotificationsScreen

@Composable
fun SaharaApp(
    onCriticalSos: () -> Unit,

    receivedAlerts: List<ReceivedAlert> =
        emptyList(),

    unreadNotificationCount: Int =
        0,

    incomingAlert: ReceivedAlert? =
        null,

    onIncomingAlertDismissed: () -> Unit =
        {},

    onNotificationsOpened: () -> Unit =
        {},

    onNonEmergencyRequest: (
        disasterType: String,
        peopleCount: String,
        explanation: String
    ) -> Unit = { _, _, _ -> }
) {

    /*
     * Current screen.
     */
    var currentDestination by remember {
        mutableStateOf(
            AppDestination.HOME
        )
    }

    /*
     * Which SOS/help request the Map screen
     * should focus on.
     */
    var focusedMapAlert by remember {
        mutableStateOf<ReceivedAlert?>(
            null
        )
    }

    /*
     * Incident selected from the
     * Notifications screen.
     */
    var selectedIncident by remember {
        mutableStateOf<ReceivedAlert?>(
            null
        )
    }

    /*
     * If Map was opened from Incident Details,
     * this tells the Map where Back should go.
     *
     * null = normal map navigation
     */
    var mapBackDestination by remember {
        mutableStateOf<AppDestination?>(
            null
        )
    }

    /*
     * TEMPORARY LOCAL WITNESS REPORTS.
     *
     * These currently only live on this phone.
     *
     * Later we will send them through
     * RESCUEMESH as real witness packets.
     */
    val witnessReports =
        remember {
            mutableStateListOf<WitnessReport>()
        }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Scaffold(
            containerColor =
                Color.White,

            bottomBar = {

                /*
                 * Hide bottom navigation on
                 * Notifications and Incident Details.
                 *
                 * If the Map was opened from
                 * Incident Details, hide it there too
                 * because we have a Back button.
                 */
                val hideBottomNavigation =
                    currentDestination ==
                            AppDestination.NOTIFICATIONS ||
                            currentDestination ==
                            AppDestination.INCIDENT_DETAILS ||
                            (
                                    currentDestination ==
                                            AppDestination.MAP &&
                                            mapBackDestination != null
                                    )

                if (!hideBottomNavigation) {

                    BottomNavigationBar(
                        currentDestination =
                            currentDestination,

                        /*
                         * HOME
                         */
                        onHomeClick = {

                            mapBackDestination =
                                null

                            currentDestination =
                                AppDestination.HOME
                        },

                        /*
                         * BIG CRITICAL SOS BUTTON
                         */
                        onSosClick =
                            onCriticalSos,

                        /*
                         * NORMAL MAP NAVIGATION
                         */
                        onMapClick = {

                            /*
                             * Focus the newest alert
                             * if one exists.
                             */
                            focusedMapAlert =
                                receivedAlerts
                                    .firstOrNull()

                            /*
                             * Since this came from the
                             * bottom navigation, Map does
                             * not need a Back button.
                             */
                            mapBackDestination =
                                null

                            currentDestination =
                                AppDestination.MAP
                        }
                    )
                }
            }
        ) { innerPadding ->

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding
                        )
            ) {

                when (
                    currentDestination
                ) {

                    /*
                     * =========================
                     * HOME
                     * =========================
                     */
                    AppDestination.HOME -> {

                        HomeScreen(
                            /*
                             * MAP button on the
                             * Emergency Alert card.
                             */
                            onMapClick = {

                                focusedMapAlert =
                                    receivedAlerts
                                        .firstOrNull()

                                mapBackDestination =
                                    null

                                currentDestination =
                                    AppDestination.MAP
                            },

                            unreadNotificationCount =
                                unreadNotificationCount,

                            /*
                             * Bell icon.
                             */
                            onNotificationClick = {

                                onNotificationsOpened()

                                currentDestination =
                                    AppDestination.NOTIFICATIONS
                            },

                            /*
                             * Middle help form.
                             */
                            onSendHelpRequest =
                                onNonEmergencyRequest
                        )
                    }

                    /*
                     * =========================
                     * HELP
                     * =========================
                     */
                    AppDestination.HELP -> {

                        HelpScreen()
                    }

                    /*
                     * =========================
                     * MAP
                     * =========================
                     */
                    AppDestination.MAP -> {

                        MapScreen(
                            /*
                             * Show all received
                             * incidents as markers.
                             */
                            alerts =
                                receivedAlerts,

                            /*
                             * Zoom into this specific one.
                             */
                            focusedAlert =
                                focusedMapAlert,

                            unreadNotificationCount =
                                unreadNotificationCount,

                            /*
                             * Bell icon from Map.
                             */
                            onNotificationClick = {

                                onNotificationsOpened()

                                mapBackDestination =
                                    null

                                currentDestination =
                                    AppDestination.NOTIFICATIONS
                            },

                            /*
                             * Only exists when the Map
                             * was opened from something
                             * such as Incident Details.
                             */
                            onBackClick =
                                mapBackDestination
                                    ?.let {
                                            destination ->

                                        {
                                            currentDestination =
                                                destination

                                            mapBackDestination =
                                                null
                                        }
                                    }
                        )
                    }

                    /*
                     * =========================
                     * NOTIFICATIONS
                     * =========================
                     */
                    AppDestination.NOTIFICATIONS -> {

                        NotificationsScreen(
                            alerts =
                                receivedAlerts,

                            onBackClick = {

                                currentDestination =
                                    AppDestination.HOME
                            },

                            /*
                             * View Details →
                             */
                            onViewDetails = {
                                    alert ->

                                selectedIncident =
                                    alert

                                currentDestination =
                                    AppDestination.INCIDENT_DETAILS
                            }
                        )
                    }

                    /*
                     * =========================
                     * INCIDENT DETAILS
                     * =========================
                     */
                    AppDestination.INCIDENT_DETAILS -> {

                        val incident =
                            selectedIncident

                        if (incident != null) {

                            IncidentDetailsScreen(
                                alert =
                                    incident,

                                /*
                                 * Only show witness reports
                                 * belonging to this SOS.
                                 */
                                witnessReports =
                                    witnessReports
                                        .filter {
                                                report ->

                                            report.incidentId ==
                                                    incident.packet.id
                                        },

                                /*
                                 * Back to Notifications.
                                 */
                                onBackClick = {

                                    currentDestination =
                                        AppDestination.NOTIFICATIONS
                                },

                                /*
                                 * Open this exact incident
                                 * on the full Map screen.
                                 */
                                onOpenFullMap = {

                                    focusedMapAlert =
                                        incident

                                    /*
                                     * This makes Map show
                                     * a Back arrow that returns
                                     * here.
                                     */
                                    mapBackDestination =
                                        AppDestination.INCIDENT_DETAILS

                                    currentDestination =
                                        AppDestination.MAP
                                },

                                /*
                                 * Add witness details.
                                 *
                                 * LOCAL ONLY FOR NOW.
                                 */
                                onAddDetails = {
                                        disasterType,
                                        peopleCount,
                                        explanation ->

                                    witnessReports.add(
                                        index = 0,

                                        element =
                                            WitnessReport(
                                                incidentId =
                                                    incident.packet.id,

                                                disasterType =
                                                    disasterType,

                                                peopleCount =
                                                    peopleCount,

                                                message =
                                                    explanation
                                            )
                                    )
                                }
                            )

                        } else {

                            /*
                             * Safety fallback.
                             *
                             * Normally this should
                             * never happen.
                             */
                            currentDestination =
                                AppDestination.NOTIFICATIONS
                        }
                    }
                }
            }
        }

        /*
         * =====================================
         * IN-APP SOS HEADS-UP ALERT
         * =====================================
         *
         * This stays above whatever screen
         * is currently open.
         */
        incomingAlert?.let {
                alert ->

            IncomingAlertBanner(
                alert =
                    alert,

                /*
                 * Clicking the popup opens
                 * Notifications.
                 */
                onClick = {

                    onIncomingAlertDismissed()

                    onNotificationsOpened()

                    mapBackDestination =
                        null

                    currentDestination =
                        AppDestination.NOTIFICATIONS
                },

                /*
                 * Countdown completed.
                 */
                onDismiss = {

                    onIncomingAlertDismissed()
                },

                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter
                        )
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 10.dp
                        )
                        .zIndex(
                            10f
                        )
            )
        }
    }
}