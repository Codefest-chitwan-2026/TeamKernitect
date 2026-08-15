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
    val message: String
) {

    fun toJson(): String =
        JSONObject().apply {

            put("id", id)
            put("type", type)

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

        }.toString()

    fun canRelay(): Boolean =
        hopCount < ttl

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
         * Big red SOS button.
         */
        fun createCriticalSos(
            latitude: Double,
            longitude: Double
        ): RescuePacket {

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
                    "Critical emergency SOS"
            )
        }

        /*
         * Non-emergency help form.
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
                        )
            )
        }

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
                     * Defaults make this compatible
                     * with any older packets that
                     * don't contain these fields.
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
                        )
                )

            } catch (_: Exception) {

                null
            }
        }
    }
}