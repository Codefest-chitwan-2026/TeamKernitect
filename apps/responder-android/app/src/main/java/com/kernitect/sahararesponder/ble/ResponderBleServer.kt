package com.kernitect.sahararesponder.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

class ResponderBleServer(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit,
    private val onStatusChanged: (String) -> Unit,
    private val onReady: () -> Unit,
) {
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var server: BluetoothGattServer? = null

    private val callback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            if (service.uuid != BleConstants.SERVICE_UUID) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "GATT server started")
                onReady()
            } else {
                Log.e(TAG, "GATT service creation failed: $status")
                onStatusChanged("RESCUEMESH server failed ($status)")
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Citizen device connected")
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (characteristic.uuid != BleConstants.MESSAGE_UUID || preparedWrite || offset != 0) {
                if (responseNeeded) respond(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED)
                return
            }

            val raw = value.toString(Charsets.UTF_8)
            Log.d(TAG, "Raw packet received (${value.size} bytes)")
            onMessageReceived(raw)
            if (responseNeeded) respond(device, requestId, BluetoothGatt.GATT_SUCCESS)
        }

        @Suppress("DEPRECATION")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            val supported = descriptor.uuid == BleConstants.CCCD_UUID && !preparedWrite && offset == 0
            if (supported) descriptor.value = value
            if (responseNeeded) {
                respond(
                    device,
                    requestId,
                    if (supported) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED,
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (server != null) return
        try {
            val opened = manager.openGattServer(context, callback)
            if (opened == null) {
                Log.e(TAG, "Could not open GATT server")
                onStatusChanged("RESCUEMESH server unavailable")
                return
            }
            server = opened
            val service = BluetoothGattService(
                BleConstants.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY,
            )
            val characteristic = BluetoothGattCharacteristic(
                BleConstants.MESSAGE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            )
            characteristic.addDescriptor(
                BluetoothGattDescriptor(
                    BleConstants.CCCD_UUID,
                    BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
                ),
            )
            service.addCharacteristic(characteristic)
            if (!opened.addService(service)) {
                Log.e(TAG, "Could not add RESCUEMESH GATT service")
                onStatusChanged("RESCUEMESH server unavailable")
            }
        } catch (_: SecurityException) {
            Log.e(TAG, "Bluetooth connect permission missing")
            onStatusChanged("Bluetooth permission required")
        }
    }

    @SuppressLint("MissingPermission")
    private fun respond(device: BluetoothDevice, requestId: Int, status: Int) {
        try {
            server?.sendResponse(device, requestId, status, 0, null)
        } catch (_: SecurityException) {
            Log.w(TAG, "Could not send GATT response: permission missing")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        val current = server ?: return
        try {
            current.clearServices()
            current.close()
        } catch (_: Exception) {
            // Best-effort lifecycle cleanup.
        }
        server = null
    }

    private companion object {
        const val TAG = "SaharaResponder"
    }
}
