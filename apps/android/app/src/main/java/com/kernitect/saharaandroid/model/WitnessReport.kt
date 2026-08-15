package com.kernitect.saharaandroid.model

import java.util.UUID

data class WitnessReport(
    val id: String = UUID.randomUUID().toString(),

    /*
     * Original SOS packet ID.
     */
    val incidentId: String,

    val disasterType: String,

    val peopleCount: String,

    val message: String,

    val createdAt: Long =
        System.currentTimeMillis()
)