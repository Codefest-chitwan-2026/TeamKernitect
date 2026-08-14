package com.kernitect.saharaandroid.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
            .height(94.dp)
    ) {

        /*
         * Gray bottom area from the design.
         */
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter),
            color = Color(0xFFD9D9D9),
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 34.dp,
                        end = 34.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                /*
                 * HOME
                 */
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {

                    IconButton(
                        onClick = onHomeClick
                    ) {

                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(34.dp),
                            tint =
                                if (
                                    currentDestination ==
                                    AppDestination.HOME
                                ) {
                                    Color.Black
                                } else {
                                    Color(0xFF6F6F6F)
                                }
                        )
                    }
                }

                /*
                 * Empty middle space for the SOS button.
                 */
                Box(
                    modifier = Modifier.weight(1f)
                )

                /*
                 * MAP
                 */
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {

                    IconButton(
                        onClick = onMapClick
                    ) {

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Map",
                            modifier = Modifier.size(34.dp),
                            tint =
                                if (
                                    currentDestination ==
                                    AppDestination.MAP
                                ) {
                                    Color.Black
                                } else {
                                    Color(0xFF6F6F6F)
                                }
                        )
                    }
                }
            }
        }

        /*
         * Large critical SOS button.
         *
         * It sits above the navigation background,
         * like the Figma design.
         */
        SosButton(
            onClick = onSosClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
    }
}