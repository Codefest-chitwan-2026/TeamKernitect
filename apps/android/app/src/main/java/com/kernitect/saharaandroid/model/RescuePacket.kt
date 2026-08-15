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
        SEVERITY_UNKNOWN,

    /*
     * Present on responder acknowledgement, status, and location
     * packets. This always points to the original citizen SOS ID.
     */
    val incidentId: String? = null,

    val rescueStatus: String? = null,

    val responderAccuracyMeters: Float? = null
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

            incidentId?.let {
                put("incidentId", it)
            }

            rescueStatus?.let {
                put("status", it)
            }

            responderAccuracyMeters?.let {
                put("responderAccuracyMeters", it)
            }

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

        const val TYPE_SOS_ACK =
            "SOS_ACK"

        const val TYPE_RESCUE_STATUS =
            "RESCUE_STATUS"

        const val TYPE_RESPONDER_LOCATION =
            "RESPONDER_LOCATION"


        const val STATUS_RESPONDER_RECEIVED =
            "RESPONDER_RECEIVED"

        const val STATUS_ON_THE_WAY =
            "ON_THE_WAY"

        const val STATUS_ARRIVED =
            "ARRIVED"

        const val STATUS_RESCUED =
            "RESCUED"


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

            timestamp: Long =
                System.currentTimeMillis(),

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
                    timestamp,

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

            explanation: String,

            timestamp: Long =
                System.currentTimeMillis()

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
                    timestamp,

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
         * Responder-side helpers. These create real mesh packets;
         * the citizen timeline changes only after receiving one.
         */
        fun createResponderStatus(
            incidentId: String,
            status: String,
            timestamp: Long = System.currentTimeMillis()
        ): RescuePacket {

            val packetType =
                if (status == STATUS_RESPONDER_RECEIVED) {
                    TYPE_SOS_ACK
                } else {
                    TYPE_RESCUE_STATUS
                }

            return RescuePacket(
                id = UUID.randomUUID().toString(),
                type = packetType,
                latitude = 0.0,
                longitude = 0.0,
                timestamp = timestamp,
                hopCount = 0,
                ttl = DEFAULT_TTL,
                priority = PRIORITY_HIGH,
                message = status,
                incidentId = incidentId,
                rescueStatus = status
            )
        }


        fun createResponderLocation(
            incidentId: String,
            latitude: Double,
            longitude: Double,
            accuracyMeters: Float,
            timestamp: Long = System.currentTimeMillis()
        ): RescuePacket {

            return RescuePacket(
                id = UUID.randomUUID().toString(),
                type = TYPE_RESPONDER_LOCATION,
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp,
                hopCount = 0,
                ttl = DEFAULT_TTL,
                priority = PRIORITY_HIGH,
                message = "Responder location update",
                incidentId = incidentId,
                responderAccuracyMeters = accuracyMeters
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
                        ),

                    incidentId =
                        obj.optString(
                            "incidentId"
                        ).takeIf {
                            it.isNotBlank()
                        },

                    rescueStatus =
                        obj.optString(
                            "status",
                            obj.optString("rescueStatus")
                        ).takeIf {
                            it.isNotBlank()
                        },

                    responderAccuracyMeters =
                        if (
                            (
                                    obj.has("responderAccuracyMeters") &&
                                    !obj.isNull("responderAccuracyMeters")
                                    ) ||
                            (
                                    obj.has("accuracyMeters") &&
                                    !obj.isNull("accuracyMeters")
                                    )
                        ) {
                            obj.optDouble(
                                "responderAccuracyMeters",
                                obj.optDouble("accuracyMeters")
                            ).toFloat()
                        } else {
                            null
                        }
                )

            } catch (_: Exception) {

                null
            }
        }
    }
}
