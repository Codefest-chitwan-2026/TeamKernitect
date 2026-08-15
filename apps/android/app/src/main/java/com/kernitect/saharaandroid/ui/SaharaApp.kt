package com.kernitect.saharaandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.ui.components.IncomingAlertBanner
import com.kernitect.saharaandroid.ui.navigation.AppDestination
import com.kernitect.saharaandroid.ui.navigation.BottomNavigationBar
import com.kernitect.saharaandroid.ui.screens.help.HelpScreen
import com.kernitect.saharaandroid.ui.screens.home.HomeScreen
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

    var currentDestination by remember {
        mutableStateOf(
            AppDestination.HOME
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Scaffold(
            containerColor = Color.White,

            /*
             * Hide bottom navigation while
             * viewing notifications.
             */
            bottomBar = {

                if (
                    currentDestination !=
                    AppDestination.NOTIFICATIONS
                ) {

                    BottomNavigationBar(
                        currentDestination =
                            currentDestination,

                        onHomeClick = {

                            currentDestination =
                                AppDestination.HOME
                        },

                        onSosClick =
                            onCriticalSos,

                        onMapClick = {

                            currentDestination =
                                AppDestination.MAP
                        }
                    )
                }
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
            ) {

                when (
                    currentDestination
                ) {

                    AppDestination.HOME -> {

                        HomeScreen(
                            onMapClick = {

                                currentDestination =
                                    AppDestination.MAP
                            },

                            unreadNotificationCount =
                                unreadNotificationCount,

                            onNotificationClick = {

                                /*
                                 * Clear unread badge.
                                 */
                                onNotificationsOpened()

                                /*
                                 * Open notification history.
                                 */
                                currentDestination =
                                    AppDestination.NOTIFICATIONS
                            },

                            onSendHelpRequest =
                                onNonEmergencyRequest
                        )
                    }

                    AppDestination.HELP -> {

                        HelpScreen()
                    }

                    AppDestination.MAP -> {

                        MapScreen()
                    }

                    AppDestination.NOTIFICATIONS -> {

                        NotificationsScreen(
                            alerts =
                                receivedAlerts,

                            onBackClick = {

                                currentDestination =
                                    AppDestination.HOME
                            }
                        )
                    }
                }
            }
        }

        /*
         * =========================================
         * IN-APP HEADS-UP NOTIFICATION
         * =========================================
         *
         * Appears above whichever screen
         * the user is currently viewing.
         */
        incomingAlert?.let { alert ->

            IncomingAlertBanner(
                alert = alert,

                /*
                 * Clicking the banner opens
                 * notification history.
                 */
                onClick = {

                    /*
                     * Remove the temporary banner.
                     */
                    onIncomingAlertDismissed()

                    /*
                     * Clear unread badge because
                     * notifications are being opened.
                     */
                    onNotificationsOpened()

                    currentDestination =
                        AppDestination.NOTIFICATIONS
                },

                /*
                 * Called automatically when
                 * the countdown bar finishes.
                 */
                onDismiss = {

                    onIncomingAlertDismissed()
                },

                modifier = Modifier
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