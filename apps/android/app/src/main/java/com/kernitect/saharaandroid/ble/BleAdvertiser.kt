package com.kernitect.saharaandroid.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log

class BleAdvertiser(
    private val context: Context,

    private val onStatusChanged:
        (String) -> Unit
) {

    companion object {

        private const val TAG =
            "SAHARA_BLE"
    }

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val bluetoothAdapter
        get() =
            bluetoothManager.adapter

    private var advertising =
        false

    private val advertiseCallback =
        object : AdvertiseCallback() {

            override fun onStartSuccess(
                settingsInEffect: AdvertiseSettings?
            ) {

                advertising =
                    true

                Log.d(
                    TAG,
                    "Android RESCUEMESH advertisement started"
                )

                onStatusChanged(
                    "Advertising RESCUEMESH Android node"
                )
            }

            override fun onStartFailure(
                errorCode: Int
            ) {

                advertising =
                    false

                val message =
                    when (errorCode) {

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

                Log.e(
                    TAG,
                    message
                )

                onStatusChanged(
                    message
                )
            }
        }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {

        if (
            !AppRequirements
                .hasAllRuntimePermissions(
                    context
                )
        ) {

            onStatusChanged(
                "Advertising failed: missing permissions"
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
                "Advertising failed: Bluetooth is off"
            )

            return
        }

        if (advertising) {

            onStatusChanged(
                "Already advertising"
            )

            return
        }

        val advertiser =
            bluetoothAdapter
                ?.bluetoothLeAdvertiser

        if (advertiser == null) {

            onStatusChanged(
                "BLE advertiser unavailable"
            )

            return
        }

        val settings =
            AdvertiseSettings.Builder()
                .setAdvertiseMode(
                    AdvertiseSettings
                        .ADVERTISE_MODE_LOW_LATENCY
                )
                .setTxPowerLevel(
                    AdvertiseSettings
                        .ADVERTISE_TX_POWER_HIGH
                )
                .setConnectable(
                    true
                )
                .setTimeout(
                    0
                )
                .build()

        /*
         * Main RESCUEMESH service.
         *
         * Android and Windows both advertise this.
         */
        val advertiseData =
            AdvertiseData.Builder()
                .addServiceUuid(
                    ParcelUuid(
                        BleConstants.SERVICE_UUID
                    )
                )
                .setIncludeDeviceName(
                    false
                )
                .setIncludeTxPowerLevel(
                    false
                )
                .build()

        /*
         * Android-only routing marker.
         */
        val scanResponse =
            AdvertiseData.Builder()
                .addServiceUuid(
                    ParcelUuid(
                        BleConstants.ANDROID_NODE_UUID
                    )
                )
                .setIncludeDeviceName(
                    false
                )
                .setIncludeTxPowerLevel(
                    false
                )
                .build()

        try {

            advertiser.startAdvertising(
                settings,
                advertiseData,
                scanResponse,
                advertiseCallback
            )

            onStatusChanged(
                "Starting Android node advertisement..."
            )

        } catch (_: SecurityException) {

            onStatusChanged(
                "Advertising permission error"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {

        val advertiser =
            bluetoothAdapter
                ?.bluetoothLeAdvertiser
                ?: return

        if (!advertising) {
            return
        }

        try {

            advertiser.stopAdvertising(
                advertiseCallback
            )

        } catch (_: SecurityException) {

            onStatusChanged(
                "Could not stop advertisement"
            )

            return
        }

        advertising =
            false

        Log.d(
            TAG,
            "Android RESCUEMESH advertisement stopped"
        )

        onStatusChanged(
            "Advertising stopped"
        )
    }
}