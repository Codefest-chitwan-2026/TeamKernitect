package com.kernitect.saharaandroid.disaster

import android.location.Location
import com.kernitect.saharaandroid.data.local.entity.PublicAlertEntity

data class MatchedDisasterAlert(

    val alert: PublicAlertEntity,

    val distanceMeters: Float
)

object DisasterAlertMatcher {

    fun findMatchingAlerts(

        latitude: Double,

        longitude: Double,

        alerts: List<PublicAlertEntity>,

        currentTime: Long =
            System.currentTimeMillis()

    ): List<MatchedDisasterAlert> {

        return alerts
            .filter { alert ->

                /*
                 * Ignore expired or future alerts.
                 */
                currentTime >= alert.startsAt &&
                        currentTime <= alert.expiresAt
            }
            .mapNotNull { alert ->

                val results =
                    FloatArray(1)

                /*
                 * Android calculates straight-line
                 * distance locally.
                 *
                 * No internet is needed.
                 */
                Location.distanceBetween(
                    latitude,
                    longitude,
                    alert.latitude,
                    alert.longitude,
                    results
                )

                val distance =
                    results[0]

                if (
                    distance <=
                    alert.affectedRadiusMeters
                ) {

                    MatchedDisasterAlert(
                        alert =
                            alert,

                        distanceMeters =
                            distance
                    )

                } else {

                    null
                }
            }
            .sortedWith(

                compareByDescending<MatchedDisasterAlert> {

                    severityRank(
                        it.alert.severity
                    )

                }.thenBy {

                    it.distanceMeters
                }
            )
    }


    fun findPrimaryAlert(

        latitude: Double,

        longitude: Double,

        alerts: List<PublicAlertEntity>,

        currentTime: Long =
            System.currentTimeMillis()

    ): MatchedDisasterAlert? {

        return findMatchingAlerts(
            latitude =
                latitude,

            longitude =
                longitude,

            alerts =
                alerts,

            currentTime =
                currentTime
        ).firstOrNull()
    }


    private fun severityRank(
        severity: String
    ): Int {

        return when (
            severity.uppercase()
        ) {

            "EXTREME" ->
                5

            "VERY_HIGH" ->
                4

            "HIGH" ->
                3

            "MODERATE" ->
                2

            "LOW" ->
                1

            else ->
                0
        }
    }
}