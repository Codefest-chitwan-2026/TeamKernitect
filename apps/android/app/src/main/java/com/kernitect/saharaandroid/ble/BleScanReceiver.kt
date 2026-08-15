package com.kernitect.saharaandroid.ble

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BleScanReceiver : BroadcastReceiver() {

    companion object {

        private const val TAG =
            "SAHARA_BLE"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val errorCode =
            intent.getIntExtra(
                BluetoothLeScanner.EXTRA_ERROR_CODE,
                0
            )

        if (
            errorCode != 0
        ) {

            Log.e(
                TAG,
                "PendingIntent BLE scan failed: $errorCode"
            )

            BleScanBus.listener?.invoke(
                emptyList(),
                errorCode
            )

            return
        }


        @Suppress("DEPRECATION")
        val results =
            intent.getParcelableArrayListExtra<ScanResult>(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT
            )
                ?: arrayListOf()


        Log.d(
            TAG,
            "PendingIntent delivered ${results.size} BLE result(s)"
        )


        BleScanBus.listener?.invoke(
            results,
            null
        )
    }
}