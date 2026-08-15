package com.kernitect.sahararesponder

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kernitect.sahararesponder.ble.ResponderBleAdvertiser
import com.kernitect.sahararesponder.ble.ResponderBleServer
import com.kernitect.sahararesponder.model.RescuePacket
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.ui.screens.home.ResponderHomeScreen
import com.kernitect.sahararesponder.ui.theme.SaharaResponderTheme

class MainActivity : ComponentActivity() {
    private var meshStatus by mutableStateOf("Starting RESCUEMESH")
    private val incidents = mutableStateListOf<ResponderIncident>()
    private val seenPacketIds = mutableSetOf<String>()
    private var receiverStarted = false
    private lateinit var bleServer: ResponderBleServer
    private lateinit var bleAdvertiser: ResponderBleAdvertiser

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) startReceiverIfReady() else {
            Log.w(TAG, "Bluetooth permission missing")
            meshStatus = "Bluetooth permission required"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bleAdvertiser = ResponderBleAdvertiser(applicationContext, ::postStatus)
        bleServer = ResponderBleServer(
            context = applicationContext,
            onMessageReceived = ::handlePacket,
            onStatusChanged = ::postStatus,
            onReady = { runOnUiThread { bleAdvertiser.start() } },
        )
        setContent {
            SaharaResponderTheme {
                Scaffold(Modifier.fillMaxSize()) { padding ->
                    ResponderHomeScreen(meshStatus, incidents, Modifier.padding(padding))
                }
            }
        }
        ensurePermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()
        if (::bleServer.isInitialized && hasRequiredPermissions()) startReceiverIfReady()
    }

    private fun ensurePermissionsAndStart() {
        if (hasRequiredPermissions()) startReceiverIfReady()
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            meshStatus = "Bluetooth permission required"
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE))
        }
    }

    private fun startReceiverIfReady() {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null) {
            meshStatus = "Bluetooth unavailable"
            return
        }
        if (!adapter.isEnabled) {
            meshStatus = "Bluetooth disabled"
            return
        }
        if (receiverStarted) return
        receiverStarted = true
        meshStatus = "Starting RESCUEMESH"
        bleServer.start()
    }

    private fun handlePacket(raw: String) {
        val packet = RescuePacket.fromJson(raw)
        if (packet == null) {
            Log.w(TAG, "Malformed JSON ignored")
            return
        }
        if (packet.type != RescuePacket.TYPE_SOS) {
            Log.d(TAG, "Unsupported packet type ignored: ${packet.type}")
            return
        }
        runOnUiThread {
            if (!seenPacketIds.add(packet.id)) {
                Log.d(TAG, "Duplicate packet ignored: ${packet.id}")
                return@runOnUiThread
            }
            Log.i(TAG, "SOS parsed: ${packet.id}, priority=${packet.priority}")
            incidents.add(0, ResponderIncident.fromPacket(packet))
            meshStatus = "SOS received"
        }
    }

    private fun postStatus(status: String) = runOnUiThread { meshStatus = status }

    private fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        if (::bleAdvertiser.isInitialized) bleAdvertiser.stop()
        if (::bleServer.isInitialized) bleServer.stop()
        receiverStarted = false
        super.onDestroy()
    }

    private companion object { const val TAG = "SaharaResponder" }
}
