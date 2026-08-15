package com.kernitect.sahararesponder.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

class ResponderBleAdvertiser(
    context: Context,
    private val onStatusChanged: (String) -> Unit,
) {
    private val adapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var advertising = false

    private val callback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            advertising = true
            Log.i(TAG, "BLE advertising started")
            onStatusChanged("Ready for SOS")
        }

        override fun onStartFailure(errorCode: Int) {
            advertising = false
            Log.e(TAG, "BLE advertiser failure: $errorCode")
            onStatusChanged("RESCUEMESH advertising failed ($errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (advertising) return
        val advertiser = adapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "BLE advertiser unavailable")
            onStatusChanged("BLE advertising unavailable")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
        val scanResponse = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BleConstants.ANDROID_NODE_UUID))
            .addManufacturerData(BleConstants.RESPONDER_MANUFACTURER_ID, BleConstants.RESPONDER_ROLE_MARKER)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        try {
            onStatusChanged("Starting RESCUEMESH")
            advertiser.startAdvertising(settings, data, scanResponse, callback)
        } catch (_: SecurityException) {
            Log.e(TAG, "Bluetooth advertise permission missing")
            onStatusChanged("Bluetooth permission required")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!advertising) return
        try {
            adapter?.bluetoothLeAdvertiser?.stopAdvertising(callback)
        } catch (_: SecurityException) {
            Log.w(TAG, "Could not stop advertiser: permission missing")
        }
        advertising = false
    }

    private companion object {
        const val TAG = "SaharaResponder"
    }
}
