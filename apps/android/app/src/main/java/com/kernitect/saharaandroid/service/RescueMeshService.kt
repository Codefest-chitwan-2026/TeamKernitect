package com.kernitect.saharaandroid.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kernitect.saharaandroid.MainActivity
import com.kernitect.saharaandroid.R
import com.kernitect.saharaandroid.ble.AppRequirements
import com.kernitect.saharaandroid.data.local.SaharaDatabase
import com.kernitect.saharaandroid.data.local.entity.IncidentEntity
import com.kernitect.saharaandroid.mesh.MeshEngine
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.model.RescuePacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class RescueMeshService : Service() {


    companion object {

        /*
         * =========================================
         * INTENT ACTIONS
         * =========================================
         */

        private const val ACTION_START =
            "com.kernitect.saharaandroid.mesh.START"

        private const val ACTION_SEND_CRITICAL =
            "com.kernitect.saharaandroid.mesh.SEND_CRITICAL"

        private const val ACTION_SEND_HELP =
            "com.kernitect.saharaandroid.mesh.SEND_HELP"

        private const val ACTION_CANCEL =
            "com.kernitect.saharaandroid.mesh.CANCEL"


        /*
         * =========================================
         * EXTRAS
         * =========================================
         */

        private const val EXTRA_LATITUDE =
            "latitude"

        private const val EXTRA_LONGITUDE =
            "longitude"

        private const val EXTRA_LIKELY_DISASTER =
            "likelyDisaster"

        private const val EXTRA_AREA_SEVERITY =
            "areaSeverity"

        private const val EXTRA_DISASTER_TYPE =
            "disasterType"

        private const val EXTRA_PEOPLE_COUNT =
            "peopleCount"

        private const val EXTRA_EXPLANATION =
            "explanation"


        /*
         * =========================================
         * NOTIFICATIONS
         * =========================================
         */

        private const val RELAY_CHANNEL_ID =
            "sahara_rescuemesh_service"

        private const val EMERGENCY_CHANNEL_ID =
            "sahara_emergency_packets"

        private const val SERVICE_NOTIFICATION_ID =
            1001


        /*
         * =========================================
         * PUBLIC COMMANDS
         * =========================================
         */

        fun start(
            context: Context
        ) {

            val intent =
                Intent(
                    context,
                    RescueMeshService::class.java
                ).apply {

                    action =
                        ACTION_START
                }


            startForegroundCommand(
                context,
                intent
            )
        }


        fun sendCriticalSos(

            context: Context,

            latitude: Double,

            longitude: Double,

            likelyDisaster: String,

            areaSeverity: String

        ) {

            val intent =
                Intent(
                    context,
                    RescueMeshService::class.java
                ).apply {

                    action =
                        ACTION_SEND_CRITICAL


                    putExtra(
                        EXTRA_LATITUDE,
                        latitude
                    )

                    putExtra(
                        EXTRA_LONGITUDE,
                        longitude
                    )

                    putExtra(
                        EXTRA_LIKELY_DISASTER,
                        likelyDisaster
                    )

                    putExtra(
                        EXTRA_AREA_SEVERITY,
                        areaSeverity
                    )
                }


            startForegroundCommand(
                context,
                intent
            )
        }


        fun sendHelpRequest(

            context: Context,

            latitude: Double,

            longitude: Double,

            disasterType: String,

            peopleCount: String,

            explanation: String

        ) {

            val intent =
                Intent(
                    context,
                    RescueMeshService::class.java
                ).apply {

                    action =
                        ACTION_SEND_HELP


                    putExtra(
                        EXTRA_LATITUDE,
                        latitude
                    )

                    putExtra(
                        EXTRA_LONGITUDE,
                        longitude
                    )

                    putExtra(
                        EXTRA_DISASTER_TYPE,
                        disasterType
                    )

                    putExtra(
                        EXTRA_PEOPLE_COUNT,
                        peopleCount
                    )

                    putExtra(
                        EXTRA_EXPLANATION,
                        explanation
                    )
                }


            startForegroundCommand(
                context,
                intent
            )
        }


        fun cancelPending(
            context: Context
        ) {

            val intent =
                Intent(
                    context,
                    RescueMeshService::class.java
                ).apply {

                    action =
                        ACTION_CANCEL
                }


            startForegroundCommand(
                context,
                intent
            )
        }


        private fun startForegroundCommand(

            context: Context,

            intent: Intent

        ) {

            ContextCompat.startForegroundService(
                context,
                intent
            )
        }
    }


    /*
     * =============================================
     * SERVICE STATE
     * =============================================
     */

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )


    private lateinit var meshEngine:
            MeshEngine


    private var meshStarted =
        false


    /*
     * =============================================
     * SERVICE CREATED
     * =============================================
     */

    override fun onCreate() {

        super.onCreate()


        createNotificationChannels()


        /*
         * Must happen immediately when this service
         * is created as a foreground service.
         */
        startRelayForeground()


        /*
         * Database belongs to application process,
         * so it remains available without Activity.
         */
        val incidentDao =
            SaharaDatabase
                .getInstance(
                    applicationContext
                )
                .incidentDao()


        /*
         * =========================================
         * THE ONLY MESH ENGINE
         * =========================================
         */
        meshEngine =
            MeshEngine(

                context =
                    applicationContext,


                /*
                 * Status → UI when open.
                 */
                onStatusChanged = {
                        status ->

                    MeshServiceState
                        .status
                        .value =
                        status
                },


                /*
                 * =================================
                 * PACKET RECEIVED
                 * =================================
                 */
                onPacketReceived = {
                        packet ->


                    val receivedAt =
                        System.currentTimeMillis()


                    val alert =
                        ReceivedAlert(

                            packet =
                                packet,

                            receivedAt =
                                receivedAt
                        )


                    /*
                     * Persist even when no Activity
                     * exists.
                     */
                    serviceScope.launch {

                        incidentDao.insertIncident(

                            IncidentEntity(

                                id =
                                    packet.id,

                                packetJson =
                                    packet.toJson(),

                                receivedAt =
                                    receivedAt,

                                isRead =
                                    false
                            )
                        )
                    }


                    /*
                     * In-app popup if UI is alive.
                     */
                    MeshServiceState
                        .incomingAlerts
                        .tryEmit(
                            alert
                        )


                    /*
                     * Android system notification
                     * works independently from Compose.
                     */
                    showEmergencyNotification(
                        packet
                    )
                },


                /*
                 * =================================
                 * PACKET SENT
                 * =================================
                 */
                onPacketSent = {
                        packet ->


                    MeshServiceState
                        .sentPackets
                        .tryEmit(
                            packet
                        )
                }
            )


        MeshServiceState
            .running
            .value =
            true


        ensureMeshStarted()
    }


    /*
     * =============================================
     * COMMANDS FROM MAIN ACTIVITY
     * =============================================
     */

    override fun onStartCommand(

        intent: Intent?,

        flags: Int,

        startId: Int

    ): Int {


        ensureMeshStarted()


        when (
            intent?.action
        ) {

            ACTION_START -> {

                /*
                 * Nothing more needed.
                 *
                 * ensureMeshStarted() above handles it.
                 */
            }


            ACTION_SEND_CRITICAL -> {

                if (
                    !meshStarted
                ) {

                    MeshServiceState
                        .status
                        .value =
                        "RESCUEMESH is not ready"

                    return START_STICKY
                }


                val latitude =
                    intent.getDoubleExtra(
                        EXTRA_LATITUDE,
                        Double.NaN
                    )


                val longitude =
                    intent.getDoubleExtra(
                        EXTRA_LONGITUDE,
                        Double.NaN
                    )


                if (
                    latitude.isNaN() ||
                    longitude.isNaN()
                ) {

                    MeshServiceState
                        .status
                        .value =
                        "Cannot send SOS: invalid location"

                    return START_STICKY
                }


                val likelyDisaster =
                    intent.getStringExtra(
                        EXTRA_LIKELY_DISASTER
                    )
                        ?: RescuePacket.DISASTER_UNKNOWN


                val areaSeverity =
                    intent.getStringExtra(
                        EXTRA_AREA_SEVERITY
                    )
                        ?: RescuePacket.SEVERITY_UNKNOWN


                meshEngine.originateSos(

                    latitude =
                        latitude,

                    longitude =
                        longitude,

                    likelyDisaster =
                        likelyDisaster,

                    areaSeverity =
                        areaSeverity
                )
            }


            ACTION_SEND_HELP -> {

                if (
                    !meshStarted
                ) {

                    MeshServiceState
                        .status
                        .value =
                        "RESCUEMESH is not ready"

                    return START_STICKY
                }


                val latitude =
                    intent.getDoubleExtra(
                        EXTRA_LATITUDE,
                        Double.NaN
                    )


                val longitude =
                    intent.getDoubleExtra(
                        EXTRA_LONGITUDE,
                        Double.NaN
                    )


                if (
                    latitude.isNaN() ||
                    longitude.isNaN()
                ) {

                    MeshServiceState
                        .status
                        .value =
                        "Cannot send help request: invalid location"

                    return START_STICKY
                }


                meshEngine.originateHelpRequest(

                    latitude =
                        latitude,

                    longitude =
                        longitude,

                    disasterType =
                        intent.getStringExtra(
                            EXTRA_DISASTER_TYPE
                        ) ?: "OTHER",

                    peopleCount =
                        intent.getStringExtra(
                            EXTRA_PEOPLE_COUNT
                        ) ?: "Unknown",

                    explanation =
                        intent.getStringExtra(
                            EXTRA_EXPLANATION
                        ) ?: ""
                )
            }


            ACTION_CANCEL -> {

                if (
                    meshStarted
                ) {

                    meshEngine
                        .cancelPendingForward()
                }
            }
        }


        /*
         * Ask Android to recreate the service
         * after ordinary process reclamation.
         */
        return START_STICKY
    }


    /*
     * =============================================
     * START BLE
     * =============================================
     */

    private fun ensureMeshStarted() {

        if (
            meshStarted
        ) {

            return
        }


        if (
            !AppRequirements
                .hasMeshPermissions(
                    this
                )
        ) {

            MeshServiceState
                .status
                .value =
                "Nearby device permission required"

            return
        }


        if (
            !AppRequirements
                .isBluetoothEnabled(
                    this
                )
        ) {

            MeshServiceState
                .status
                .value =
                "Turn on Bluetooth"

            return
        }


        meshEngine.start()


        meshStarted =
            true


        MeshServiceState
            .status
            .value =
            "RESCUEMESH relay active"
    }


    /*
     * =============================================
     * FOREGROUND NOTIFICATION
     * =============================================
     */

    private fun startRelayForeground() {

        val notification =

            NotificationCompat
                .Builder(
                    this,
                    RELAY_CHANNEL_ID
                )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    "SAHARA RESCUEMESH active"
                )
                .setContentText(
                    "Listening for nearby emergency packets"
                )
                .setContentIntent(
                    createOpenAppIntent()
                )
                .setOngoing(
                    true
                )
                .setOnlyAlertOnce(
                    true
                )
                .setCategory(
                    NotificationCompat.CATEGORY_SERVICE
                )
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .build()


        ServiceCompat.startForeground(

            this,

            SERVICE_NOTIFICATION_ID,

            notification,

            ServiceInfo
                .FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
    }


    /*
     * =============================================
     * RECEIVED SOS NOTIFICATION
     * =============================================
     */

    @SuppressLint(
        "MissingPermission"
    )
    private fun showEmergencyNotification(
        packet: RescuePacket
    ) {

        /*
         * Android 13+:
         *
         * BLE service can continue even if notification
         * permission was denied, but we cannot post the
         * normal emergency notification.
         */
        if (
            !AppRequirements
                .hasNotificationPermission(
                    this
                )
        ) {

            return
        }


        val critical =
            packet.priority ==
                    RescuePacket.PRIORITY_CRITICAL


        val title =

            if (
                critical
            ) {

                "Emergency SOS Received"

            } else {

                "Help Request Received"
            }


        val body =

            if (
                critical
            ) {

                buildCriticalNotificationText(
                    packet
                )

            } else {

                packet.message
            }


        val notification =

            NotificationCompat
                .Builder(
                    this,
                    EMERGENCY_CHANNEL_ID
                )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    body
                )
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(
                            body
                        )
                )
                .setContentIntent(
                    createOpenAppIntent()
                )
                .setAutoCancel(
                    true
                )
                .setCategory(
                    NotificationCompat.CATEGORY_MESSAGE
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .build()


        val notificationId =
            packet.id
                .hashCode()
                .and(
                    0x7fffffff
                )


        NotificationManagerCompat
            .from(
                this
            )
            .notify(
                notificationId,
                notification
            )
    }


    private fun buildCriticalNotificationText(
        packet: RescuePacket
    ): String {

        return if (
            packet.likelyDisaster !=
            RescuePacket.DISASTER_UNKNOWN
        ) {

            "Critical SOS received through RESCUEMESH. " +
                    "Likely disaster: ${
                        prettyValue(
                            packet.likelyDisaster
                        )
                    }. " +
                    "Area severity: ${
                        prettyValue(
                            packet.areaSeverity
                        )
                    }."

        } else {

            "Critical SOS received through RESCUEMESH."
        }
    }


    private fun prettyValue(
        value: String
    ): String {

        return value
            .lowercase(
                Locale.getDefault()
            )
            .replace(
                "_",
                " "
            )
            .split(
                " "
            )
            .joinToString(
                " "
            ) {
                    word ->

                word.replaceFirstChar {

                    if (
                        it.isLowerCase()
                    ) {

                        it.titlecase(
                            Locale.getDefault()
                        )

                    } else {

                        it.toString()
                    }
                }
            }
    }


    /*
     * =============================================
     * NOTIFICATION CLICK
     * =============================================
     */

    private fun createOpenAppIntent():
            PendingIntent {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }


        return PendingIntent.getActivity(

            this,

            0,

            intent,

            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }


    /*
     * =============================================
     * CHANNELS
     * =============================================
     */

    private fun createNotificationChannels() {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )


        /*
         * Quiet permanent relay notification.
         */
        val relayChannel =
            NotificationChannel(

                RELAY_CHANNEL_ID,

                "RESCUEMESH Relay",

                NotificationManager.IMPORTANCE_LOW

            ).apply {

                description =
                    "Shows when SAHARA is listening for nearby emergency packets."
            }


        /*
         * Loud/high-priority incoming SOS channel.
         */
        val emergencyChannel =
            NotificationChannel(

                EMERGENCY_CHANNEL_ID,

                "Emergency SOS Alerts",

                NotificationManager.IMPORTANCE_HIGH

            ).apply {

                description =
                    "Notifications for emergency packets received through RESCUEMESH."

                enableVibration(
                    true
                )
            }


        manager.createNotificationChannel(
            relayChannel
        )


        manager.createNotificationChannel(
            emergencyChannel
        )
    }


    /*
     * =============================================
     * CLEANUP
     * =============================================
     */

    override fun onDestroy() {

        if (
            ::meshEngine.isInitialized
        ) {

            meshEngine.stop()
        }


        meshStarted =
            false


        MeshServiceState
            .running
            .value =
            false


        MeshServiceState
            .status
            .value =
            "RESCUEMESH inactive"


        serviceScope.cancel()


        super.onDestroy()
    }


    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}