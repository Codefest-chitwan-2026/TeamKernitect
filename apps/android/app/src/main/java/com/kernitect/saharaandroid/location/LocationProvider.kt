package com.kernitect.saharaandroid.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationProvider(
    context: Context
) {

    companion object {

        /*
         * For the hackathon SOS we reject locations
         * worse than 100 meters.
         */
        const val MAX_ACCEPTABLE_ACCURACY_METERS =
            100f

        private const val LOCATION_TIMEOUT_MS =
            20_000L
    }

    private val appContext =
        context.applicationContext

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(
            appContext
        )

    fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {

        if (!hasFineLocationPermission()) {

            onError(
                "Precise location permission is required"
            )

            return
        }

        requestCurrentLocation(
            onSuccess = onSuccess,
            onError = onError
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation(
        onSuccess: (Location) -> Unit,
        onError: (String) -> Unit
    ) {

        val request =
            CurrentLocationRequest.Builder()
                /*
                 * We want the most accurate fix available.
                 */
                .setPriority(
                    Priority.PRIORITY_HIGH_ACCURACY
                )

                /*
                 * Require fine-grained positioning.
                 */
                .setGranularity(
                    Granularity.GRANULARITY_FINE
                )

                /*
                 * Do not accept an old cached location.
                 *
                 * We want a fresh SOS location.
                 */
                .setMaxUpdateAgeMillis(
                    0
                )

                /*
                 * Give GPS some time to obtain a fix.
                 */
                .setDurationMillis(
                    LOCATION_TIMEOUT_MS
                )

                .build()

        try {

            fusedLocationClient
                .getCurrentLocation(
                    request,
                    null
                )
                .addOnSuccessListener { location ->

                    if (location == null) {

                        onError(
                            "Could not get current location. Try moving near a window or outdoors."
                        )

                        return@addOnSuccessListener
                    }

                    /*
                     * Location.hasAccuracy() tells us
                     * whether Android supplied an
                     * estimated horizontal accuracy.
                     */
                    if (!location.hasAccuracy()) {

                        onError(
                            "Location accuracy unavailable"
                        )

                        return@addOnSuccessListener
                    }

                    if (
                        location.accuracy >
                        MAX_ACCEPTABLE_ACCURACY_METERS
                    ) {

                        onError(
                            "Location accuracy too low: " +
                                    "${location.accuracy.toInt()} m. " +
                                    "Move near a window or outdoors."
                        )

                        return@addOnSuccessListener
                    }

                    onSuccess(
                        location
                    )
                }
                .addOnFailureListener { exception ->

                    onError(
                        exception.message
                            ?: "Failed to get current location"
                    )
                }

        } catch (_: SecurityException) {

            onError(
                "Location permission error"
            )
        }
    }

    private fun hasFineLocationPermission():
            Boolean {

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED
    }
}