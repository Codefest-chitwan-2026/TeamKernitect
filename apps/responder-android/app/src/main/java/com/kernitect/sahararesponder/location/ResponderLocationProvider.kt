package com.kernitect.sahararesponder.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class ResponderLocationProvider(
    context: Context,
    private val onLocation: (ResponderLocation) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private var running = false
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            onLocation(
                ResponderLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0f,
                    timestamp = location.time,
                ),
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        if (!hasLocationPermission()) {
            onStatus("Responder location permission required")
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()
        try {
            running = true
            onStatus("Locating responder…")
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener {
                    running = false
                    onStatus("Responder location unavailable")
                }
        } catch (_: SecurityException) {
            running = false
            onStatus("Responder location permission required")
        }
    }

    fun stop() {
        if (!running) return
        running = false
        client.removeLocationUpdates(callback)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val UPDATE_INTERVAL_MS = 5_000L
        const val MIN_UPDATE_INTERVAL_MS = 2_500L
    }
}
