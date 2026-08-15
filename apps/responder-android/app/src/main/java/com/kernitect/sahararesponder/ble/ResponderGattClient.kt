package com.kernitect.sahararesponder.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log

class ResponderGattClient(
    private val context: Context,
    private val onPhaseChanged: (String) -> Unit,
    private val onSuccess: () -> Unit,
    private val onFailure: (String) -> Unit,
) {
    private var gatt: BluetoothGatt? = null
    private var pendingJson: String? = null
    private var completed = false

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(current: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = current
                requestMtu(current)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (!completed && pendingJson != null) fail("Relay disconnected before ACK write")
                closeGatt(current)
            } else if (status != BluetoothGatt.GATT_SUCCESS) fail("BLE connection failed: $status")
        }

        override fun onMtuChanged(current: BluetoothGatt, mtu: Int, status: Int) = discover(current)

        override fun onServicesDiscovered(current: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return fail("Service discovery failed: $status")
            val characteristic = current.getService(BleConstants.SERVICE_UUID)?.getCharacteristic(BleConstants.MESSAGE_UUID)
                ?: return fail("RESCUEMESH message characteristic not found")
            enableNotifications(current, characteristic)
        }

        override fun onDescriptorWrite(current: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid != BleConstants.CCCD_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) return fail("CCCD write failed: $status")
            writeAck(current, descriptor.characteristic)
        }

        override fun onCharacteristicWrite(current: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid != BleConstants.MESSAGE_UUID) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                completed = true
                pendingJson = null
                Log.i(TAG, "ACK written to mesh")
                onSuccess()
                closeGatt(current)
            } else fail("ACK write failed: $status")
        }
    }

    @SuppressLint("MissingPermission")
    fun connectAndSend(device: BluetoothDevice, json: String) {
        close()
        pendingJson = json
        completed = false
        try {
            onPhaseChanged("CONNECTING")
            Log.i(TAG, "Connecting to relay")
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        } catch (_: SecurityException) { fail("Bluetooth connect permission required") }
    }

    @SuppressLint("MissingPermission")
    private fun requestMtu(current: BluetoothGatt) {
        try { if (!current.requestMtu(512)) discover(current) } catch (_: SecurityException) { fail("MTU permission error") }
    }

    @SuppressLint("MissingPermission")
    private fun discover(current: BluetoothGatt) {
        try { if (!current.discoverServices()) fail("Could not start service discovery") } catch (_: SecurityException) { fail("Service discovery permission error") }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(current: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            if (!current.setCharacteristicNotification(characteristic, true)) return fail("Could not enable notifications")
            val descriptor = characteristic.getDescriptor(BleConstants.CCCD_UUID) ?: return fail("CCCD not found")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (current.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) != BluetoothStatusCodes.SUCCESS) fail("Could not start CCCD write")
            } else {
                @Suppress("DEPRECATION") descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION") if (!current.writeDescriptor(descriptor)) fail("Could not start CCCD write")
            }
        } catch (_: SecurityException) { fail("Notification permission error") }
    }

    @SuppressLint("MissingPermission")
    private fun writeAck(current: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val bytes = pendingJson?.toByteArray(Charsets.UTF_8) ?: return fail("No ACK pending")
        onPhaseChanged("SENDING")
        Log.i(TAG, "ACK write started")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (current.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) != BluetoothStatusCodes.SUCCESS) fail("Could not start ACK write")
            } else {
                @Suppress("DEPRECATION") characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION") characteristic.value = bytes
                @Suppress("DEPRECATION") if (!current.writeCharacteristic(characteristic)) fail("Could not start ACK write")
            }
        } catch (_: SecurityException) { fail("ACK write permission error") }
    }

    private fun fail(reason: String) {
        if (completed) return
        completed = true
        Log.e(TAG, "ACK send failed: $reason")
        onFailure(reason)
        close()
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt(current: BluetoothGatt) {
        try { current.disconnect() } catch (_: Exception) { }
        try { current.close() } catch (_: Exception) { }
        if (gatt === current) gatt = null
    }

    fun close() {
        gatt?.let(::closeGatt)
        gatt = null
        pendingJson = null
    }

    private companion object { const val TAG = "SaharaResponder" }
}
