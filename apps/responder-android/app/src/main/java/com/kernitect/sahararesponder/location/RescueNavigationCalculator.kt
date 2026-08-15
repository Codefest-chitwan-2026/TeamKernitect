package com.kernitect.sahararesponder.location

import android.location.Location
import java.util.Locale
import kotlin.math.roundToInt

object RescueNavigationCalculator {
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

    fun distanceMeters(from: ResponderLocation, victimLatitude: Double, victimLongitude: Double): Float? {
        if (!isValidCoordinate(from.latitude, from.longitude) || !isValidCoordinate(victimLatitude, victimLongitude)) return null
        val results = FloatArray(2)
        Location.distanceBetween(from.latitude, from.longitude, victimLatitude, victimLongitude, results)
        return results[0]
    }

    fun bearingDegrees(from: ResponderLocation, victimLatitude: Double, victimLongitude: Double): Float? {
        if (!isValidCoordinate(from.latitude, from.longitude) || !isValidCoordinate(victimLatitude, victimLongitude)) return null
        val results = FloatArray(2)
        Location.distanceBetween(from.latitude, from.longitude, victimLatitude, victimLongitude, results)
        return ((results[1] % 360f) + 360f) % 360f
    }

    fun bearingLabel(degrees: Float): String {
        val labels = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return labels[((degrees + 22.5f) / 45f).toInt() % labels.size]
    }

    fun formatDistance(meters: Float): String =
        if (meters < 1_000f) "${meters.roundToInt()} m"
        else String.format(Locale.getDefault(), "%.1f km", meters / 1_000f)
}
