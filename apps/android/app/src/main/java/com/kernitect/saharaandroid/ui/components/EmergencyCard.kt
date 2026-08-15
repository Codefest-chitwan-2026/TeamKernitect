package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernitect.saharaandroid.data.local.entity.PublicAlertEntity

@Composable
fun EmergencyCard(
    alert: PublicAlertEntity? = null,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier =
            modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(8.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFFF5A5F)
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
        ) {

            /*
             * ============================
             * NO LOCAL ALERT
             * ============================
             */
            if (alert == null) {

                Text(
                    text = "Emergency Alert",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "No active local emergency alert",
                    modifier =
                        Modifier.padding(
                            top = 6.dp
                        ),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Your location is not currently inside a stored emergency zone.",
                    modifier =
                        Modifier.padding(
                            top = 4.dp
                        ),
                    color =
                        Color.White.copy(
                            alpha = 0.9f
                        ),
                    fontSize = 12.sp
                )

                return@Column
            }


            /*
             * ============================
             * ACTIVE LOCAL ALERT
             * ============================
             */

            Text(
                text = "Emergency Alert",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )


            /*
             * Hackathon demo marker.
             */


            Text(
                text =
                    "Type: ${
                        alert.disasterType
                            .replace(
                                "_",
                                " "
                            )
                    }",

                modifier =
                    Modifier.padding(
                        top = 5.dp
                    ),

                color = Color.White,

                fontSize = 15.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    "Severity: ${
                        alert.severity
                            .replace(
                                "_",
                                " "
                            )
                    }",

                modifier =
                    Modifier.padding(
                        top = 2.dp
                    ),

                color = Color.White,

                fontSize = 13.sp,

                fontWeight =
                    FontWeight.Medium
            )


            Text(
                text =
                    alert.message,

                modifier =
                    Modifier.padding(
                        top = 7.dp
                    ),

                color =
                    Color.White.copy(
                        alpha = 0.95f
                    ),

                fontSize = 13.sp
            )


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 9.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            alert.municipality,

                        color = Color.White,

                        fontSize = 12.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(
                        text =
                            "${alert.district}, ${alert.province}",

                        color =
                            Color.White.copy(
                                alpha = 0.9f
                            ),

                        fontSize = 11.sp
                    )
                }


                Button(
                    onClick =
                        onMapClick,

                    shape =
                        RoundedCornerShape(50),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFF3BEBE),

                            contentColor =
                                Color.Black
                        ),

                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 15.dp,
                            vertical = 3.dp
                        )
                ) {

                    Text(
                        text = "MAP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}