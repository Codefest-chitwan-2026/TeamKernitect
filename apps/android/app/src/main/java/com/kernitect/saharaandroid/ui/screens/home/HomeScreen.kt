package com.kernitect.saharaandroid.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kernitect.saharaandroid.ui.components.EmergencyCard
import com.kernitect.saharaandroid.ui.components.HelpRequestForm
import com.kernitect.saharaandroid.ui.components.SaharaTopBar
import com.kernitect.saharaandroid.ui.components.TrackingSection

@Composable
fun HomeScreen(
    onMapClick: () -> Unit,

    onSendHelpRequest: (
        disasterType: String,
        peopleCount: String,
        explanation: String
    ) -> Unit = { _, _, _ -> }
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 4.dp,
                bottom = 18.dp
            ),
        verticalArrangement = Arrangement.spacedBy(
            14.dp
        )
    ) {

        SaharaTopBar(
            title = "Home"
        )

        EmergencyCard(
            type = "—",
            location = "—",
            onMapClick = onMapClick
        )

        HelpRequestForm(
            onSend = onSendHelpRequest
        )

        TrackingSection(
            modifier = Modifier.padding(
                top = 2.dp
            )
        )
    }
}