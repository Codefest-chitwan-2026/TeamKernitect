package com.kernitect.saharaandroid.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            .height(92.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter),
            color = Color(0xFFE0E0E0)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onHomeClick
                ) {
                    Text(
                        text = "HOME",
                        color = if (currentDestination == AppDestination.HOME) {
                            Color.Black
                        } else {
                            Color.DarkGray
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onMapClick
                ) {
                    Text(
                        text = "MAP",
                        color = if (currentDestination == AppDestination.MAP) {
                            Color.Black
                        } else {
                            Color.DarkGray
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SosButton(
            onClick = onSosClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
    }
}