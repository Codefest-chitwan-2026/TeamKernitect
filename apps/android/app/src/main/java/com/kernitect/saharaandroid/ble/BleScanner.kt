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

class BleScanner(
    private val context: Context,
    private val onDeviceFound: (
        device: BluetoothDevice,
        rssi: Int
    ) -> Unit,
    private val onStatusChanged: (String) -> Unit
) {

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val bluetoothAdapter
        get() = bluetoothManager.adapter

    private var scanning = false

    private val scanCallback =
        object : ScanCallback() {

            @SuppressLint("MissingPermission")
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {

                super.onScanResult(
                    callbackType,
                    result
                )

                onDeviceFound(
                    result.device,
                    result.rssi
                )
            }

            @SuppressLint("MissingPermission")
            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {

                super.onBatchScanResults(
                    results
                )

                results.forEach { result ->

                    onDeviceFound(
                        result.device,
                        result.rssi
                    )
                }
            }

            override fun onScanFailed(
                errorCode: Int
            ) {

                scanning = false

                val message =
                    when (errorCode) {

                        SCAN_FAILED_ALREADY_STARTED ->
                            "Scan already started"

                        SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
                            "Scan failed: registration error"

                        SCAN_FAILED_INTERNAL_ERROR ->
                            "Scan failed: internal error"

                        SCAN_FAILED_FEATURE_UNSUPPORTED ->
                            "Scan failed: unsupported"

                        else ->
                            "Scan failed: error $errorCode"
                    }

                onStatusChanged(message)
            }
        }

    @SuppressLint("MissingPermission")
    fun startScanning() {

        if (
            !AppRequirements
                .hasAllRuntimePermissions(context)
        ) {
            onStatusChanged(
                "Scan failed: missing permissions"
            )
            return
        }

        if (
            !AppRequirements
                .isBluetoothEnabled(context)
        ) {
            onStatusChanged(
                "Scan failed: Bluetooth is off"
            )
            return
        }

        if (scanning) {
            onStatusChanged(
                "Already scanning"
            )
            return
        }

        val scanner =
            bluetoothAdapter
                ?.bluetoothLeScanner

        if (scanner == null) {
            onStatusChanged(
                "Scan failed: BLE scanner unavailable"
            )
            return
        }

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
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .build()

        scanning = true

        scanner.startScan(
            listOf(filter),
            settings,
            scanCallback
        )

        onStatusChanged(
            "Scanning for RESCUEMESH devices..."
        )
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {

        bluetoothAdapter
            ?.bluetoothLeScanner
            ?.stopScan(scanCallback)

        scanning = false

        onStatusChanged(
            "Scanning stopped"
        )
    }
}