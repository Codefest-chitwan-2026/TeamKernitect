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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BleGattServer(
    private val context: Context,

    private val onMessageReceived:
        (
        device: BluetoothDevice,
        message: String
    ) -> Unit,

    private val onStatusChanged:
        (String) -> Unit,

    private val onReady:
        () -> Unit
) {

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private var gattServer:
            BluetoothGattServer? = null

    private val callback =
        object : BluetoothGattServerCallback() {

            override fun onServiceAdded(
                status: Int,
                service: BluetoothGattService
            ) {

                if (
                    service.uuid !=
                    BleConstants.SERVICE_UUID
                ) {
                    return
                }

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    onStatusChanged(
                        "GATT service ready"
                    )

                    onReady()

                } else {

                    onStatusChanged(
                        "GATT service creation failed: $status"
                    )
                }
            }

            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {

                onStatusChanged(
                    "GATT server connection state: $newState"
                )
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

                        sendResponse(
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

                    sendResponse(
                        device = device,
                        requestId = requestId,
                        status =
                            BluetoothGatt
                                .GATT_SUCCESS
                    )
                }
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

                if (
                    descriptor.uuid ==
                    BleConstants.CCCD_UUID
                ) {

                    @Suppress("DEPRECATION")
                    descriptor.value =
                        value

                    onStatusChanged(
                        "Client enabled notifications"
                    )

                    if (responseNeeded) {

                        sendResponse(
                            device = device,
                            requestId = requestId,
                            status =
                                BluetoothGatt
                                    .GATT_SUCCESS
                        )
                    }

                    return
                }

                if (responseNeeded) {

                    sendResponse(
                        device = device,
                        requestId = requestId,
                        status =
                            BluetoothGatt
                                .GATT_REQUEST_NOT_SUPPORTED
                    )
                }
            }
        }

    fun startServer() {

        if (!hasConnectPermission()) {

            onStatusChanged(
                "Cannot start GATT server: missing Bluetooth permission"
            )

            return
        }

        startServerWithPermission()
    }

    @SuppressLint("MissingPermission")
    private fun startServerWithPermission() {

        if (
            gattServer != null
        ) {

            onStatusChanged(
                "GATT server already running"
            )

            return
        }

        try {

            val server =
                bluetoothManager.openGattServer(
                    context,
                    callback
                )

            if (server == null) {

                onStatusChanged(
                    "Could not open GATT server"
                )

                return
            }

            gattServer =
                server

            val service =
                BluetoothGattService(
                    BleConstants.SERVICE_UUID,
                    BluetoothGattService
                        .SERVICE_TYPE_PRIMARY
                )

            val characteristic =
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

            /*
             * Android GATT server requires us to add
             * the CCCD explicitly.
             */
            val cccd =
                BluetoothGattDescriptor(
                    BleConstants.CCCD_UUID,

                    BluetoothGattDescriptor
                        .PERMISSION_READ or
                            BluetoothGattDescriptor
                                .PERMISSION_WRITE
                )

            characteristic.addDescriptor(
                cccd
            )

            service.addCharacteristic(
                characteristic
            )

            val added =
                server.addService(
                    service
                )

            if (!added) {

                onStatusChanged(
                    "Could not add RESCUEMESH GATT service"
                )
            } else {

                onStatusChanged(
                    "Creating RESCUEMESH GATT service..."
                )
            }

        } catch (_: SecurityException) {

            onStatusChanged(
                "GATT server permission error"
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendResponse(
        device: BluetoothDevice,
        requestId: Int,
        status: Int
    ) {

        try {

            gattServer?.sendResponse(
                device,
                requestId,
                status,
                0,
                null
            )

        } catch (_: SecurityException) {

            onStatusChanged(
                "Could not send GATT response"
            )
        }
    }

    fun stopServer() {

        val server =
            gattServer
                ?: return

        if (!hasConnectPermission()) {

            gattServer =
                null

            return
        }

        stopServerWithPermission(
            server
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopServerWithPermission(
        server: BluetoothGattServer
    ) {

        try {

            server.clearServices()

        } catch (_: Exception) {
        }

        try {

            server.close()

        } catch (_: Exception) {
        }

        gattServer =
            null

        onStatusChanged(
            "GATT server stopped"
        )
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
}