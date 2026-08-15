package com.kernitect.sahararesponder.mesh

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kernitect.sahararesponder.ble.ResponderBleScanner
import com.kernitect.sahararesponder.ble.ResponderGattClient
import com.kernitect.sahararesponder.model.RescueAckPacket

class ResponderMeshSender(
    context: Context,
    private val onStateChanged: (incidentId: String, state: AckSendState, failureReason: String?) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingPacket: RescueAckPacket? = null
    private val scanner = ResponderBleScanner(
        context = context.applicationContext,
        onDeviceFound = { device ->
            val packet = pendingPacket ?: return@ResponderBleScanner
            handler.removeCallbacks(scanTimeout)
            update(packet, AckSendState.CONNECTING)
            gattClient.connectAndSend(device, packet.toJson())
        },
        onFailure = { fail(it) },
    )
    private val gattClient = ResponderGattClient(
        context = context.applicationContext,
        onPhaseChanged = { phase ->
            val packet = pendingPacket ?: return@ResponderGattClient
            if (phase == "SENDING") update(packet, AckSendState.SENDING)
        },
        onSuccess = {
            val packet = pendingPacket ?: return@ResponderGattClient
            update(packet, AckSendState.SENT_TO_MESH)
            pendingPacket = null
        },
        onFailure = { fail(it) },
    )
    private val scanTimeout = Runnable { fail("No nearby Android RESCUEMESH relay found") }

    fun send(packet: RescueAckPacket) {
        cancelTransport()
        pendingPacket = packet
        Log.i(TAG, "Searching for RESCUEMESH relay")
        update(packet, AckSendState.SEARCHING)
        scanner.start()
        handler.postDelayed(scanTimeout, ANDROID_SCAN_TIMEOUT_MS)
    }

    private fun fail(reason: String) {
        val packet = pendingPacket ?: return
        scanner.stop()
        handler.removeCallbacks(scanTimeout)
        update(packet, AckSendState.FAILED, reason)
        pendingPacket = null
    }

    private fun update(packet: RescueAckPacket, state: AckSendState, reason: String? = null) {
        handler.post { onStateChanged(packet.incidentId, state, reason) }
    }

    private fun cancelTransport() {
        scanner.stop()
        gattClient.close()
        handler.removeCallbacks(scanTimeout)
        pendingPacket = null
    }

    fun close() = cancelTransport()

    private companion object {
        const val TAG = "SaharaResponder"
        const val ANDROID_SCAN_TIMEOUT_MS = 3_000L
    }
}
