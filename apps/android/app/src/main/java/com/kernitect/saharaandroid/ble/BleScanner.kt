package com.kernitect.saharaandroid.ble

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log

class BleScanner(
    context: Context,

    private val onDeviceFound:
        (
        device: BluetoothDevice,
        rssi: Int
    ) -> Unit,

    private val onStatusChanged:
        (String) -> Unit
) {

    companion object {

        private const val TAG =
            "SAHARA_BLE"

        private const val SCAN_REQUEST_CODE =
            7101
    }


    enum class ScanTarget {

        ANDROID_ONLY,

        ANY_RESCUEMESH
    }


    data class CachedCandidate(

        val device: BluetoothDevice,

        val rssi: Int
    )


    private val appContext =
        context.applicationContext


    private val bluetoothManager =
        appContext.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager


    private val bluetoothAdapter
        get() =
            bluetoothManager.adapter


    private var scanning =
        false


    private var currentTarget:
            ScanTarget? = null


    /*
     * Windows/non-Android RESCUEMESH node seen
     * while we briefly prefer another phone.
     */
    private var cachedFallbackCandidate:
            CachedCandidate? = null


    /*
     * =========================================
     * PENDING INTENT
     * =========================================
     *
     * Android's Bluetooth service will send BLE
     * results here even when Sahara Activity is
     * not visible.
     */
    private val scanPendingIntent:
            PendingIntent by lazy {

        val intent =
            Intent(
                appContext,
                BleScanReceiver::class.java
            )


        /*
         * Bluetooth needs to attach ScanResult
         * extras to the Intent.
         *
         * From Android 12 onward PendingIntent
         * mutability must be explicit.
         */
        val flags =

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_MUTABLE

            } else {

                PendingIntent.FLAG_UPDATE_CURRENT
            }


        PendingIntent.getBroadcast(

            appContext,

            SCAN_REQUEST_CODE,

            intent,

            flags
        )
    }


    init {

        /*
         * Receive results from BleScanReceiver.
         */
        BleScanBus.listener = {
                results,
                errorCode ->


            if (
                errorCode != null
            ) {

                scanning =
                    false


                currentTarget =
                    null


                Log.e(
                    TAG,
                    "BLE PendingIntent scan error=$errorCode"
                )


                onStatusChanged(
                    "BLE scan failed: $errorCode"
                )


            } else {

                results.forEach {
                        result ->

                    inspectResult(
                        result
                    )
                }
            }
        }
    }


    /*
     * =========================================
     * PROCESS RESULT
     * =========================================
     */

    @SuppressLint(
        "MissingPermission"
    )
    private fun inspectResult(
        result: ScanResult
    ) {

        val target =
            currentTarget
                ?: return


        val serviceUuids =
            result.scanRecord
                ?.serviceUuids
                ?.map {
                        parcelUuid ->

                    parcelUuid.uuid
                }
                ?: emptyList()


        val isAndroidNode =

            serviceUuids.contains(
                BleConstants.ANDROID_NODE_UUID
            )


        Log.d(
            TAG,
            "Scan address=${result.device.address} " +
                    "rssi=${result.rssi} " +
                    "androidNode=$isAndroidNode " +
                    "target=$target"
        )


        /*
         * =====================================
         * PHASE 1 — PREFER ANDROID
         * =====================================
         */
        if (
            target ==
            ScanTarget.ANDROID_ONLY
        ) {

            /*
             * Android relay?
             *
             * Use it immediately.
             */
            if (
                isAndroidNode
            ) {

                Log.d(
                    TAG,
                    "Android relay accepted"
                )


                onDeviceFound(

                    result.device,

                    result.rssi
                )


                return
            }


            /*
             * Otherwise this can be the Windows
             * gateway.
             *
             * Don't discard it.
             *
             * Remember the strongest gateway while
             * MeshEngine waits its 3-second Android
             * preference period.
             */
            val existing =
                cachedFallbackCandidate


            if (
                existing == null ||
                result.rssi >
                existing.rssi
            ) {

                cachedFallbackCandidate =

                    CachedCandidate(

                        device =
                            result.device,

                        rssi =
                            result.rssi
                    )


                Log.d(
                    TAG,
                    "Cached gateway candidate " +
                            "address=${result.device.address} " +
                            "rssi=${result.rssi}"
                )


                onStatusChanged(
                    "Gateway detected. Waiting briefly for Android relay..."
                )
            }


            return
        }


        /*
         * =====================================
         * PHASE 2 — ANY RESCUEMESH NODE
         * =====================================
         */

        Log.d(
            TAG,
            "RESCUEMESH gateway accepted"
        )


        onDeviceFound(

            result.device,

            result.rssi
        )
    }


    /*
     * =========================================
     * START SCAN
     * =========================================
     */

    @SuppressLint(
        "MissingPermission"
    )
    fun startScanning(
        target: ScanTarget
    ) {

        if (
            !AppRequirements
                .hasMeshPermissions(
                    appContext
                )
        ) {

            onStatusChanged(
                "Scan failed: missing Nearby Devices permission"
            )


            return
        }


        if (
            !AppRequirements
                .isBluetoothEnabled(
                    appContext
                )
        ) {

            onStatusChanged(
                "Scan failed: Bluetooth is off"
            )


            return
        }


        if (
            scanning
        ) {

            stopScanning()
        }


        val scanner =
            bluetoothAdapter
                ?.bluetoothLeScanner


        if (
            scanner == null
        ) {

            onStatusChanged(
                "BLE scanner unavailable"
            )


            return
        }


        /*
         * IMPORTANT:
         *
         * Keep this FILTERED scan.
         *
         * Both Android nodes and Windows gateway
         * advertise SERVICE_UUID.
         */
        val filter =

            ScanFilter
                .Builder()
                .setServiceUuid(

                    ParcelUuid(
                        BleConstants.SERVICE_UUID
                    )
                )
                .build()


        val settings =

            ScanSettings
                .Builder()
                .setScanMode(
                    ScanSettings
                        .SCAN_MODE_LOW_LATENCY
                )
                .setCallbackType(
                    ScanSettings
                        .CALLBACK_TYPE_ALL_MATCHES
                )
                .setMatchMode(
                    ScanSettings
                        .MATCH_MODE_AGGRESSIVE
                )
                .setNumOfMatches(
                    ScanSettings
                        .MATCH_NUM_MAX_ADVERTISEMENT
                )
                .setReportDelay(
                    0L
                )
                .build()


        /*
         * New packet = forget old fallback.
         */
        if (
            target ==
            ScanTarget.ANDROID_ONLY
        ) {

            cachedFallbackCandidate =
                null
        }


        currentTarget =
            target


        scanning =
            true


        try {

            /*
             * =================================
             * IMPORTANT CHANGE
             * =================================
             *
             * OLD:
             *
             * scanner.startScan(..., ScanCallback)
             *
             * NEW:
             *
             * scanner.startScan(..., PendingIntent)
             *
             * This uses Android's system-delivered
             * background BLE discovery mechanism.
             */
            val result =

                scanner.startScan(

                    listOf(
                        filter
                    ),

                    settings,

                    scanPendingIntent
                )


            if (
                result != 0
            ) {

                scanning =
                    false


                currentTarget =
                    null


                Log.e(
                    TAG,
                    "Could not start BLE PendingIntent scan: $result"
                )


                onStatusChanged(
                    "BLE scan failed to start: $result"
                )


                return
            }


            Log.d(
                TAG,
                "BLE PendingIntent scan started target=$target"
            )


        } catch (
            error: SecurityException
        ) {

            scanning =
                false


            currentTarget =
                null


            Log.e(
                TAG,
                "BLE scan permission error",
                error
            )


            onStatusChanged(
                "BLE scan permission error"
            )


            return
        }


        when (
            target
        ) {

            ScanTarget.ANDROID_ONLY -> {

                onStatusChanged(
                    "Scanning for Android relay nodes..."
                )
            }


            ScanTarget.ANY_RESCUEMESH -> {

                onStatusChanged(
                    "Scanning for RESCUEMESH gateway..."
                )
            }
        }
    }


    /*
     * =========================================
     * CACHED FALLBACK GATEWAY
     * =========================================
     */

    fun takeFallbackCandidate():
            CachedCandidate? {

        val candidate =
            cachedFallbackCandidate


        cachedFallbackCandidate =
            null


        if (
            candidate != null
        ) {

            Log.d(
                TAG,
                "Using cached gateway candidate " +
                        "rssi=${candidate.rssi}"
            )
        }


        return candidate
    }


    /*
     * =========================================
     * STOP SCAN
     * =========================================
     */

    @SuppressLint(
        "MissingPermission"
    )
    fun stopScanning() {

        if (
            !scanning
        ) {

            return
        }


        try {

            bluetoothAdapter
                ?.bluetoothLeScanner
                ?.stopScan(
                    scanPendingIntent
                )

        } catch (
            error: SecurityException
        ) {

            Log.e(
                TAG,
                "BLE stopScan permission error",
                error
            )
        }


        scanning =
            false


        currentTarget =
            null


        Log.d(
            TAG,
            "BLE PendingIntent scan stopped"
        )
    }
}