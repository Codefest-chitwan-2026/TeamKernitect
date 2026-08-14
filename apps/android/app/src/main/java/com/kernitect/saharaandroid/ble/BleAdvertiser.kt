package com.kernitect.saharaandroid.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid

class BleAdvertiser (
    private val context: Context,
    private val onStatusChanged: (String) -> Unit
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter
        get() = bluetoothManager.adapter

    private var advertising = false

    private val advertiseCallback =
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                advertising = true
                onStatusChanged("Advertising RESCUEMESH service")
            }

            override fun onStartFailure(errorCode: Int) {
                advertising = false

                val message = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE ->
                        "Advertising failed: data too large"

                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                        "Advertising failed: too many advertisers"

                    ADVERTISE_FAILED_ALREADY_STARTED ->
                        "Advertising already started"

                    ADVERTISE_FAILED_INTERNAL_ERROR ->
                        "Advertising failed: internal error"

                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                        "Advertising failed: unsupported"

                    else ->
                        "Advertising failed: error $errorCode"
                }

                onStatusChanged(message)
            }
        }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (!AppRequirements.hasAllRuntimePermissions(context)) {
            onStatusChanged("Advertising failed: missing permissions")
            return
        }

        if (!AppRequirements.isBluetoothEnabled(context)) {
            onStatusChanged("Advertising failed: Bluetooth is off")
            return
        }

        if (advertising) {
            onStatusChanged("Already advertising")
            return
        }

        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        if (advertiser == null) {
            onStatusChanged(
                "Advertising failed: BLE advertising unavailable"
            )
            return
        }

        val settings =
            AdvertiseSettings.Builder()
                .setAdvertiseMode(
                    AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                ).setTxPowerLevel(
                    AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0).build()

        val advertiseData =
            AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
                .setIncludeDeviceName(false).build()

        advertiser.startAdvertising(
            settings,
            advertiseData,
            advertiseCallback
        )

        onStatusChanged("Starting Advertisement...")
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        val advertiser =
            bluetoothAdapter?.bluetoothLeAdvertiser?: return

        advertiser.stopAdvertising(advertiseCallback)

        advertising = false

        onStatusChanged("Advertising stopped")
    }
}