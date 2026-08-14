package com.kernitect.saharaandroid.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationProvider(
    private val context: Context
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager =
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

    companion object {

        // For the hackathon, reject obviously bad fixes.
        const val MAX_ACCEPTABLE_ACCURACY_METERS = 100f
    }

    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {

        val hasFineLocation =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {

            onError(
                "Precise location permission is not granted"
            )

            return
        }

        if (!locationManager.isLocationEnabled) {

            onError(
                "Location services are disabled"
            )

            return
        }

        getFreshLocation(
            onSuccess = onSuccess,
            onError = onError
        )
    }

    @SuppressLint("MissingPermission")
    private fun getFreshLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {

        val request =
            CurrentLocationRequest.Builder()
                .setPriority(
                    Priority.PRIORITY_HIGH_ACCURACY
                )
                .setGranularity(
                    Granularity.GRANULARITY_FINE
                )

                // 0 = do not return an old cached location.
                .setMaxUpdateAgeMillis(0)

                // Give GPS some time to obtain a real fix.
                .setDurationMillis(20_000)
                .build()

        val cancellationTokenSource =
            CancellationTokenSource()

        fusedLocationClient
            .getCurrentLocation(
                request,
                cancellationTokenSource.token
            )
            .addOnSuccessListener { location ->

                if (location == null) {

                    onError(
                        "Could not obtain a fresh location. " +
                                "Move near a window or outdoors and try again."
                    )

                    return@addOnSuccessListener
                }

                if (!location.hasAccuracy()) {

                    onError(
                        "Location received without an accuracy estimate."
                    )

                    return@addOnSuccessListener
                }

                if (
                    location.accuracy >
                    MAX_ACCEPTABLE_ACCURACY_METERS
                ) {

                    onError(
                        "Location accuracy is too low: " +
                                "${location.accuracy.toInt()} m. " +
                                "Move near a window or outdoors and try again."
                    )

                    return@addOnSuccessListener
                }

                onSuccess(location)
            }
            .addOnFailureListener { exception ->

                onError(
                    exception.message
                        ?: "Unable to obtain current location"
                )
            }
    }
}