package com.kernitect.saharaandroid.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

class BleScanner(
    private val context: Context,

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
    }

    enum class ScanTarget {

        ANDROID_ONLY,

        ANY_RESCUEMESH
    }

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val bluetoothAdapter
        get() =
            bluetoothManager.adapter

    private var scanning =
        false

    private var currentTarget:
            ScanTarget? = null

    private val scanCallback =
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {

                inspectResult(
                    result
                )
            }

            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {

                results.forEach {

                    inspectResult(
                        it
                    )
                }
            }

            override fun onScanFailed(
                errorCode: Int
            ) {

                scanning =
                    false

                currentTarget =
                    null

                Log.e(
                    TAG,
                    "BLE scan failed: $errorCode"
                )

                onStatusChanged(
                    "BLE scan failed: $errorCode"
                )
            }
        }

    @SuppressLint("MissingPermission")
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
                    it.uuid
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
         * During phase 1 Windows is ignored.
         */
        if (
            target ==
            ScanTarget.ANDROID_ONLY &&
            !isAndroidNode
        ) {

            return
        }

        onDeviceFound(
            result.device,
            result.rssi
        )
    }

    @SuppressLint("MissingPermission")
    fun startScanning(
        target: ScanTarget
    ) {

        if (
            !AppRequirements
                .hasAllRuntimePermissions(
                    context
                )
        ) {

            onStatusChanged(
                "Scan failed: missing permissions"
            )

            return
        }

        if (
            !AppRequirements
                .isBluetoothEnabled(
                    context
                )
        ) {

            onStatusChanged(
                "Scan failed: Bluetooth is off"
            )

            return
        }

        if (scanning) {

            stopScanning()
        }

        val scanner =
            bluetoothAdapter
                ?.bluetoothLeScanner

        if (scanner == null) {

            onStatusChanged(
                "BLE scanner unavailable"
            )

            return
        }

        /*
         * Both Android and Windows expose SERVICE_UUID.
         */
        val filter =
            ScanFilter.Builder()
                .setServiceUuid(
                    ParcelUuid(
                        BleConstants.SERVICE_UUID
                    )
                )
                .build()

        val settings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings
                        .SCAN_MODE_LOW_LATENCY
                )
                .build()

        currentTarget =
            target

        scanning =
            true

        try {

            scanner.startScan(
                listOf(filter),
                settings,
                scanCallback
            )

        } catch (_: SecurityException) {

            scanning =
                false

            currentTarget =
                null

            onStatusChanged(
                "BLE scan permission error"
            )

            return
        }

        when (target) {

            ScanTarget.ANDROID_ONLY ->

                onStatusChanged(
                    "Scanning for Android relay nodes..."
                )

            ScanTarget.ANY_RESCUEMESH ->

                onStatusChanged(
                    "Scanning for RESCUEMESH gateway..."
                )
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {

        if (!scanning) {
            return
        }

        try {

            bluetoothAdapter
                ?.bluetoothLeScanner
                ?.stopScan(
                    scanCallback
                )

        } catch (_: SecurityException) {

            // Cleanup only.
        }

        scanning =
            false

        currentTarget =
            null

        Log.d(
            TAG,
            "BLE scan stopped"
        )
    }
}