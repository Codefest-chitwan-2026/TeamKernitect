package com.kernitect.saharaandroid.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kernitect.saharaandroid.data.local.entity.PublicAlertEntity
import com.kernitect.saharaandroid.data.local.entity.IncidentEntity
import com.kernitect.saharaandroid.data.local.entity.TrackingEventEntity
import com.kernitect.saharaandroid.service.MeshServiceState
import com.kernitect.saharaandroid.ui.components.EmergencyCard
import com.kernitect.saharaandroid.ui.components.HelpRequestForm
import com.kernitect.saharaandroid.ui.components.SaharaTopBar
import com.kernitect.saharaandroid.ui.components.TrackingSection

@Composable
fun HomeScreen(

    /*
     * Local public disaster warning
     * matched against this phone's GPS.
     */
    publicAlert: PublicAlertEntity? =
        null,

    onMapClick: () -> Unit,

    unreadNotificationCount: Int =
        0,

    localIncidents: List<IncidentEntity> =
        emptyList(),

    trackingEvents: List<TrackingEventEntity> =
        emptyList(),

    responderDistances: Map<String, MeshServiceState.ResponderDistance> =
        emptyMap(),

    onNotificationClick: () -> Unit =
        {},

    onSendHelpRequest: (
        disasterType: String,
        peopleCount: String,
        explanation: String
    ) -> Unit = { _, _, _ -> }
) {

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
                    top = 4.dp,
                    bottom = 18.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        SaharaTopBar(
            title = "Home",

            unreadCount =
                unreadNotificationCount,

            onNotificationClick =
                onNotificationClick
        )


        /*
         * PUBLIC / AREA EMERGENCY WARNING.
         *
         * This is different from the user's
         * personal SOS button.
         */
        EmergencyCard(
            alert =
                publicAlert,

            onMapClick =
                onMapClick
        )


        HelpRequestForm(
            onSend =
                onSendHelpRequest
        )


        TrackingSection(
            incidents = localIncidents,

            events = trackingEvents,

            responderDistances = responderDistances,

            modifier =
                Modifier.padding(
                    top = 2.dp
                )
        )
    }
}
