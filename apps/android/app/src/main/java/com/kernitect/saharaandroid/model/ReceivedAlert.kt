package com.kernitect.saharaandroid.model

data class ReceivedAlert(
    val packet: RescuePacket,
    val receivedAt: Long = System.currentTimeMillis()
)