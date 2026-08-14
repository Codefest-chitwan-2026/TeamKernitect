package com.kernitect.saharaandroid.ble

import android.Manifest
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
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BleGattServer(
    private val context: Context,

    private val onMessageReceived: (
        device: BluetoothDevice,
        message: String
    ) -> Unit,

    private val onStatusChanged: (String) -> Unit,

    private val onReady: (() -> Unit)? = null
) {

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private var gattServer:
            BluetoothGattServer? = null

    private val gattServerCallback =
        object : BluetoothGattServerCallback() {

            override fun onServiceAdded(
                status: Int,
                service: BluetoothGattService
            ) {

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged(
                        "GATT service ready"
                    )

                    onReady?.invoke()

                } else {

                    onStatusChanged(
                        "Failed to add GATT service: $status"
                    )
                }
            }

            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {

                when (newState) {

                    BluetoothProfile.STATE_CONNECTED -> {

                        onStatusChanged(
                            "GATT client connected"
                        )
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {

                        onStatusChanged(
                            "GATT client disconnected"
                        )
                    }
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {

                if (
                    characteristic.uuid !=
                    BleConstants.MESSAGE_UUID
                ) {

                    if (responseNeeded) {

                        sendResponseSafely(
                            device = device,
                            requestId = requestId,
                            status =
                                BluetoothGatt
                                    .GATT_REQUEST_NOT_SUPPORTED
                        )
                    }

                    return
                }

                if (
                    preparedWrite ||
                    offset != 0
                ) {

                    if (responseNeeded) {

                        sendResponseSafely(
                            device = device,
                            requestId = requestId,
                            status =
                                BluetoothGatt
                                    .GATT_REQUEST_NOT_SUPPORTED
                        )
                    }

                    return
                }

                val message =
                    value.toString(
                        Charsets.UTF_8
                    )

                onMessageReceived(
                    device,
                    message
                )

                if (responseNeeded) {

                    sendResponseSafely(
                        device = device,
                        requestId = requestId,
                        status =
                            BluetoothGatt.GATT_SUCCESS
                    )
                }

                onStatusChanged(
                    "Message received"
                )
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor
            ) {

                if (
                    descriptor.uuid !=
                    BleConstants.CCCD_UUID
                ) {

                    sendResponseSafely(
                        device = device,
                        requestId = requestId,
                        status =
                            BluetoothGatt
                                .GATT_REQUEST_NOT_SUPPORTED
                    )

                    return
                }

                sendResponseSafely(
                    device = device,
                    requestId = requestId,
                    status =
                        BluetoothGatt.GATT_SUCCESS,
                    value =
                        byteArrayOf(
                            0x00,
                            0x00
                        )
                )
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {

                if (!responseNeeded) {
                    return
                }

                if (
                    descriptor.uuid ==
                    BleConstants.CCCD_UUID &&
                    !preparedWrite &&
                    offset == 0
                ) {

                    sendResponseSafely(
                        device = device,
                        requestId = requestId,
                        status =
                            BluetoothGatt.GATT_SUCCESS
                    )

                } else {

                    sendResponseSafely(
                        device = device,
                        requestId = requestId,
                        status =
                            BluetoothGatt
                                .GATT_REQUEST_NOT_SUPPORTED
                    )
                }
            }
        }

    private fun hasBluetoothConnectPermission():
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
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startServer() {

        if (!hasBluetoothConnectPermission()) {

            onStatusChanged(
                "GATT server failed: Bluetooth permission missing"
            )

            return
        }

        if (
            !AppRequirements
                .isBluetoothEnabled(context)
        ) {

            onStatusChanged(
                "GATT server failed: Bluetooth is off"
            )

            return
        }

        if (gattServer != null) {

            onStatusChanged(
                "GATT server already running"
            )

            return
        }

        startServerWithPermission()
    }

    @SuppressLint("MissingPermission")
    private fun startServerWithPermission() {

        try {

            val server =
                bluetoothManager.openGattServer(
                    context,
                    gattServerCallback
                )

            if (server == null) {

                onStatusChanged(
                    "Failed to open GATT server"
                )

                return
            }

            val service =
                BluetoothGattService(
                    BleConstants.SERVICE_UUID,
                    BluetoothGattService
                        .SERVICE_TYPE_PRIMARY
                )

            val messageCharacteristic =
                BluetoothGattCharacteristic(
                    BleConstants.MESSAGE_UUID,

                    BluetoothGattCharacteristic
                        .PROPERTY_WRITE or
                            BluetoothGattCharacteristic
                                .PROPERTY_WRITE_NO_RESPONSE or
                            BluetoothGattCharacteristic
                                .PROPERTY_NOTIFY,

                    BluetoothGattCharacteristic
                        .PERMISSION_WRITE
                )

            val cccdDescriptor =
                BluetoothGattDescriptor(
                    BleConstants.CCCD_UUID,

                    BluetoothGattDescriptor
                        .PERMISSION_READ or
                            BluetoothGattDescriptor
                                .PERMISSION_WRITE
                )

            if (
                !messageCharacteristic
                    .addDescriptor(
                        cccdDescriptor
                    )
            ) {

                onStatusChanged(
                    "Failed to add CCCD descriptor"
                )

                server.close()

                return
            }

            if (
                !service.addCharacteristic(
                    messageCharacteristic
                )
            ) {

                onStatusChanged(
                    "Failed to add MESSAGE characteristic"
                )

                server.close()

                return
            }

            gattServer =
                server

            if (
                !server.addService(
                    service
                )
            ) {

                onStatusChanged(
                    "Failed to start GATT service"
                )

                server.close()

                gattServer = null

                return
            }

            onStatusChanged(
                "Starting GATT server..."
            )

        } catch (_: SecurityException) {

            gattServer = null

            onStatusChanged(
                "GATT server permission error"
            )
        }
    }

    private fun sendResponseSafely(
        device: BluetoothDevice,
        requestId: Int,
        status: Int,
        value: ByteArray? = null
    ) {

        if (!hasBluetoothConnectPermission()) {
            return
        }

        sendResponseWithPermission(
            device = device,
            requestId = requestId,
            status = status,
            value = value
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendResponseWithPermission(
        device: BluetoothDevice,
        requestId: Int,
        status: Int,
        value: ByteArray?
    ) {

        try {

            gattServer?.sendResponse(
                device,
                requestId,
                status,
                0,
                value
            )

        } catch (_: SecurityException) {

            onStatusChanged(
                "Unable to send GATT response"
            )
        }
    }

    fun stopServer() {

        if (gattServer == null) {
            return
        }

        if (!hasBluetoothConnectPermission()) {

            gattServer = null

            return
        }

        stopServerWithPermission()
    }

    @SuppressLint("MissingPermission")
    private fun stopServerWithPermission() {

        try {

            gattServer?.clearServices()
            gattServer?.close()

        } catch (_: SecurityException) {
            // Cleanup only.
        }

        gattServer = null

        onStatusChanged(
            "GATT server stopped"
        )
    }
}