package com.kernitect.saharaandroid.ui.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernitect.saharaandroid.model.ReceivedAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    alerts: List<ReceivedAlert>,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 14.dp
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {

            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(
                    Alignment.CenterStart
                )
            ) {
                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription =
                        "Back",
                    tint = Color.Black
                )
            }

            Text(
                text = "Notifications",
                modifier = Modifier.align(
                    Alignment.Center
                ),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        if (alerts.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text =
                        "No SOS notifications yet.",
                    color =
                        Color(0xFF777777),
                    fontSize = 14.sp
                )
            }

        } else {

            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                items(
                    items = alerts,
                    key = {
                        it.packet.id
                    }
                ) { alert ->

                    NotificationCard(
                        alert = alert
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    alert: ReceivedAlert
) {
    val packet =
        alert.packet

    val critical =
        packet.priority ==
                "CRITICAL"

    val cardColor =
        if (critical) {
            Color(0xFFFFEEEE)
        } else {
            Color(0xFFF4F4F4)
        }

    val borderColor =
        if (critical) {
            Color(0xFFE60000)
        } else {
            Color(0xFFCCCCCC)
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                10.dp
            ),

        border =
            BorderStroke(
                width = 1.dp,
                color = borderColor
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardColor
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,
                    color =
                        if (critical) {
                            Color(0xFFE60000)
                        } else {
                            Color(0xFF555555)
                        },
                    modifier =
                        Modifier.size(
                            9.dp
                        )
                ) {}

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Text(
                    text =
                        if (critical) {
                            "CRITICAL SOS"
                        } else {
                            "HELP REQUEST"
                        },

                    modifier =
                        Modifier.weight(1f),

                    color =
                        if (critical) {
                            Color(0xFFE60000)
                        } else {
                            Color.Black
                        },

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        formatReceivedTime(
                            alert.receivedAt
                        ),

                    fontSize =
                        10.sp,

                    color =
                        Color(0xFF777777)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text = packet.message,
                fontSize = 13.sp,
                color = Color.Black
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    "Location: " +
                            "${packet.latitude}, ${packet.longitude}",

                fontSize =
                    11.sp,

                color =
                    Color(0xFF555555)
            )

            Text(
                text =
                    "Hop: ${packet.hopCount}/${packet.ttl}",

                modifier =
                    Modifier.padding(
                        top = 3.dp
                    ),

                fontSize =
                    11.sp,

                color =
                    Color(0xFF555555)
            )
        }
    }
}

private fun formatReceivedTime(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        )

    return formatter.format(
        Date(timestamp)
    )
}