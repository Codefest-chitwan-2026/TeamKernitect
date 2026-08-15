package com.kernitect.saharaandroid.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kernitect.saharaandroid.R
import com.kernitect.saharaandroid.ui.components.SosButton

@Composable
fun BottomNavigationBar(
    currentDestination: AppDestination,
    onHomeClick: () -> Unit,
    onSosClick: () -> Unit,
    onMapClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
    ) {

        /*
         * GRAY NAVIGATION BACKGROUND
         *
         * Taller than the original version.
         * Rounded top corners give the bar a
         * softer, more modern appearance.
         */
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .align(Alignment.BottomCenter),
            color = Color(0xFFD9D9D9)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            )
        }

        /*
         * HOME ICON
         *
         * Pulled inward toward the SOS button.
         *
         * Active = black
         * Inactive = gray
         *
         * No ripple or pressed background.
         */
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 62.dp,
                    bottom = 10.dp
                )
                .size(42.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHomeClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.home
                ),
                contentDescription = "Home",
                modifier = Modifier.size(34.dp),
                colorFilter = ColorFilter.tint(
                    if (
                        currentDestination ==
                        AppDestination.HOME
                    ) {
                        Color.Black
                    } else {
                        Color(0xFF777777)
                    }
                )
            )
        }

        /*
         * MAP ICON
         *
         * Same spacing as Home.
         *
         * Active = black
         * Inactive = gray
         *
         * No ripple or pressed background.
         */
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 62.dp,
                    bottom = 10.dp
                )
                .size(42.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMapClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.map
                ),
                contentDescription = "Map",
                modifier = Modifier.size(34.dp),
                colorFilter = ColorFilter.tint(
                    if (
                        currentDestination ==
                        AppDestination.MAP
                    ) {
                        Color.Black
                    } else {
                        Color(0xFF777777)
                    }
                )
            )
        }

        /*
         * SOS BUTTON
         *
         * Sits above the navigation bar.
         *
         * The SosButton itself uses the
         * double-circle design:
         *
         *     WHITE OUTER CIRCLE
         *       RED INNER CIRCLE
         *            SOS
         */
        SosButton(
            onClick = onSosClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
    }
}