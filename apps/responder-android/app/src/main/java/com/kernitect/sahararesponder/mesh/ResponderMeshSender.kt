package com.kernitect.sahararesponder.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kernitect.sahararesponder.ble.ResponderBleScanner
import com.kernitect.sahararesponder.ble.ResponderGattClient
import com.kernitect.sahararesponder.model.MeshOutgoingPacket

class ResponderMeshSender(
    context: Context,
    private val onStateChanged: (packet: MeshOutgoingPacket, state: AckSendState, failureReason: String?) -> Unit,
) {
    enum class PeerTarget { ANY_ANDROID, RESPONDER }
    private data class QueuedSend(val packet: MeshOutgoingPacket, val excludedAddress: String?, val target: PeerTarget)
    private val handler = Handler(Looper.getMainLooper())
    private var pendingSend: QueuedSend? = null
    private var cachedAttemptAddress: String? = null
    private var fallbackScanStarted = false
    private val queue = ArrayDeque<QueuedSend>()
    private val responderCache = NearbyPeerCache<BluetoothDevice>(RESPONDER_CACHE_FRESH_MS)
    private val scanner = ResponderBleScanner(
        context = context.applicationContext,
        onDeviceFound = { device ->
            val send = pendingSend
            if (send == null) {
                safeAddress(device)?.let { responderCache.remember(device, it, System.currentTimeMillis()) }
                handler.removeCallbacks(warmScanTimeout)
                Log.i(TAG, "Nearby responder cached")
                return@ResponderBleScanner
            }
            val packet = send.packet
            if (send.target == PeerTarget.RESPONDER) {
                safeAddress(device)?.let { responderCache.remember(device, it, System.currentTimeMillis()) }
            }
            handler.removeCallbacks(scanTimeout)
            update(packet, AckSendState.CONNECTING)
            gattClient.connectAndSend(device, packet.toJson())
        },
        onFailure = { if (pendingSend != null) fail(it) },
    )
    private val gattClient = ResponderGattClient(
        context = context.applicationContext,
        onPhaseChanged = { phase ->
            val packet = pendingSend?.packet ?: return@ResponderGattClient
            if (phase == "SENDING") update(packet, AckSendState.SENDING)
        },
        onSuccess = {
            val packet = pendingSend?.packet ?: return@ResponderGattClient
            update(packet, AckSendState.SENT_TO_MESH)
            pendingSend = null
            startNext()
        },
        onFailure = { cachedFailureOrFail(it) },
    )
    private val scanTimeout = Runnable { fail("No nearby Android RESCUEMESH relay found") }
    private val warmScanTimeout: Runnable = Runnable { stopWarmScan() }

    private fun stopWarmScan() = scanner.stop()

    fun warmResponderDiscovery(excludedAddress: String? = null) {
        if (pendingSend != null) return
        handler.removeCallbacks(warmScanTimeout)
        scanner.start(excludedAddress, requireResponderPeer = true)
        handler.postDelayed(warmScanTimeout, WARM_SCAN_MS)
        Log.i(TAG, "Warming nearby responder discovery")
    }

    fun cancelWarmDiscovery() {
        if (pendingSend == null) scanner.stop()
        handler.removeCallbacks(warmScanTimeout)
    }

    fun send(packet: MeshOutgoingPacket, excludedAddress: String? = null, target: PeerTarget = PeerTarget.ANY_ANDROID) {
        if (pendingSend?.packet?.id == packet.id || queue.any { it.packet.id == packet.id }) return
        queue.addLast(QueuedSend(packet, excludedAddress, target))
        if (pendingSend == null) startNext()
    }

    private fun startNext() {
        val send = queue.removeFirstOrNull() ?: return
        handler.removeCallbacks(warmScanTimeout)
        scanner.stop()
        val packet = send.packet
        pendingSend = send
        fallbackScanStarted = false
        cachedAttemptAddress = null
        val cached = if (send.target == PeerTarget.RESPONDER) {
            responderCache.select(System.currentTimeMillis(), send.excludedAddress)
        } else null
        if (cached != null) {
            cachedAttemptAddress = cached.address
            Log.i(TAG, "Using pre-discovered responder")
            update(packet, AckSendState.CONNECTING)
            gattClient.connectAndSend(cached.value, packet.toJson())
        } else {
            startFallbackScan(send)
        }
    }

    private fun startFallbackScan(send: QueuedSend) {
        fallbackScanStarted = true
        Log.i(TAG, "Searching for RESCUEMESH relay")
        update(send.packet, AckSendState.SEARCHING)
        scanner.start(send.excludedAddress, send.target == PeerTarget.RESPONDER)
        handler.postDelayed(scanTimeout, ANDROID_SCAN_TIMEOUT_MS)
    }

    private fun cachedFailureOrFail(reason: String) {
        val send = pendingSend ?: return
        val attempted = cachedAttemptAddress
        if (attempted != null && !fallbackScanStarted) {
            responderCache.invalidate(attempted)
            cachedAttemptAddress = null
            Log.i(TAG, "Cached responder unavailable; falling back to scan")
            startFallbackScan(send)
        } else fail(reason)
    }

    private fun fail(reason: String) {
        val packet = pendingSend?.packet ?: return
        scanner.stop()
        handler.removeCallbacks(scanTimeout)
        update(packet, AckSendState.FAILED, reason)
        pendingSend = null
        cachedAttemptAddress = null
        startNext()
    }

    private fun update(packet: MeshOutgoingPacket, state: AckSendState, reason: String? = null) {
        handler.post { onStateChanged(packet, state, reason) }
    }

    private fun cancelTransport() {
        scanner.stop()
        gattClient.close()
        handler.removeCallbacks(scanTimeout)
        handler.removeCallbacks(warmScanTimeout)
        pendingSend = null
        queue.clear()
    }

    fun close() = cancelTransport()

    @SuppressLint("MissingPermission")
    private fun safeAddress(device: BluetoothDevice): String? = try { device.address } catch (_: SecurityException) { null }

    private companion object {
        const val TAG = "SaharaResponder"
        const val ANDROID_SCAN_TIMEOUT_MS = 3_000L
        const val WARM_SCAN_MS = 5_000L
        const val RESPONDER_CACHE_FRESH_MS = 15_000L
    }
}

data class NearbyPeer<T>(val value: T, val address: String, val lastSeenAt: Long)

class NearbyPeerCache<T>(private val freshnessMs: Long) {
    private var peer: NearbyPeer<T>? = null

    fun remember(value: T, address: String, seenAt: Long) {
        peer = NearbyPeer(value, address, seenAt)
    }

    fun select(now: Long, excludedAddress: String?): NearbyPeer<T>? = peer?.takeIf {
        now - it.lastSeenAt <= freshnessMs && !it.address.equals(excludedAddress, ignoreCase = true)
    }

    fun invalidate(address: String) {
        if (peer?.address.equals(address, ignoreCase = true)) peer = null
    }
}
