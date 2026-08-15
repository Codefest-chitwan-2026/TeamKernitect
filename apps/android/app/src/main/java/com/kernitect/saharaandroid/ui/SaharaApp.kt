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
import com.kernitect.saharaandroid.data.local.entity.PublicAlertEntity
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

    /*
     * Active public emergency alert
     * matched against this phone's location.
     */
    publicAlert: PublicAlertEntity? =
        null,

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
     * ==========================================
     * CURRENT SCREEN
     * ==========================================
     */
    var currentDestination by remember {

        mutableStateOf(
            AppDestination.HOME
        )
    }


    /*
     * ==========================================
     * FOCUSED RESCUEMESH INCIDENT
     * ==========================================
     *
     * Specific SOS/help request the Map screen
     * should focus on.
     */
    var focusedMapAlert by remember {

        mutableStateOf<ReceivedAlert?>(
            null
        )
    }


    /*
     * ==========================================
     * FOCUSED PUBLIC EMERGENCY ALERT
     * ==========================================
     *
     * Selected when the citizen presses MAP
     * on the Home Emergency Alert card.
     */
    var focusedPublicAlert by remember {

        mutableStateOf<PublicAlertEntity?>(
            null
        )
    }


    /*
     * ==========================================
     * SELECTED INCIDENT
     * ==========================================
     *
     * Incident selected from Notifications.
     */
    var selectedIncident by remember {

        mutableStateOf<ReceivedAlert?>(
            null
        )
    }


    /*
     * ==========================================
     * MAP BACK DESTINATION
     * ==========================================
     *
     * If Map was opened from Incident Details,
     * this tells the Map where Back should go.
     *
     * null = normal Map navigation.
     */
    var mapBackDestination by remember {

        mutableStateOf<AppDestination?>(
            null
        )
    }


    /*
     * ==========================================
     * TEMPORARY WITNESS REPORTS
     * ==========================================
     *
     * LOCAL ONLY FOR NOW.
     *
     * Later these can become real
     * RESCUEMESH packets.
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


            /*
             * ==========================================
             * BOTTOM NAVIGATION
             * ==========================================
             */
            bottomBar = {

                /*
                 * Hide bottom navigation on:
                 *
                 * - Notifications
                 * - Incident Details
                 * - Map opened from Incident Details
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


                if (
                    !hideBottomNavigation
                ) {

                    BottomNavigationBar(

                        currentDestination =
                            currentDestination,


                        /*
                         * =================================
                         * HOME
                         * =================================
                         */
                        onHomeClick = {

                            mapBackDestination =
                                null

                            currentDestination =
                                AppDestination.HOME
                        },


                        /*
                         * =================================
                         * BIG CRITICAL SOS BUTTON
                         * =================================
                         */
                        onSosClick =
                            onCriticalSos,


                        /*
                         * =================================
                         * NORMAL MAP NAVIGATION
                         * =================================
                         *
                         * This is different from the MAP
                         * button on the public Emergency
                         * Alert card.
                         */
                        onMapClick = {

                            /*
                             * Normal Map tab should not
                             * force-focus a public warning.
                             */
                            focusedPublicAlert =
                                null


                            /*
                             * Focus newest received
                             * SOS/help request if one exists.
                             */
                            focusedMapAlert =
                                receivedAlerts
                                    .firstOrNull()


                            /*
                             * Since this came from bottom
                             * navigation, Map does not need
                             * a Back button.
                             */
                            mapBackDestination =
                                null


                            currentDestination =
                                AppDestination.MAP
                        }
                    )
                }
            }

        ) {
                innerPadding ->


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
                     * =====================================
                     * HOME
                     * =====================================
                     */
                    AppDestination.HOME -> {

                        HomeScreen(

                            /*
                             * Active public disaster alert.
                             */
                            publicAlert =
                                publicAlert,


                            /*
                             * MAP button on the red
                             * Emergency Alert card.
                             */
                            onMapClick = {

                                /*
                                 * Focus the PUBLIC alert,
                                 * not an unrelated SOS.
                                 */
                                focusedPublicAlert =
                                    publicAlert


                                /*
                                 * Clear any previous
                                 * victim incident focus.
                                 */
                                focusedMapAlert =
                                    null


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
                             * Normal Help Request form.
                             */
                            onSendHelpRequest =
                                onNonEmergencyRequest
                        )
                    }


                    /*
                     * =====================================
                     * HELP
                     * =====================================
                     */
                    AppDestination.HELP -> {

                        HelpScreen()
                    }


                    /*
                     * =====================================
                     * MAP
                     * =====================================
                     */
                    AppDestination.MAP -> {

                        MapScreen(

                            /*
                             * All received SOS/help
                             * incidents shown as markers.
                             */
                            alerts =
                                receivedAlerts,


                            /*
                             * Specific victim incident
                             * the camera should focus on.
                             */
                            focusedAlert =
                                focusedMapAlert,


                            /*
                             * Current public emergency
                             * warning shown on the map.
                             */
                            publicAlert =
                                publicAlert,


                            /*
                             * Public alert specifically
                             * selected from Home.
                             */
                            focusedPublicAlert =
                                focusedPublicAlert,


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
                             * Only exists when Map was
                             * opened from Incident Details.
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
                     * =====================================
                     * NOTIFICATIONS
                     * =====================================
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
                     * =====================================
                     * INCIDENT DETAILS
                     * =====================================
                     */
                    AppDestination.INCIDENT_DETAILS -> {

                        val incident =
                            selectedIncident


                        if (
                            incident != null
                        ) {

                            IncidentDetailsScreen(

                                alert =
                                    incident,


                                /*
                                 * Only show witness reports
                                 * belonging to this incident.
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

                                    /*
                                     * We are opening a victim
                                     * incident, not a public
                                     * emergency warning.
                                     */
                                    focusedPublicAlert =
                                        null


                                    focusedMapAlert =
                                        incident


                                    /*
                                     * Map now knows Back should
                                     * return to Incident Details.
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

                                        index =
                                            0,


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
                             */
                            currentDestination =
                                AppDestination.NOTIFICATIONS
                        }
                    }
                }
            }
        }


        /*
         * ==========================================
         * IN-APP SOS HEADS-UP ALERT
         * ==========================================
         *
         * Appears above whichever screen
         * is currently open.
         */
        incomingAlert?.let {
                alert ->


            IncomingAlertBanner(

                alert =
                    alert,


                /*
                 * Clicking popup opens
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