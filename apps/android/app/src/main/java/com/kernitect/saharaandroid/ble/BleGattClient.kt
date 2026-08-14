package com.kernitect.saharaandroid.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BleGattClient(
    private val context: Context,
    private val onStatusChanged: (String) -> Unit,
    private val onMessageSent: (() -> Unit)? = null
) {

    private var bluetoothGatt: BluetoothGatt? = null

    private var pendingMessage: String? = null

    private var messageCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback =
        object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                if (
                    status == BluetoothGatt.GATT_SUCCESS &&
                    newState == BluetoothProfile.STATE_CONNECTED
                ) {

                    onStatusChanged(
                        "Connected. Discovering services..."
                    )

                    discoverServicesSafely(gatt)

                    return
                }

                if (
                    newState == BluetoothProfile.STATE_DISCONNECTED
                ) {

                    onStatusChanged(
                        "GATT disconnected"
                    )

                    closeSpecificGattSafely(gatt)

                    if (bluetoothGatt === gatt) {
                        bluetoothGatt = null
                    }

                    messageCharacteristic = null

                    return
                }

                if (status != BluetoothGatt.GATT_SUCCESS) {

                    onStatusChanged(
                        "GATT connection failed: $status"
                    )

                    closeSpecificGattSafely(gatt)

                    if (bluetoothGatt === gatt) {
                        bluetoothGatt = null
                    }

                    messageCharacteristic = null
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {

                if (status != BluetoothGatt.GATT_SUCCESS) {

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
                        "RESCUEMESH service not found"
                    )

                    return
                }

                val characteristic =
                    service.getCharacteristic(
                        BleConstants.MESSAGE_UUID
                    )

                if (characteristic == null) {

                    onStatusChanged(
                        "MESSAGE characteristic not found"
                    )

                    return
                }

                messageCharacteristic =
                    characteristic

                onStatusChanged(
                    "Service found. Requesting larger MTU..."
                )

                requestMtuSafely(gatt)
            }

            override fun onMtuChanged(
                gatt: BluetoothGatt,
                mtu: Int,
                status: Int
            ) {

                if (status == BluetoothGatt.GATT_SUCCESS) {

                    onStatusChanged(
                        "MTU $mtu. Sending message..."
                    )

                } else {

                    onStatusChanged(
                        "MTU request failed. Trying send..."
                    )
                }

                writePendingMessageSafely(gatt)
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

                if (status == BluetoothGatt.GATT_SUCCESS) {

                    pendingMessage = null

                    onStatusChanged(
                        "SOS JSON sent successfully"
                    )

                    onMessageSent?.invoke()

                } else {

                    onStatusChanged(
                        "SOS write failed: $status"
                    )
                }
            }
        }

    /*
     * Android 12+ requires BLUETOOTH_CONNECT
     * for GATT connections and operations.
     *
     * Android 11 and lower use the legacy
     * Bluetooth permission model.
     */
    private fun hasBluetoothConnectPermission(): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun connectAndSend(
        device: BluetoothDevice,
        message: String
    ) {

        if (!hasBluetoothConnectPermission()) {

            onStatusChanged(
                "GATT client failed: Bluetooth permission missing"
            )

            return
        }

        if (!AppRequirements.isBluetoothEnabled(context)) {

            onStatusChanged(
                "GATT client failed: Bluetooth is off"
            )

            return
        }

        connectWithPermission(
            device = device,
            message = message
        )
    }

    @SuppressLint("MissingPermission")
    private fun connectWithPermission(
        device: BluetoothDevice,
        message: String
    ) {

        try {

            /*
             * Close any previous client connection before
             * starting another one.
             */
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()

            bluetoothGatt = null
            messageCharacteristic = null

            pendingMessage =
                message

            onStatusChanged(
                "Connecting to RESCUEMESH device..."
            )

            bluetoothGatt =
                device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )

            if (bluetoothGatt == null) {

                onStatusChanged(
                    "Failed to create GATT connection"
                )
            }

        } catch (_: SecurityException) {

            bluetoothGatt = null
            messageCharacteristic = null

            onStatusChanged(
                "GATT connection permission error"
            )
        }
    }

    private fun discoverServicesSafely(
        gatt: BluetoothGatt
    ) {

        if (!hasBluetoothConnectPermission()) {

            onStatusChanged(
                "Cannot discover services: permission missing"
            )

            return
        }

        discoverServicesWithPermission(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun discoverServicesWithPermission(
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

    private fun requestMtuSafely(
        gatt: BluetoothGatt
    ) {

        if (!hasBluetoothConnectPermission()) {

            onStatusChanged(
                "MTU request skipped: permission missing"
            )

            return
        }

        requestMtuWithPermission(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun requestMtuWithPermission(
        gatt: BluetoothGatt
    ) {

        try {

            val started =
                gatt.requestMtu(512)

            /*
             * Some devices may refuse to start an MTU request.
             * In that case we still attempt the write.
             */
            if (!started) {

                onStatusChanged(
                    "MTU request unavailable. Trying send..."
                )

                writePendingMessageSafely(gatt)
            }

        } catch (_: SecurityException) {

            onStatusChanged(
                "MTU request permission error"
            )
        }
    }

    private fun writePendingMessageSafely(
        gatt: BluetoothGatt
    ) {

        if (!hasBluetoothConnectPermission()) {

            onStatusChanged(
                "Cannot send SOS: Bluetooth permission missing"
            )

            return
        }

        writePendingMessageWithPermission(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun writePendingMessageWithPermission(
        gatt: BluetoothGatt
    ) {

        val message =
            pendingMessage
                ?: return

        val characteristic =
            messageCharacteristic
                ?: run {

                    onStatusChanged(
                        "Cannot send: characteristic unavailable"
                    )

                    return
                }

        val bytes =
            message.toByteArray(
                Charsets.UTF_8
            )

        onStatusChanged(
            "Writing ${bytes.size} bytes..."
        )

        try {

            /*
             * Android 13 / API 33 introduced the
             * newer writeCharacteristic overload.
             */
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                val result =
                    gatt.writeCharacteristic(
                        characteristic,
                        bytes,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )

                if (
                    result !=
                    BluetoothStatusCodes.SUCCESS
                ) {

                    onStatusChanged(
                        "Could not start write: $result"
                    )
                }

            } else {

                /*
                 * Android 11 uses this legacy API.
                 */
                @Suppress("DEPRECATION")
                characteristic.writeType =
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

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
                        "Could not start write"
                    )
                }
            }

        } catch (_: SecurityException) {

            onStatusChanged(
                "SOS write permission error"
            )
        }
    }

    fun close() {

        if (bluetoothGatt == null) {

            pendingMessage = null
            messageCharacteristic = null

            return
        }

        if (!hasBluetoothConnectPermission()) {

            bluetoothGatt = null
            pendingMessage = null
            messageCharacteristic = null

            return
        }

        closeWithPermission()
    }

    @SuppressLint("MissingPermission")
    private fun closeWithPermission() {

        try {

            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()

        } catch (_: SecurityException) {
            // Permission may have been revoked while running.
        }

        bluetoothGatt = null
        pendingMessage = null
        messageCharacteristic = null
    }

    private fun closeSpecificGattSafely(
        gatt: BluetoothGatt
    ) {

        if (!hasBluetoothConnectPermission()) {
            return
        }

        closeSpecificGattWithPermission(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun closeSpecificGattWithPermission(
        gatt: BluetoothGatt
    ) {

        try {

            gatt.close()

        } catch (_: SecurityException) {
            // Ignore cleanup failure.
        }
    }
}