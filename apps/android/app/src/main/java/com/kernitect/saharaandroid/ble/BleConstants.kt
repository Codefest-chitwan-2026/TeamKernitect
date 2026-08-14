package com.kernitect.saharaandroid.ble

import java.util.UUID

object BleConstants {

    /*
     * Main RESCUEMESH service.
     *
     * Android phones and Windows both use this.
     */
    val SERVICE_UUID: UUID =
        UUID.fromString(
            "12345678-1234-5678-1234-56789abcdef0"
        )

    /*
     * Characteristic carrying our JSON packet.
     */
    val MESSAGE_UUID: UUID =
        UUID.fromString(
            "12345678-1234-5678-1234-56789abcdef1"
        )

    /*
     * Android-only marker.
     *
     * Windows does NOT advertise this.
     *
     * This lets phones prefer another phone
     * before falling back to Windows.
     */
    val ANDROID_NODE_UUID: UUID =
        UUID.fromString(
            "12345678-1234-5678-1234-56789abcdef2"
        )

    /*
     * Standard Client Characteristic
     * Configuration Descriptor.
     */
    val CCCD_UUID: UUID =
        UUID.fromString(
            "00002902-0000-1000-8000-00805f9b34fb"
        )
}