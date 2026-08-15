package com.kernitect.saharaandroid.model

import org.json.JSONObject
import java.util.UUID

data class RescuePacket(

    val id: String,

    val type: String,

    val latitude: Double,

    val longitude: Double,

    val timestamp: Long,

    val hopCount: Int,

    val ttl: Int,

    val priority: String,

    val message: String,

    /*
     * Environmental context determined from
     * locally stored public emergency alerts.
     *
     * This is NOT treated as confirmed cause.
     */
    val likelyDisaster: String =
        DISASTER_UNKNOWN,

    /*
     * Severity of the local public-alert zone
     * containing the sender.
     */
    val areaSeverity: String =
        SEVERITY_UNKNOWN
) {

    fun toJson(): String =

        JSONObject().apply {

            put(
                "id",
                id
            )

            put(
                "type",
                type
            )

            put(
                "latitude",
                latitude
            )

            put(
                "longitude",
                longitude
            )

            put(
                "timestamp",
                timestamp
            )

            put(
                "hopCount",
                hopCount
            )

            put(
                "ttl",
                ttl
            )

            put(
                "priority",
                priority
            )

            put(
                "message",
                message
            )

            /*
             * Disaster-awareness fields.
             */
            put(
                "likelyDisaster",
                likelyDisaster
            )

            put(
                "areaSeverity",
                areaSeverity
            )

        }.toString()


    fun canRelay(): Boolean =

        hopCount < ttl


    /*
     * copy() preserves:
     *
     * - original GPS
     * - original timestamp
     * - disaster context
     * - priority
     * - message
     *
     * Only hopCount changes.
     */
    fun nextHop(): RescuePacket =

        copy(
            hopCount =
                hopCount + 1
        )


    companion object {

        const val TYPE_SOS =
            "SOS"


        const val PRIORITY_NORMAL =
            "NORMAL"

        const val PRIORITY_HIGH =
            "HIGH"

        const val PRIORITY_CRITICAL =
            "CRITICAL"


        const val DEFAULT_TTL =
            5


        /*
         * Used when no locally stored public
         * disaster alert matches the sender's GPS.
         */
        const val DISASTER_UNKNOWN =
            "UNKNOWN"

        const val SEVERITY_UNKNOWN =
            "UNKNOWN"


        /*
         * ==========================================
         * CRITICAL SOS
         * ==========================================
         *
         * Big red emergency button.
         */
        fun createCriticalSos(

            latitude: Double,

            longitude: Double,

            likelyDisaster: String =
                DISASTER_UNKNOWN,

            areaSeverity: String =
                SEVERITY_UNKNOWN

        ): RescuePacket {


            /*
             * Keep the environmental information
             * human-readable too.
             *
             * Existing notification/details screens
             * already display packet.message.
             */
            val message =

                if (
                    likelyDisaster !=
                    DISASTER_UNKNOWN
                ) {

                    "Critical emergency SOS" +
                            " | Likely disaster: ${
                                likelyDisaster
                                    .replace(
                                        "_",
                                        " "
                                    )
                            }" +
                            " | Area severity: ${
                                areaSeverity
                                    .replace(
                                        "_",
                                        " "
                                    )
                            }"

                } else {

                    "Critical emergency SOS" +
                            " | Likely disaster: UNKNOWN"
                }


            return RescuePacket(

                id =
                    UUID.randomUUID()
                        .toString(),

                type =
                    TYPE_SOS,

                latitude =
                    latitude,

                longitude =
                    longitude,

                timestamp =
                    System.currentTimeMillis(),

                hopCount =
                    0,

                ttl =
                    DEFAULT_TTL,

                priority =
                    PRIORITY_CRITICAL,

                message =
                    message,

                likelyDisaster =
                    likelyDisaster,

                areaSeverity =
                    areaSeverity
            )
        }


        /*
         * ==========================================
         * NORMAL HELP REQUEST
         * ==========================================
         *
         * This remains based on what the citizen
         * explicitly reports.
         *
         * We are NOT automatically overriding their
         * selected problem using area-alert context.
         */
        fun createHelpRequest(

            latitude: Double,

            longitude: Double,

            disasterType: String,

            peopleCount: String,

            explanation: String

        ): RescuePacket {

            val messageParts =
                mutableListOf<String>()


            messageParts.add(
                disasterType
            )


            messageParts.add(
                "People: $peopleCount"
            )


            if (
                explanation
                    .isNotBlank()
            ) {

                messageParts.add(
                    explanation.trim()
                )
            }


            return RescuePacket(

                id =
                    UUID.randomUUID()
                        .toString(),

                type =
                    TYPE_SOS,

                latitude =
                    latitude,

                longitude =
                    longitude,

                timestamp =
                    System.currentTimeMillis(),

                hopCount =
                    0,

                ttl =
                    DEFAULT_TTL,

                priority =
                    PRIORITY_NORMAL,

                message =
                    messageParts
                        .joinToString(
                            separator = " | "
                        ),

                likelyDisaster =
                    DISASTER_UNKNOWN,

                areaSeverity =
                    SEVERITY_UNKNOWN
            )
        }


        /*
         * ==========================================
         * JSON → PACKET
         * ==========================================
         */
        fun fromJson(
            json: String
        ): RescuePacket? {

            return try {

                val obj =
                    JSONObject(
                        json
                    )


                RescuePacket(

                    id =
                        obj.getString(
                            "id"
                        ),

                    type =
                        obj.getString(
                            "type"
                        ),

                    latitude =
                        obj.getDouble(
                            "latitude"
                        ),

                    longitude =
                        obj.getDouble(
                            "longitude"
                        ),

                    timestamp =
                        obj.getLong(
                            "timestamp"
                        ),

                    hopCount =
                        obj.getInt(
                            "hopCount"
                        ),

                    ttl =
                        obj.getInt(
                            "ttl"
                        ),


                    /*
                     * Backward compatibility with
                     * older RESCUEMESH packets.
                     */
                    priority =
                        obj.optString(
                            "priority",
                            PRIORITY_CRITICAL
                        ),


                    message =
                        obj.optString(
                            "message",
                            "Critical emergency SOS"
                        ),


                    /*
                     * Older phones may send packets
                     * without these fields.
                     *
                     * They remain valid.
                     */
                    likelyDisaster =
                        obj.optString(
                            "likelyDisaster",
                            DISASTER_UNKNOWN
                        ),


                    areaSeverity =
                        obj.optString(
                            "areaSeverity",
                            SEVERITY_UNKNOWN
                        )
                )

            } catch (_: Exception) {

                null
            }
        }
    }
}