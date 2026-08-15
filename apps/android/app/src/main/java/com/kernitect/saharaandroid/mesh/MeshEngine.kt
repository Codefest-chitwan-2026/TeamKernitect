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


        /*
         * Prefer a phone relay briefly.
         */
        private const val ANDROID_SCAN_MS =
            3_000L


        /*
         * Background discovery can take longer,
         * so give the gateway substantially more
         * time than the old 12-second window.
         */
        private const val FALLBACK_SCAN_MS =
            30_000L


        /*
         * Become available again shortly after
         * forwarding a packet.
         */
        private const val READVERTISE_DELAY_MS =
            2_500L
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


    private var pendingPacket:
            RescuePacket? = null


    private var sendingPacket:
            RescuePacket? = null


    private var excludedAddress:
            String? = null


    private var sendingAddress:
            String? = null


    private var scanPhase:
            ScanPhase? = null


    /*
     * =========================================
     * BLE ADVERTISER
     * =========================================
     */

    private val advertiser =
        BleAdvertiser(

            context =
                appContext,

            onStatusChanged = {
                    status ->

                postStatus(
                    "Advertiser: $status"
                )
            }
        )


    /*
     * =========================================
     * BLE SCANNER
     * =========================================
     */

    private val scanner =
        BleScanner(

            context =
                appContext,

            onDeviceFound = {
                    device,
                    rssi ->


                mainHandler.post {

                    handleDeviceFound(

                        device =
                            device,

                        rssi =
                            rssi
                    )
                }
            },

            onStatusChanged = {
                    status ->

                postStatus(
                    "Scanner: $status"
                )
            }
        )


    /*
     * =========================================
     * GATT CLIENT
     * =========================================
     */

    private val gattClient =
        BleGattClient(

            context =
                appContext,

            onStatusChanged = {
                    status ->

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


    /*
     * =========================================
     * GATT SERVER
     * =========================================
     */

    private val gattServer =
        BleGattServer(

            context =
                appContext,

            onMessageReceived = {
                    device,
                    message ->


                mainHandler.post {

                    handleIncomingMessage(

                        sourceDevice =
                            device,

                        rawMessage =
                            message
                    )
                }
            },

            onStatusChanged = {
                    status ->

                postStatus(
                    "Server: $status"
                )
            },

            onReady = {

                mainHandler.post {

                    postStatus(
                        "GATT ready. Starting advertisement..."
                    )


                    advertiser
                        .startAdvertising()
                }
            }
        )


    /*
     * =========================================
     * ANDROID PREFERENCE TIMEOUT
     * =========================================
     *
     * Critical improvement:
     *
     * While looking for Android, BleScanner also
     * remembers a Windows/non-Android gateway.
     *
     * After 3 seconds:
     *
     * 1. use remembered gateway immediately, or
     * 2. perform a longer fallback scan.
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


            /*
             * Grab a gateway that may already
             * have been seen during phase 1.
             */
            val rememberedGateway =
                scanner
                    .takeFallbackCandidate()


            scanner
                .stopScanning()


            if (
                rememberedGateway != null
            ) {

                Log.d(
                    TAG,
                    "No Android relay. Using cached gateway."
                )


                postStatus(
                    "No Android relay found. Using detected gateway..."
                )


                /*
                 * Make handleDeviceFound treat it
                 * as the fallback/gateway phase.
                 */
                scanPhase =
                    ScanPhase.ANY_RESCUEMESH


                handleDeviceFound(

                    device =
                        rememberedGateway.device,

                    rssi =
                        rememberedGateway.rssi
                )


                return@Runnable
            }


            Log.d(
                TAG,
                "No Android relay or cached gateway. Starting fallback scan."
            )


            postStatus(
                "No Android relay found. Looking for gateway..."
            )


            startFallbackScan()
        }


    /*
     * =========================================
     * FALLBACK TIMEOUT
     * =========================================
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


            scanner
                .stopScanning()


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


    /*
     * =========================================
     * RE-ADVERTISEMENT
     * =========================================
     */

    private val readvertiseRunnable =
        Runnable {

            if (
                started &&
                pendingPacket == null &&
                sendingPacket == null
            ) {

                Log.d(
                    TAG,
                    "Resuming RESCUEMESH advertisement"
                )


                postStatus(
                    "Relay ready. Listening for emergency packets..."
                )


                advertiser
                    .startAdvertising()
            }
        }


    /*
     * =========================================
     * START
     * =========================================
     */

    fun start() {

        if (
            started
        ) {

            return
        }


        started =
            true


        postStatus(
            "Starting RESCUEMESH node..."
        )


        gattServer
            .startServer()
    }


    /*
     * =========================================
     * STOP
     * =========================================
     */

    fun stop() {

        cancelScanTimers()


        mainHandler.removeCallbacks(
            readvertiseRunnable
        )


        scanner
            .stopScanning()


        advertiser
            .stopAdvertising()


        gattClient
            .close()


        gattServer
            .stopServer()


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


    /*
     * =========================================
     * CRITICAL SOS
     * =========================================
     */

    fun originateSos(

        latitude: Double,

        longitude: Double,

        likelyDisaster: String =
            RescuePacket.DISASTER_UNKNOWN,

        areaSeverity: String =
            RescuePacket.SEVERITY_UNKNOWN

    ): RescuePacket {


        val packet =
            RescuePacket
                .createCriticalSos(

                    latitude =
                        latitude,

                    longitude =
                        longitude,

                    likelyDisaster =
                        likelyDisaster,

                    areaSeverity =
                        areaSeverity
                )


        relayManager
            .markSeen(
                packet.id
            )


        Log.d(
            TAG,
            "Created CRITICAL SOS ${packet.id}"
        )


        postStatus(
            "Critical SOS created. Looking for Android relay first..."
        )


        beginForward(

            packet =
                packet,

            sourceAddress =
                null
        )


        return packet
    }


    /*
     * =========================================
     * HELP REQUEST
     * =========================================
     */

    fun originateHelpRequest(

        latitude: Double,

        longitude: Double,

        disasterType: String,

        peopleCount: String,

        explanation: String

    ): RescuePacket {


        val packet =
            RescuePacket
                .createHelpRequest(

                    latitude =
                        latitude,

                    longitude =
                        longitude,

                    disasterType =
                        disasterType,

                    peopleCount =
                        peopleCount,

                    explanation =
                        explanation
                )


        relayManager
            .markSeen(
                packet.id
            )


        Log.d(
            TAG,
            "Created NORMAL help request ${packet.id}"
        )


        postStatus(
            "Help request created. Looking for Android relay first..."
        )


        beginForward(

            packet =
                packet,

            sourceAddress =
                null
        )


        return packet
    }


    /*
     * =========================================
     * RETRY
     * =========================================
     */

    fun retryPendingForward() {

        val packet =
            pendingPacket


        if (
            packet == null
        ) {

            postStatus(
                "No pending packet to retry"
            )


            return
        }


        mainHandler.removeCallbacks(
            readvertiseRunnable
        )


        gattClient
            .close()


        scanner
            .stopScanning()


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


    /*
     * =========================================
     * CANCEL
     * =========================================
     */

    fun cancelPendingForward() {

        cancelScanTimers()


        mainHandler.removeCallbacks(
            readvertiseRunnable
        )


        scanner
            .stopScanning()


        gattClient
            .close()


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


        if (
            started
        ) {

            advertiser
                .startAdvertising()
        }


        postStatus(
            "Pending request cancelled"
        )
    }


    /*
     * =========================================
     * PACKET RECEIVED
     * =========================================
     */

    private fun handleIncomingMessage(

        sourceDevice: BluetoothDevice,

        rawMessage: String

    ) {

        val sourceAddress =
            safeAddress(
                sourceDevice
            )


        val packet =
            RescuePacket
                .fromJson(
                    rawMessage
                )


        if (
            packet == null
        ) {

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
            !relayManager
                .markSeen(
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
                    "priority=${packet.priority} " +
                    "hop=${packet.hopCount} " +
                    "source=$sourceAddress"
        )


        postStatus(
            "Received ${packet.priority} request ${packet.id.take(8)} " +
                    "at hop ${packet.hopCount}"
        )


        if (
            !packet.canRelay()
        ) {

            postStatus(
                "TTL reached. Packet will not be forwarded."
            )


            return
        }


        val nextPacket =
            packet.nextHop()


        postStatus(
            "Preparing hop ${nextPacket.hopCount}/${nextPacket.ttl}"
        )


        beginForward(

            packet =
                nextPacket,

            sourceAddress =
                sourceAddress
        )
    }


    /*
     * =========================================
     * BEGIN FORWARD
     * =========================================
     */

    private fun beginForward(

        packet: RescuePacket,

        sourceAddress: String?

    ) {

        mainHandler.removeCallbacks(
            readvertiseRunnable
        )


        cancelScanTimers()


        scanner
            .stopScanning()


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
     * =========================================
     * ANDROID-PREFERRED SCAN
     * =========================================
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


        scanner
            .stopScanning()


        scanPhase =
            ScanPhase.ANDROID_PREFERRED


        postStatus(
            "Looking for nearby Android relay..."
        )


        scanner
            .startScanning(
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
     * =========================================
     * FALLBACK SCAN
     * =========================================
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


        scanner
            .stopScanning()


        scanPhase =
            ScanPhase.ANY_RESCUEMESH


        postStatus(
            "Looking for RESCUEMESH gateway..."
        )


        scanner
            .startScanning(
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


    /*
     * =========================================
     * DEVICE FOUND
     * =========================================
     */

    private fun handleDeviceFound(

        device: BluetoothDevice,

        rssi: Int

    ) {

        val packet =
            pendingPacket
                ?: return


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
         * Never directly return the packet to
         * the previous BLE hop.
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


        scanner
            .stopScanning()


        sendingPacket =
            packet


        sendingAddress =
            candidateAddress


        val destinationType =

            when (
                scanPhase
            ) {

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


        pauseAdvertisingAfterForward()


        gattClient
            .connectAndSend(

                device =
                    device,

                message =
                    packet.toJson()
            )
    }


    /*
     * =========================================
     * SEND SUCCESS
     * =========================================
     */

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
            "${packet.priority} request ${packet.id.take(8)} " +
                    "sent successfully at hop ${packet.hopCount}"
        )


        /*
         * Become a relay again after this
         * forwarding operation.
         */
        mainHandler.removeCallbacks(
            readvertiseRunnable
        )


        mainHandler.postDelayed(

            readvertiseRunnable,

            READVERTISE_DELAY_MS
        )
    }


    /*
     * =========================================
     * PAUSE ADVERTISING WHILE SENDING
     * =========================================
     */

    private fun pauseAdvertisingAfterForward() {

        Log.d(
            TAG,
            "Temporarily stopping advertisement while forwarding"
        )


        postStatus(
            "Forwarding request. Temporarily hiding this relay."
        )


        advertiser
            .stopAdvertising()
    }


    /*
     * =========================================
     * TIMER CLEANUP
     * =========================================
     */

    private fun cancelScanTimers() {

        mainHandler.removeCallbacks(
            androidScanTimeoutRunnable
        )


        mainHandler.removeCallbacks(
            fallbackScanTimeoutRunnable
        )
    }


    /*
     * =========================================
     * ADDRESS
     * =========================================
     */

    @SuppressLint(
        "MissingPermission"
    )
    private fun safeAddress(
        device: BluetoothDevice
    ): String {

        return try {

            device.address
                ?: "(unknown)"

        } catch (
            _: SecurityException
        ) {

            "(permission denied)"
        }
    }


    /*
     * =========================================
     * STATUS
     * =========================================
     */

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