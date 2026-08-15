package com.kernitect.saharaandroid.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernitect.saharaandroid.model.ReceivedAlert

@Composable
fun IncomingAlertBanner(
    alert: ReceivedAlert,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(
        alert.packet.id
    ) {
        Animatable(1f)
    }

    LaunchedEffect(
        alert.packet.id
    ) {
        progress.snapTo(1f)

        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 4000,
                easing = LinearEasing
            )
        )

        onDismiss()
    }

    val isCritical =
        alert.packet.priority == "CRITICAL"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isCritical) {
                    Color(0xFFFFEEEE)
                } else {
                    Color.White
                }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column {

            Column(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 13.dp
                )
            ) {

                Text(
                    text =
                        if (isCritical) {
                            "SOS ALERT RECEIVED"
                        } else {
                            "HELP REQUEST RECEIVED"
                        },
                    color =
                        if (isCritical) {
                            Color(0xFFE60000)
                        } else {
                            Color.Black
                        },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = alert.packet.message,
                    modifier = Modifier.padding(
                        top = 4.dp
                    ),
                    color = Color(0xFF555555),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            /*
             * Countdown bar.
             *
             * Left side stays fixed while the
             * right side moves toward the left.
             */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Color(0xFFE0E0E0)
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            progress.value
                        )
                        .height(4.dp)
                        .clip(
                            RoundedCornerShape(50)
                        )
                        .background(
                            if (isCritical) {
                                Color(0xFFE60000)
                            } else {
                                Color(0xFF555555)
                            }
                        )
                )
            }
        }
    }
}