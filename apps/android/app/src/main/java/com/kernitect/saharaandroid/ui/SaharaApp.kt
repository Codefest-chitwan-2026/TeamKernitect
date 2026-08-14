package com.kernitect.saharaandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kernitect.saharaandroid.ui.navigation.AppDestination
import com.kernitect.saharaandroid.ui.navigation.BottomNavigationBar
import com.kernitect.saharaandroid.ui.screens.help.HelpScreen
import com.kernitect.saharaandroid.ui.screens.home.HomeScreen
import com.kernitect.saharaandroid.ui.screens.map.MapScreen
import androidx.compose.ui.graphics.Color

@Composable
fun SaharaApp(
    onCriticalSos: () -> Unit,

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

    Scaffold(
        containerColor = Color.White,
        bottomBar = {

            BottomNavigationBar(
                currentDestination = currentDestination,

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
    ) { innerPadding ->

        Box(
            modifier = Modifier.padding(
                innerPadding
            )
        ) {
            when (currentDestination) {

                AppDestination.HOME -> {

                    HomeScreen(
                        onMapClick = {
                            currentDestination =
                                AppDestination.MAP
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
            }
        }
    }
}