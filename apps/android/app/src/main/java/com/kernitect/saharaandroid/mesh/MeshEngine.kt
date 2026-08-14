package com.kernitect.saharaandroid.mesh

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
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

    private var pendingPacket:
            RescuePacket? = null

    private var sendingPacket:
            RescuePacket? = null

    private var excludedDevice:
            BluetoothDevice? = null

    companion object {

        private const val SCAN_TIMEOUT_MS =
            15_000L
    }

    /*
     * Advertisement is created before the server
     * because the server's onReady callback uses it.
     */
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

                handleDeviceFound(
                    device = device,
                    rssi = rssi
                )
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

                val packet =
                    sendingPacket

                if (packet != null) {

                    mainHandler.post {

                        onPacketSent(
                            packet
                        )
                    }

                    postStatus(
                        "Packet ${packet.id.take(8)} sent at hop ${packet.hopCount}"
                    )
                }

                sendingPacket =
                    null
            }
        )

    private val gattServer =
        BleGattServer(
            context = appContext,

            onMessageReceived = {
                    device,
                    message ->

                handleIncomingMessage(
                    sourceDevice =
                        device,

                    rawMessage =
                        message
                )
            },

            onStatusChanged = { status ->

                postStatus(
                    "Server: $status"
                )
            },

            onReady = {

                postStatus(
                    "GATT ready. Starting advertisement..."
                )

                advertiser.startAdvertising()
            }
        )

    private val scanTimeoutRunnable =
        Runnable {

            if (pendingPacket != null) {

                scanner.stopScanning()

                postStatus(
                    "No next relay found within 15 seconds"
                )
            }
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

        mainHandler.removeCallbacks(
            scanTimeoutRunnable
        )

        scanner.stopScanning()

        advertiser.stopAdvertising()

        gattClient.close()

        gattServer.stopServer()

        pendingPacket =
            null

        sendingPacket =
            null

        excludedDevice =
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
         * Mark our own packet as seen so that if it
         * eventually loops back to us, it is ignored.
         */
        relayManager.markSeen(
            packet.id
        )

        postStatus(
            "SOS created. Looking for first relay..."
        )

        beginForward(
            packet = packet,
            sourceDevice = null
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

        postStatus(
            "Retrying relay scan..."
        )

        startRelayScan()
    }

    private fun handleIncomingMessage(
        sourceDevice: BluetoothDevice,
        rawMessage: String
    ) {

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

        val isNewPacket =
            relayManager.markSeen(
                packet.id
            )

        if (!isNewPacket) {

            postStatus(
                "Duplicate SOS ${packet.id.take(8)} ignored"
            )

            return
        }

        mainHandler.post {

            onPacketReceived(
                packet
            )
        }

        postStatus(
            "Received SOS ${packet.id.take(8)} at hop ${packet.hopCount}"
        )

        if (!packet.canRelay()) {

            postStatus(
                "TTL reached. Packet will not be forwarded."
            )

            return
        }

        val nextPacket =
            packet.nextHop()

        postStatus(
            "Relaying as hop ${nextPacket.hopCount}/${nextPacket.ttl}"
        )

        beginForward(
            packet =
                nextPacket,

            sourceDevice =
                sourceDevice
        )
    }

    private fun beginForward(
        packet: RescuePacket,
        sourceDevice: BluetoothDevice?
    ) {

        /*
         * Replace any previous pending forwarding job.
         */
        mainHandler.removeCallbacks(
            scanTimeoutRunnable
        )

        scanner.stopScanning()

        pendingPacket =
            packet

        excludedDevice =
            sourceDevice

        startRelayScan()
    }

    private fun startRelayScan() {

        if (pendingPacket == null) {
            return
        }

        mainHandler.removeCallbacks(
            scanTimeoutRunnable
        )

        scanner.startScanning()

        postStatus(
            "Scanning for next RESCUEMESH node..."
        )

        mainHandler.postDelayed(
            scanTimeoutRunnable,
            SCAN_TIMEOUT_MS
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
         * Do not immediately send a relayed packet
         * back to the phone that just gave it to us.
         */
        val source =
            excludedDevice

        if (
            source != null &&
            device == source
        ) {

            postStatus(
                "Ignoring previous hop; looking for another node..."
            )

            return
        }

        mainHandler.removeCallbacks(
            scanTimeoutRunnable
        )

        scanner.stopScanning()

        pendingPacket =
            null

        excludedDevice =
            null

        sendingPacket =
            packet

        postStatus(
            "Next relay found ($rssi dBm). Connecting..."
        )

        gattClient.connectAndSend(
            device = device,
            message = packet.toJson()
        )
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