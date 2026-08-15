package com.kernitect.saharaandroid.ble

import android.bluetooth.le.ScanResult

object BleScanBus {

    @Volatile
    var listener:
            ((
                results: List<ScanResult>,
                errorCode: Int?
            ) -> Unit)? = null
}