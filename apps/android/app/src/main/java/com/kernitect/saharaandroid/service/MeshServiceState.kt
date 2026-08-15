package com.kernitect.saharaandroid.service

import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.model.RescuePacket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/*
 * Small communication bridge between:
 *
 * RescueMeshService
 *        ↕
 * MainActivity / Compose UI
 *
 * The BLE engine itself still exists ONLY
 * inside RescueMeshService.
 */
object MeshServiceState {

    data class ResponderDistance(
        val incidentId: String,
        val distanceMeters: Float,
        val responderAccuracyMeters: Float?,
        val updatedAt: Long
    )


    /*
     * Current human-readable mesh status.
     */
    val status =
        MutableStateFlow(
            "RESCUEMESH inactive"
        )


    /*
     * Whether the foreground service
     * currently exists.
     */
    val running =
        MutableStateFlow(
            false
        )


    /*
     * Used for the in-app heads-up banner
     * when the Activity happens to be open.
     *
     * Room remains the persistent source
     * of truth.
     */
    val incomingAlerts =
        MutableSharedFlow<ReceivedAlert>(
            extraBufferCapacity = 16
        )


    /*
     * Used by the send progress dialog.
     */
    val sentPackets =
        MutableSharedFlow<RescuePacket>(
            extraBufferCapacity = 16
        )


    /*
     * Live responder positions are intentionally not timeline rows.
     * Room stores only the significant first-nearby event.
     */
    val responderDistances =
        MutableStateFlow<Map<String, ResponderDistance>>(
            emptyMap()
        )
}
