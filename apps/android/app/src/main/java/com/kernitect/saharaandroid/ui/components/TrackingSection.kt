package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TrackingUpdate(
    val date: String,
    val time: String,
    val message: String
)

@Composable
fun TrackingSection(
    updates: List<TrackingUpdate> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        Text(
            text = "Tracking",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        if (updates.isEmpty()) {

            Row(
                modifier = Modifier.padding(
                    top = 14.dp,
                    bottom = 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color(0xFF777777),
                            shape = CircleShape
                        )
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "No tracking updates yet",
                    fontSize = 11.sp,
                    color = Color(0xFF777777)
                )
            }

        } else {

            updates.forEachIndexed { index, update ->

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            text = update.date,
                            fontSize = 9.sp,
                            color = Color(0xFF555555)
                        )

                        Text(
                            text = update.time,
                            fontSize = 9.sp,
                            color = Color(0xFF777777)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                        )

                        if (index != updates.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(
                                        Color(0xFF999999)
                                    )
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = update.message,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}