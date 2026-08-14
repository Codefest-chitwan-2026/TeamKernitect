package com.kernitect.saharaandroid.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BleGattClient(
    private val context: Context,

    private val onStatusChanged:
        (String) -> Unit,

    private val onMessageSent:
    (() -> Unit)? = null
) {

    private var connectedGatt:
            BluetoothGatt? = null

    private var messageCharacteristic:
            BluetoothGattCharacteristic? = null

    private var pendingMessage:
            String? = null

    private val gattCallback =
        object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS &&
                    newState ==
                    BluetoothProfile.STATE_CONNECTED
                ) {

                    connectedGatt =
                        gatt

                    onStatusChanged(
                        "Connected. Requesting MTU..."
                    )

                    requestMtu(
                        gatt
                    )

                    return
                }

                if (
                    newState ==
                    BluetoothProfile.STATE_DISCONNECTED
                ) {

                    onStatusChanged(
                        "Disconnected from BLE node"
                    )

                    closeGatt(
                        gatt
                    )

                    return
                }

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged(
                        "BLE connection failed: $status"
                    )

                    closeGatt(
                        gatt
                    )
                }
            }

            override fun onMtuChanged(
                gatt: BluetoothGatt,
                mtu: Int,
                status: Int
            ) {

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged(
                        "MTU $mtu. Discovering services..."
                    )

                } else {

                    onStatusChanged(
                        "MTU failed. Discovering services anyway..."
                    )
                }

                discoverServices(
                    gatt
                )
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged(
                        "Service discovery failed: $status"
                    )

                    return
                }

                val service =
                    gatt.getService(
                        BleConstants.SERVICE_UUID
                    )

                if (service == null) {

                    onStatusChanged(
                        "RESCUEMESH SERVICE NOT FOUND"
                    )

                    return
                }

                onStatusChanged(
                    "RESCUEMESH SERVICE FOUND"
                )

                val characteristic =
                    service.getCharacteristic(
                        BleConstants.MESSAGE_UUID
                    )

                if (characteristic == null) {

                    onStatusChanged(
                        "MESSAGE CHANNEL NOT FOUND"
                    )

                    return
                }

                messageCharacteristic =
                    characteristic

                onStatusChanged(
                    "MESSAGE CHANNEL FOUND"
                )

                enableNotifications(
                    gatt,
                    characteristic
                )
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {

                if (
                    descriptor.uuid !=
                    BleConstants.CCCD_UUID
                ) {
                    return
                }

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged(
                        "CCCD WRITE FAILED: $status"
                    )

                    return
                }

                onStatusChanged(
                    "Notifications enabled. Sending packet..."
                )

                /*
                 * GATT operations are asynchronous.
                 * Only send after descriptor write finishes.
                 */
                writePendingMessage(
                    gatt
                )
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {

                if (
                    characteristic.uuid !=
                    BleConstants.MESSAGE_UUID
                ) {
                    return
                }

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged(
                        "MESSAGE SENT"
                    )

                    pendingMessage =
                        null

                    onMessageSent?.invoke()

                } else {

                    onStatusChanged(
                        "MESSAGE WRITE FAILED: $status"
                    )
                }
            }
        }

    fun connectAndSend(
        device: BluetoothDevice,
        message: String
    ) {

        if (!hasConnectPermission()) {

            onStatusChanged(
                "BLUETOOTH_CONNECT permission missing"
            )

            return
        }

        pendingMessage =
            message

        connect(
            device
        )
    }

    @SuppressLint("MissingPermission")
    private fun connect(
        device: BluetoothDevice
    ) {

        try {

            connectedGatt?.let {

                closeGatt(
                    it
                )
            }

            onStatusChanged(
                "Connecting to BLE node..."
            )

            connectedGatt =
                device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )

        } catch (_: SecurityException) {

            onStatusChanged(
                "Bluetooth connection permission error"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestMtu(
        gatt: BluetoothGatt
    ) {

        try {

            val started =
                gatt.requestMtu(
                    512
                )

            if (!started) {

                onStatusChanged(
                    "MTU request unavailable. Discovering services..."
                )

                discoverServices(
                    gatt
                )
            }

        } catch (_: SecurityException) {

            onStatusChanged(
                "MTU permission error"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverServices(
        gatt: BluetoothGatt
    ) {

        try {

            val started =
                gatt.discoverServices()

            if (!started) {

                onStatusChanged(
                    "Could not start service discovery"
                )
            }

        } catch (_: SecurityException) {

            onStatusChanged(
                "Service discovery permission error"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {

        try {

            val notificationSet =
                gatt.setCharacteristicNotification(
                    characteristic,
                    true
                )

            if (!notificationSet) {

                onStatusChanged(
                    "setCharacteristicNotification failed"
                )

                return
            }

            val descriptor =
                characteristic.getDescriptor(
                    BleConstants.CCCD_UUID
                )

            if (descriptor == null) {

                onStatusChanged(
                    "CCCD NOT FOUND"
                )

                return
            }

            onStatusChanged(
                "CCCD FOUND"
            )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                val result =
                    gatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor
                            .ENABLE_NOTIFICATION_VALUE
                    )

                if (
                    result !=
                    BluetoothStatusCodes.SUCCESS
                ) {

                    onStatusChanged(
                        "Could not start CCCD write: $result"
                    )
                }

            } else {

                @Suppress("DEPRECATION")
                descriptor.value =
                    BluetoothGattDescriptor
                        .ENABLE_NOTIFICATION_VALUE

                @Suppress("DEPRECATION")
                val started =
                    gatt.writeDescriptor(
                        descriptor
                    )

                if (!started) {

                    onStatusChanged(
                        "Could not start CCCD write"
                    )
                }
            }

        } catch (_: SecurityException) {

            onStatusChanged(
                "Notification permission error"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun writePendingMessage(
        gatt: BluetoothGatt
    ) {

        val characteristic =
            messageCharacteristic
                ?: run {

                    onStatusChanged(
                        "MESSAGE characteristic unavailable"
                    )

                    return
                }

        val message =
            pendingMessage
                ?: run {

                    onStatusChanged(
                        "No pending BLE message"
                    )

                    return
                }

        val bytes =
            message.toByteArray(
                Charsets.UTF_8
            )

        try {

            onStatusChanged(
                "Writing ${bytes.size} bytes..."
            )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                val result =
                    gatt.writeCharacteristic(
                        characteristic,
                        bytes,
                        BluetoothGattCharacteristic
                            .WRITE_TYPE_DEFAULT
                    )

                if (
                    result !=
                    BluetoothStatusCodes.SUCCESS
                ) {

                    onStatusChanged(
                        "Could not start message write: $result"
                    )
                }

            } else {

                @Suppress("DEPRECATION")
                characteristic.writeType =
                    BluetoothGattCharacteristic
                        .WRITE_TYPE_DEFAULT

                @Suppress("DEPRECATION")
                characteristic.value =
                    bytes

                @Suppress("DEPRECATION")
                val started =
                    gatt.writeCharacteristic(
                        characteristic
                    )

                if (!started) {

                    onStatusChanged(
                        "Could not start message write"
                    )
                }
            }

        } catch (_: SecurityException) {

            onStatusChanged(
                "Message write permission error"
            )
        }
    }

    private fun hasConnectPermission():
            Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {

            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt(
        gatt: BluetoothGatt
    ) {

        try {

            gatt.disconnect()

        } catch (_: Exception) {
        }

        try {

            gatt.close()

        } catch (_: Exception) {
        }

        if (
            connectedGatt === gatt
        ) {

            connectedGatt =
                null
        }

        messageCharacteristic =
            null
    }

    fun close() {

        connectedGatt?.let {

            closeGatt(
                it
            )
        }

        connectedGatt =
            null

        messageCharacteristic =
            null

        pendingMessage =
            null
    }
}