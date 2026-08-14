package com.kernitect.saharaandroid.ble

import java.util.UUID

object BleConstants {
    val SERVICE_UUID: UUID =
        UUID.fromString("2345678-1234-5678-1234-56789abcdef0")

    val MESSAGE_UUID: UUID =
        UUID.fromString("12345678-1234-5678-1234-56789abcdef1")

    val CCCD_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val DEVICE_NAME = "SAHARA"
}