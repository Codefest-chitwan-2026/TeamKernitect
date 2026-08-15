package com.kernitect.sahararesponder.ble

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

class ResponderBleScanner(
    context: Context,
    private val onDeviceFound: (BluetoothDevice) -> Unit,
    private val onFailure: (String) -> Unit,
) {
    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanning = false
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = inspect(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::inspect)
        override fun onScanFailed(errorCode: Int) {
            scanning = false
            onFailure("BLE scan failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun inspect(result: ScanResult) {
        if (!scanning) return
        val services = result.scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()
        if (!services.contains(BleConstants.ANDROID_NODE_UUID)) return
        scanning = false
        stopInternal()
        Log.i(TAG, "Relay found: ${safeAddress(result.device)}")
        onDeviceFound(result.device)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        stop()
        val scanner = adapter?.bluetoothLeScanner ?: run {
            onFailure("BLE scanner unavailable")
            return
        }
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanning = true
            scanner.startScan(listOf(filter), settings, callback)
        } catch (_: SecurityException) {
            scanning = false
            onFailure("Bluetooth scan permission required")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!scanning) return
        scanning = false
        stopInternal()
    }

    @SuppressLint("MissingPermission")
    private fun stopInternal() {
        try { adapter?.bluetoothLeScanner?.stopScan(callback) } catch (_: SecurityException) { }
    }

    @SuppressLint("MissingPermission")
    private fun safeAddress(device: BluetoothDevice) = try { device.address ?: "unknown" } catch (_: SecurityException) { "permission denied" }

    private companion object { const val TAG = "SaharaResponder" }
}
