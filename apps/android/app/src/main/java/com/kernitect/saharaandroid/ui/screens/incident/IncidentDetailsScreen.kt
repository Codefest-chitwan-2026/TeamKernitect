package com.kernitect.saharaandroid.ui.screens.incident

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.model.RescuePacket
import com.kernitect.saharaandroid.model.WitnessReport
import com.kernitect.saharaandroid.ui.components.SaharaTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentDetailsScreen(

    alert: ReceivedAlert,

    witnessReports: List<WitnessReport> =
        emptyList(),

    onBackClick: () -> Unit =
        {},

    onOpenFullMap: () -> Unit =
        {},

    onAddDetails: (
        disasterType: String,
        peopleCount: String,
        explanation: String
    ) -> Unit = { _, _, _ -> }

) {

    val packet =
        alert.packet

    val isCritical =
        packet.priority ==
                RescuePacket.PRIORITY_CRITICAL


    /*
     * Witness form.
     */
    var disasterType by
    remember(
        packet.id
    ) {

        mutableStateOf(
            ""
        )
    }


    var peopleCount by
    remember(
        packet.id
    ) {

        mutableStateOf(
            ""
        )
    }


    var explanation by
    remember(
        packet.id
    ) {

        mutableStateOf(
            ""
        )
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    bottom = 24.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        /*
         * =====================================
         * TOP BAR
         * =====================================
         */
        SaharaTopBar(
            title =
                "Incident Details",

            onBackClick =
                onBackClick
        )


        /*
         * =====================================
         * INCIDENT HEADER
         * =====================================
         */
        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    12.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =

                        if (
                            isCritical
                        ) {

                            Color(
                                0xFFFFEEEE
                            )

                        } else {

                            Color(
                                0xFFFFF7E8
                            )
                        }
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                Text(
                    text =

                        if (
                            isCritical
                        ) {

                            "CRITICAL SOS"

                        } else {

                            "HELP REQUEST"
                        },

                    color =

                        if (
                            isCritical
                        ) {

                            Color(
                                0xFFD90000
                            )

                        } else {

                            Color(
                                0xFFD56D00
                            )
                        },

                    fontSize =
                        22.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        "Priority: ${formatValue(packet.priority)}",

                    modifier =
                        Modifier.padding(
                            top = 6.dp
                        ),

                    color =
                        Color(
                            0xFF444444
                        ),

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Text(
                    text =
                        "Received ${formatDateTime(alert.receivedAt)}",

                    modifier =
                        Modifier.padding(
                            top = 3.dp
                        ),

                    color =
                        Color(
                            0xFF666666
                        ),

                    fontSize =
                        12.sp
                )
            }
        }


        /*
         * =====================================
         * DISASTER CONTEXT
         * =====================================
         *
         * Only meaningful when the SOS contains
         * locally inferred emergency context.
         */
        if (
            packet.likelyDisaster !=
            RescuePacket.DISASTER_UNKNOWN
        ) {

            SectionCard(
                title =
                    "Emergency Context"
            ) {

                DetailRow(
                    label =
                        "Likely disaster",

                    value =
                        formatValue(
                            packet.likelyDisaster
                        )
                )


                DetailRow(
                    label =
                        "Area severity",

                    value =

                        if (
                            packet.areaSeverity ==
                            RescuePacket.SEVERITY_UNKNOWN
                        ) {

                            "Not available"

                        } else {

                            formatValue(
                                packet.areaSeverity
                            )
                        }
                )


                Text(
                    text =
                        "Disaster context is inferred from the active local emergency alert for the sender's location.",

                    modifier =
                        Modifier.padding(
                            top = 8.dp
                        ),

                    color =
                        Color(
                            0xFF777777
                        ),

                    fontSize =
                        11.sp
                )
            }
        }


        /*
         * =====================================
         * REQUEST DETAILS
         * =====================================
         */
        SectionCard(
            title =
                "Request Details"
        ) {

            /*
             * Don't dump the long context string
             * for critical SOS packets.
             *
             * Disaster information is already shown
             * in structured fields above.
             */
            Text(
                text =

                    if (
                        isCritical
                    ) {

                        "Critical emergency assistance requested."

                    } else {

                        packet.message
                    },

                color =
                    Color(
                        0xFF444444
                    ),

                fontSize =
                    14.sp,

                lineHeight =
                    20.sp
            )


            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        vertical = 12.dp
                    )
            )


            DetailRow(
                label =
                    "Origin time",

                value =
                    formatDateTime(
                        packet.timestamp
                    )
            )


            DetailRow(
                label =
                    "Relay hop",

                value =
                    "${packet.hopCount}/${packet.ttl}"
            )


            DetailRow(
                label =
                    "Incident ID",

                value =
                    packet.id.take(
                        8
                    ).uppercase()
            )
        }


        /*
         * =====================================
         * LOCATION
         * =====================================
         */
        SectionCard(
            title =
                "Incident Location"
        ) {

            DetailRow(
                label =
                    "Latitude",

                value =
                    String.format(
                        Locale.US,
                        "%.6f",
                        packet.latitude
                    )
            )


            DetailRow(
                label =
                    "Longitude",

                value =
                    String.format(
                        Locale.US,
                        "%.6f",
                        packet.longitude
                    )
            )


            Button(
                onClick =
                    onOpenFullMap,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        ),

                shape =
                    RoundedCornerShape(
                        10.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(
                                0xFFE53935
                            ),

                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        "OPEN FULL MAP",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        /*
         * =====================================
         * WITNESS REPORTS
         * =====================================
         */
        SectionCard(
            title =
                "Witness Information"
        ) {

            if (
                witnessReports.isEmpty()
            ) {

                Text(
                    text =
                        "No additional witness information has been added.",

                    color =
                        Color(
                            0xFF777777
                        ),

                    fontSize =
                        13.sp
                )

            } else {

                witnessReports.forEachIndexed {
                        index,
                        report ->


                    if (
                        index > 0
                    ) {

                        HorizontalDivider(
                            modifier =
                                Modifier.padding(
                                    vertical = 10.dp
                                )
                        )
                    }


                    if (
                        report.disasterType
                            .isNotBlank()
                    ) {

                        DetailRow(
                            label =
                                "Disaster",

                            value =
                                report.disasterType
                        )
                    }


                    if (
                        report.peopleCount
                            .isNotBlank()
                    ) {

                        DetailRow(
                            label =
                                "People",

                            value =
                                report.peopleCount
                        )
                    }


                    if (
                        report.message
                            .isNotBlank()
                    ) {

                        Text(
                            text =
                                report.message,

                            modifier =
                                Modifier.padding(
                                    top = 5.dp
                                ),

                            color =
                                Color(
                                    0xFF444444
                                ),

                            fontSize =
                                13.sp
                        )
                    }


                    Text(
                        text =
                            formatDateTime(
                                report.createdAt
                            ),

                        modifier =
                            Modifier.padding(
                                top = 5.dp
                            ),

                        color =
                            Color(
                                0xFF888888
                            ),

                        fontSize =
                            11.sp
                    )
                }
            }
        }


        /*
         * =====================================
         * ADD WITNESS INFORMATION
         * =====================================
         */
        SectionCard(
            title =
                "Add Information"
        ) {

            Text(
                text =
                    "Add details you observed about this incident.",

                color =
                    Color(
                        0xFF666666
                    ),

                fontSize =
                    13.sp
            )


            OutlinedTextField(
                value =
                    disasterType,

                onValueChange = {

                    disasterType =
                        it
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        ),

                label = {

                    Text(
                        "Disaster type"
                    )
                },

                singleLine =
                    true
            )


            OutlinedTextField(
                value =
                    peopleCount,

                onValueChange = {

                    peopleCount =
                        it
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 8.dp
                        ),

                label = {

                    Text(
                        "People affected"
                    )
                },

                singleLine =
                    true
            )


            OutlinedTextField(
                value =
                    explanation,

                onValueChange = {

                    explanation =
                        it
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 8.dp
                        ),

                label = {

                    Text(
                        "Additional details"
                    )
                },

                minLines =
                    3
            )


            Button(
                onClick = {

                    onAddDetails(
                        disasterType.trim(),
                        peopleCount.trim(),
                        explanation.trim()
                    )


                    /*
                     * Clear form after adding.
                     */
                    disasterType =
                        ""

                    peopleCount =
                        ""

                    explanation =
                        ""
                },

                enabled =

                    disasterType
                        .isNotBlank() ||

                            peopleCount
                                .isNotBlank() ||

                            explanation
                                .isNotBlank(),

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 12.dp
                        ),

                shape =
                    RoundedCornerShape(
                        10.dp
                    )
            ) {

                Text(
                    text =
                        "ADD DETAILS",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        /*
         * Back button at bottom as well,
         * useful on long incident pages.
         */
        OutlinedButton(
            onClick =
                onBackClick,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    "BACK TO NOTIFICATIONS"
            )
        }
    }
}


@Composable
private fun SectionCard(

    title: String,

    content:
    @Composable () -> Unit

) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                12.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(
                text =
                    title,

                color =
                    Color(
                        0xFF222222
                    ),

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Column(
                modifier =
                    Modifier.padding(
                        top = 10.dp
                    )
            ) {

                content()
            }
        }
    }
}


@Composable
private fun DetailRow(

    label: String,

    value: String

) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text =
                label,

            modifier =
                Modifier.weight(
                    1f
                ),

            color =
                Color(
                    0xFF777777
                ),

            fontSize =
                12.sp
        )


        Text(
            text =
                value,

            modifier =
                Modifier.weight(
                    1.4f
                ),

            color =
                Color(
                    0xFF333333
                ),

            fontSize =
                12.sp,

            fontWeight =
                FontWeight.SemiBold
        )
    }
}


private fun formatValue(
    value: String
): String {

    return value
        .lowercase()
        .replace(
            "_",
            " "
        )
        .split(
            " "
        )
        .joinToString(
            " "
        ) {
                word ->

            word.replaceFirstChar {

                if (
                    it.isLowerCase()
                ) {

                    it.titlecase(
                        Locale.getDefault()
                    )

                } else {

                    it.toString()
                }
            }
        }
}


private fun formatDateTime(
    timestamp: Long
): String {

    val formatter =
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )


    return formatter.format(
        Date(
            timestamp
        )
    )
}