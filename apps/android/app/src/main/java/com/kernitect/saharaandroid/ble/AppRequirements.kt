package com.kernitect.saharaandroid.ble

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

object AppRequirements {

    fun requiredRuntimePermissions():
            Array<String> {

        val permissions =
            mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            permissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        }

        return permissions.toTypedArray()
    }

    fun hasAllRuntimePermissions(
        context: Context
    ): Boolean {

        return requiredRuntimePermissions()
            .all { permission ->

                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) ==
                        PackageManager.PERMISSION_GRANTED
            }
    }

    fun hasPreciseLocation(
        context: Context
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun supportsBle(
        context: Context
    ): Boolean {

        return context.packageManager
            .hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE
            )
    }

    fun isBluetoothEnabled(
        context: Context
    ): Boolean {

        val bluetoothManager =
            context.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager

        return bluetoothManager.adapter
            ?.isEnabled == true
    }

    fun isLocationServicesEnabled(
        context: Context
    ): Boolean {

        val locationManager =
            context.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        return try {

            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) ||
                    locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    )

        } catch (_: Exception) {

            false
        }
    }
}