package com.kernitect.saharaandroid.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kernitect.saharaandroid.ble.BleAdvertiser
import com.kernitect.saharaandroid.ble.BleGattClient
import com.kernitect.saharaandroid.ble.BleGattServer
import com.kernitect.saharaandroid.ble.BleScanner
import com.kernitect.saharaandroid.model.RescuePacket

class MeshEngine(
    context: Context,

    private val onStatusChanged:
        (String) -> Unit,

    private val onPacketReceived:
        (RescuePacket) -> Unit,

    private val onPacketSent:
        (RescuePacket) -> Unit
) {

    companion object {

        private const val TAG =
            "SAHARA_MESH"

        private const val ANDROID_SCAN_MS =
            3_000L

        private const val FALLBACK_SCAN_MS =
            12_000L
    }

    private enum class ScanPhase {

        ANDROID_PREFERRED,

        ANY_RESCUEMESH
    }

    private val appContext =
        context.applicationContext

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val relayManager =
        RelayManager()

    private var started =
        false

    /*
     * Packet stays here until successful write.
     */
    private var pendingPacket:
            RescuePacket? = null

    /*
     * Non-null while sending.
     */
    private var sendingPacket:
            RescuePacket? = null

    /*
     * Extra safeguard against immediate loopback.
     */
    private var excludedAddress:
            String? = null

    private var sendingAddress:
            String? = null

    private var scanPhase:
            ScanPhase? = null

    private val advertiser =
        BleAdvertiser(
            context = appContext,

            onStatusChanged = { status ->

                postStatus(
                    "Advertiser: $status"
                )
            }
        )

    private val scanner =
        BleScanner(
            context = appContext,

            onDeviceFound = {
                    device,
                    rssi ->

                mainHandler.post {

                    handleDeviceFound(
                        device = device,
                        rssi = rssi
                    )
                }
            },

            onStatusChanged = { status ->

                postStatus(
                    "Scanner: $status"
                )
            }
        )

    private val gattClient =
        BleGattClient(
            context = appContext,

            onStatusChanged = { status ->

                postStatus(
                    "Client: $status"
                )
            },

            onMessageSent = {

                mainHandler.post {

                    handleSendSuccess()
                }
            }
        )

    private val gattServer =
        BleGattServer(
            context = appContext,

            onMessageReceived = {
                    device,
                    message ->

                mainHandler.post {

                    handleIncomingMessage(
                        sourceDevice = device,
                        rawMessage = message
                    )
                }
            },

            onStatusChanged = { status ->

                postStatus(
                    "Server: $status"
                )
            },

            onReady = {

                mainHandler.post {

                    postStatus(
                        "GATT ready. Starting advertisement..."
                    )

                    advertiser.startAdvertising()
                }
            }
        )

    /*
     * Phase 1 timeout.
     *
     * No Android phone found → try gateway.
     */
    private val androidScanTimeoutRunnable =
        Runnable {

            if (
                pendingPacket == null ||
                sendingPacket != null ||
                scanPhase !=
                ScanPhase.ANDROID_PREFERRED
            ) {

                return@Runnable
            }

            scanner.stopScanning()

            Log.d(
                TAG,
                "No Android relay. Falling back to gateway."
            )

            postStatus(
                "No Android relay found. Looking for gateway..."
            )

            startFallbackScan()
        }

    /*
     * Phase 2 timeout.
     *
     * Keep packet so Retry still works.
     */
    private val fallbackScanTimeoutRunnable =
        Runnable {

            if (
                pendingPacket == null ||
                sendingPacket != null ||
                scanPhase !=
                ScanPhase.ANY_RESCUEMESH
            ) {

                return@Runnable
            }

            scanner.stopScanning()

            scanPhase =
                null

            postStatus(
                "No next relay found. Packet kept for retry."
            )

            Log.d(
                TAG,
                "No destination. Packet retained."
            )
        }

    fun start() {

        if (started) {
            return
        }

        started =
            true

        postStatus(
            "Starting RESCUEMESH node..."
        )

        gattServer.startServer()
    }

    fun stop() {

        cancelScanTimers()

        scanner.stopScanning()

        advertiser.stopAdvertising()

        gattClient.close()

        gattServer.stopServer()

        pendingPacket =
            null

        sendingPacket =
            null

        excludedAddress =
            null

        sendingAddress =
            null

        scanPhase =
            null

        started =
            false

        postStatus(
            "RESCUEMESH stopped"
        )
    }

    fun originateSos(
        latitude: Double,
        longitude: Double
    ): RescuePacket {

        val packet =
            RescuePacket.createSos(
                latitude = latitude,
                longitude = longitude
            )

        /*
         * If the packet eventually loops back,
         * the origin ignores it.
         */
        relayManager.markSeen(
            packet.id
        )

        Log.d(
            TAG,
            "Created SOS ${packet.id}"
        )

        postStatus(
            "SOS created. Looking for Android relay first..."
        )

        beginForward(
            packet = packet,
            sourceAddress = null
        )

        return packet
    }

    fun retryPendingForward() {

        val packet =
            pendingPacket

        if (packet == null) {

            postStatus(
                "No pending packet to retry"
            )

            return
        }

        gattClient.close()

        scanner.stopScanning()

        cancelScanTimers()

        sendingPacket =
            null

        sendingAddress =
            null

        scanPhase =
            null

        postStatus(
            "Retrying packet ${packet.id.take(8)} at hop ${packet.hopCount}..."
        )

        startPreferredScan()
    }

    private fun handleIncomingMessage(
        sourceDevice: BluetoothDevice,
        rawMessage: String
    ) {

        val sourceAddress =
            safeAddress(
                sourceDevice
            )

        val packet =
            RescuePacket.fromJson(
                rawMessage
            )

        if (packet == null) {

            postStatus(
                "Ignored invalid RESCUEMESH packet"
            )

            return
        }

        if (
            packet.type !=
            RescuePacket.TYPE_SOS
        ) {

            postStatus(
                "Ignored unsupported packet type: ${packet.type}"
            )

            return
        }

        if (
            !relayManager.markSeen(
                packet.id
            )
        ) {

            postStatus(
                "Duplicate SOS ${packet.id.take(8)} ignored"
            )

            return
        }

        onPacketReceived(
            packet
        )

        Log.d(
            TAG,
            "Received packet=${packet.id} " +
                    "hop=${packet.hopCount} " +
                    "source=$sourceAddress"
        )

        postStatus(
            "Received SOS ${packet.id.take(8)} at hop ${packet.hopCount}"
        )

        if (!packet.canRelay()) {

            postStatus(
                "TTL reached. Packet will not be forwarded."
            )

            return
        }

        /*
         * Increment exactly once.
         *
         * Retry does not increment again.
         */
        val nextPacket =
            packet.nextHop()

        postStatus(
            "Preparing hop ${nextPacket.hopCount}/${nextPacket.ttl}"
        )

        beginForward(
            packet = nextPacket,
            sourceAddress = sourceAddress
        )
    }

    private fun beginForward(
        packet: RescuePacket,
        sourceAddress: String?
    ) {

        cancelScanTimers()

        scanner.stopScanning()

        pendingPacket =
            packet

        sendingPacket =
            null

        sendingAddress =
            null

        excludedAddress =
            sourceAddress

        scanPhase =
            null

        Log.d(
            TAG,
            "beginForward packet=${packet.id} " +
                    "hop=${packet.hopCount}"
        )

        startPreferredScan()
    }

    /*
     * PHASE 1
     *
     * Prefer Android.
     */
    private fun startPreferredScan() {

        val packet =
            pendingPacket
                ?: return

        if (
            sendingPacket != null
        ) {
            return
        }

        cancelScanTimers()

        scanner.stopScanning()

        scanPhase =
            ScanPhase.ANDROID_PREFERRED

        postStatus(
            "Looking for nearby Android relay..."
        )

        scanner.startScanning(
            BleScanner.ScanTarget.ANDROID_ONLY
        )

        mainHandler.postDelayed(
            androidScanTimeoutRunnable,
            ANDROID_SCAN_MS
        )

        Log.d(
            TAG,
            "Android scan packet=${packet.id}"
        )
    }

    /*
     * PHASE 2
     *
     * Accept Windows gateway.
     */
    private fun startFallbackScan() {

        val packet =
            pendingPacket
                ?: return

        if (
            sendingPacket != null
        ) {
            return
        }

        cancelScanTimers()

        scanner.stopScanning()

        scanPhase =
            ScanPhase.ANY_RESCUEMESH

        postStatus(
            "Looking for RESCUEMESH gateway..."
        )

        scanner.startScanning(
            BleScanner.ScanTarget.ANY_RESCUEMESH
        )

        mainHandler.postDelayed(
            fallbackScanTimeoutRunnable,
            FALLBACK_SCAN_MS
        )

        Log.d(
            TAG,
            "Gateway scan packet=${packet.id}"
        )
    }

    private fun handleDeviceFound(
        device: BluetoothDevice,
        rssi: Int
    ) {

        val packet =
            pendingPacket
                ?: return

        /*
         * Avoid multiple simultaneous connections
         * from queued scan callbacks.
         */
        if (
            sendingPacket != null
        ) {

            return
        }

        val candidateAddress =
            safeAddress(
                device
            )

        Log.d(
            TAG,
            "Candidate=$candidateAddress " +
                    "phase=$scanPhase " +
                    "RSSI=$rssi"
        )

        /*
         * Extra safeguard.
         *
         * BLE addresses can rotate, so our primary
         * loop protection is still hiding after send.
         */
        if (
            excludedAddress != null &&
            candidateAddress.equals(
                excludedAddress,
                ignoreCase = true
            )
        ) {

            postStatus(
                "Ignoring previous hop"
            )

            return
        }

        cancelScanTimers()

        scanner.stopScanning()

        sendingPacket =
            packet

        sendingAddress =
            candidateAddress

        val destinationType =
            when (scanPhase) {

                ScanPhase.ANDROID_PREFERRED ->
                    "Android relay"

                ScanPhase.ANY_RESCUEMESH ->
                    "RESCUEMESH gateway"

                null ->
                    "RESCUEMESH node"
            }

        scanPhase =
            null

        postStatus(
            "$destinationType found. Sending hop ${packet.hopCount}..."
        )

        Log.d(
            TAG,
            "Sending to $candidateAddress"
        )

        /*
         * Current hackathon loop prevention.
         *
         * Once this node forwards, make it invisible.
         */
        pauseAdvertisingAfterForward()

        gattClient.connectAndSend(
            device = device,
            message = packet.toJson()
        )
    }

    private fun handleSendSuccess() {

        val packet =
            sendingPacket
                ?: return

        val destination =
            sendingAddress
                ?: "(unknown)"

        Log.d(
            TAG,
            "SUCCESS packet=${packet.id} " +
                    "hop=${packet.hopCount} " +
                    "destination=$destination"
        )

        /*
         * Packet only disappears after actual
         * successful characteristic write.
         */
        pendingPacket =
            null

        sendingPacket =
            null

        sendingAddress =
            null

        excludedAddress =
            null

        scanPhase =
            null

        cancelScanTimers()

        onPacketSent(
            packet
        )

        postStatus(
            "Packet ${packet.id.take(8)} sent successfully at hop ${packet.hopCount}"
        )
    }

    private fun pauseAdvertisingAfterForward() {

        Log.d(
            TAG,
            "Stopping advertisement after forwarding"
        )

        postStatus(
            "Forwarding SOS. Hiding this node from further relay scans."
        )

        advertiser.stopAdvertising()
    }

    private fun cancelScanTimers() {

        mainHandler.removeCallbacks(
            androidScanTimeoutRunnable
        )

        mainHandler.removeCallbacks(
            fallbackScanTimeoutRunnable
        )
    }

    @SuppressLint("MissingPermission")
    private fun safeAddress(
        device: BluetoothDevice
    ): String {

        return try {

            device.address
                ?: "(unknown)"

        } catch (_: SecurityException) {

            "(permission denied)"
        }
    }

    private fun postStatus(
        status: String
    ) {

        mainHandler.post {

            onStatusChanged(
                status
            )
        }
    }
}