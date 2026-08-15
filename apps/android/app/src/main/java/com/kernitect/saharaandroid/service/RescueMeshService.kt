package com.kernitect.saharaandroid.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kernitect.saharaandroid.MainActivity
import com.kernitect.saharaandroid.R
import com.kernitect.saharaandroid.ble.AppRequirements
import com.kernitect.saharaandroid.data.local.SaharaDatabase
import com.kernitect.saharaandroid.data.local.dao.IncidentDao
import com.kernitect.saharaandroid.data.local.dao.TrackingEventDao
import com.kernitect.saharaandroid.data.local.entity.IncidentEntity
import com.kernitect.saharaandroid.data.local.entity.TrackingEventEntity
import com.kernitect.saharaandroid.data.local.entity.TrackingEventType
import com.kernitect.saharaandroid.mesh.MeshEngine
import com.kernitect.saharaandroid.model.ReceivedAlert
import com.kernitect.saharaandroid.model.RescuePacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

        private const val EXTRA_REQUEST_CREATED_AT =
            "requestCreatedAt"

        private const val EXTRA_LOCATION_ATTACHED_AT =
            "locationAttachedAt"


        /*
         * =========================================
         * NOTIFICATIONS
         * =========================================
         */

        private const val RELAY_CHANNEL_ID =
            "sahara_rescuemesh_service"

        private const val EMERGENCY_CHANNEL_ID =
            "sahara_emergency_packets"

        private const val TRACKING_CHANNEL_ID =
            "sahara_rescue_tracking"

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

            areaSeverity: String,

            requestCreatedAt: Long,

            locationAttachedAt: Long

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

                    putExtra(
                        EXTRA_REQUEST_CREATED_AT,
                        requestCreatedAt
                    )

                    putExtra(
                        EXTRA_LOCATION_ATTACHED_AT,
                        locationAttachedAt
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

            explanation: String,

            requestCreatedAt: Long,

            locationAttachedAt: Long

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

                    putExtra(
                        EXTRA_REQUEST_CREATED_AT,
                        requestCreatedAt
                    )

                    putExtra(
                        EXTRA_LOCATION_ATTACHED_AT,
                        locationAttachedAt
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

    private lateinit var incidentDao:
            IncidentDao

    private lateinit var trackingEventDao:
            TrackingEventDao

    private val locallyOriginatedPacketIds =
        ConcurrentHashMap.newKeySet<String>()


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
        val database =
            SaharaDatabase
                .getInstance(
                    applicationContext
                )

        incidentDao =
            database.incidentDao()

        trackingEventDao =
            database.trackingEventDao()


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
                    handleReceivedPacket(
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

                    persistSuccessfulLocalRelay(
                        packet
                    )


                    if (
                        packet.type == RescuePacket.TYPE_SOS
                    ) {
                        MeshServiceState
                            .sentPackets
                            .tryEmit(
                                packet
                            )
                    }
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


                val requestCreatedAt =
                    intent.getLongExtra(
                        EXTRA_REQUEST_CREATED_AT,
                        System.currentTimeMillis()
                    )

                val locationAttachedAt =
                    intent.getLongExtra(
                        EXTRA_LOCATION_ATTACHED_AT,
                        requestCreatedAt
                    )

                val packet =
                    meshEngine.originateSos(

                    latitude =
                        latitude,

                    longitude =
                        longitude,

                    timestamp =
                        requestCreatedAt,

                    likelyDisaster =
                        likelyDisaster,

                    areaSeverity =
                        areaSeverity
                )

                persistLocalRequest(
                    packet = packet,
                    locationAttachedAt = locationAttachedAt
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


                val requestCreatedAt =
                    intent.getLongExtra(
                        EXTRA_REQUEST_CREATED_AT,
                        System.currentTimeMillis()
                    )

                val locationAttachedAt =
                    intent.getLongExtra(
                        EXTRA_LOCATION_ATTACHED_AT,
                        requestCreatedAt
                    )

                val packet =
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
                        ) ?: "",

                    timestamp =
                        requestCreatedAt
                )

                persistLocalRequest(
                    packet = packet,
                    locationAttachedAt = locationAttachedAt
                )
            }


            ACTION_CANCEL -> {

                if (
                    meshStarted
                ) {

                    val cancelledPacket =
                        meshEngine
                        .cancelPendingForward()

                    cancelledPacket?.let {
                        discardCancelledUnrelayedRequest(it)
                    }
                }
            }
        }


        /*
         * Ask Android to recreate the service
         * after ordinary process reclamation.
         */
        return START_STICKY
    }


    private fun persistLocalRequest(
        packet: RescuePacket,
        locationAttachedAt: Long
    ) {

        locallyOriginatedPacketIds.add(
            packet.id
        )

        serviceScope.launch {

            incidentDao.insertIncident(
                IncidentEntity(
                    id = packet.id,
                    packetJson = packet.toJson(),
                    receivedAt = packet.timestamp,
                    isRead = true,
                    isLocalOrigin = true
                )
            )

            trackingEventDao.insertEvent(
                TrackingEventEntity(
                    id = UUID.randomUUID().toString(),
                    incidentId = packet.id,
                    type = TrackingEventType.SOS_CREATED.name,
                    title = if (
                        packet.priority == RescuePacket.PRIORITY_CRITICAL
                    ) {
                        "SOS Created"
                    } else {
                        "Help Request Created"
                    },
                    description = "Emergency request created.",
                    timestamp = packet.timestamp
                )
            )

            trackingEventDao.insertEvent(
                TrackingEventEntity(
                    id = UUID.randomUUID().toString(),
                    incidentId = packet.id,
                    type = TrackingEventType.LOCATION_ATTACHED.name,
                    title = "Location Attached",
                    description = String.format(
                        Locale.getDefault(),
                        "Location %.5f, %.5f attached.",
                        packet.latitude,
                        packet.longitude
                    ),
                    timestamp = locationAttachedAt
                )
            )
        }
    }


    private fun persistSuccessfulLocalRelay(
        packet: RescuePacket
    ) {

        serviceScope.launch {

            val isLocalRequest =
                locallyOriginatedPacketIds.contains(packet.id) ||
                        incidentDao.findLocalIncident(packet.id) != null

            if (
                isLocalRequest &&
                packet.type == RescuePacket.TYPE_SOS
            ) {

                trackingEventDao.insertEvent(
                    TrackingEventEntity(
                        id = UUID.randomUUID().toString(),
                        incidentId = packet.id,
                        type = TrackingEventType.SOS_RELAYED.name,
                        title = "Relayed Through RESCUEMESH",
                        description = "Successfully forwarded to a nearby node.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }


    private fun discardCancelledUnrelayedRequest(
        packet: RescuePacket
    ) {
        serviceScope.launch {
            val wasRelayed = trackingEventDao.hasEvent(
                packet.id,
                TrackingEventType.SOS_RELAYED.name
            )

            if (
                !wasRelayed &&
                incidentDao.findLocalIncident(packet.id) != null
            ) {
                trackingEventDao.deleteEventsForIncident(packet.id)
                incidentDao.deleteLocalIncident(packet.id)
                locallyOriginatedPacketIds.remove(packet.id)
            }
        }
    }


    private fun handleReceivedPacket(
        packet: RescuePacket
    ) {

        if (
            packet.type != RescuePacket.TYPE_SOS
        ) {
            processResponderPacket(
                packet
            )
            return
        }

        val receivedAt =
            System.currentTimeMillis()

        val alert =
            ReceivedAlert(
                packet = packet,
                receivedAt = receivedAt
            )

        serviceScope.launch {
            incidentDao.insertIncident(
                IncidentEntity(
                    id = packet.id,
                    packetJson = packet.toJson(),
                    receivedAt = receivedAt,
                    isRead = false,
                    isLocalOrigin = false
                )
            )
        }

        MeshServiceState
            .incomingAlerts
            .tryEmit(alert)

        showEmergencyNotification(
            packet
        )
    }


    private fun processResponderPacket(
        packet: RescuePacket
    ) {

        val incidentId =
            packet.incidentId
                ?: return

        serviceScope.launch {

            val incident =
                incidentDao.findLocalIncident(
                    incidentId
                ) ?: return@launch

            when (packet.type) {

                RescuePacket.TYPE_SOS_ACK -> {
                    if (
                        packet.rescueStatus == null ||
                        packet.rescueStatus == RescuePacket.STATUS_RESPONDER_RECEIVED
                    ) {
                        insertResponderStatusEvent(
                            incidentId = incidentId,
                            status = RescuePacket.STATUS_RESPONDER_RECEIVED,
                            timestamp = packet.timestamp
                        )
                    }
                }

                RescuePacket.TYPE_RESCUE_STATUS -> {
                    packet.rescueStatus?.let {
                        insertResponderStatusEvent(
                            incidentId = incidentId,
                            status = it,
                            timestamp = packet.timestamp
                        )
                    }
                }

                RescuePacket.TYPE_RESPONDER_LOCATION -> {
                    processResponderLocation(
                        incident = incident,
                        packet = packet
                    )
                }
            }
        }
    }


    private suspend fun insertResponderStatusEvent(
        incidentId: String,
        status: String,
        timestamp: Long
    ) {

        val presentation =
            when (status) {
                RescuePacket.STATUS_RESPONDER_RECEIVED ->
                    Triple(
                        TrackingEventType.RESPONDER_RECEIVED,
                        "Responder Received SOS",
                        "Your emergency request reached a rescue team."
                    )

                RescuePacket.STATUS_ON_THE_WAY ->
                    Triple(
                        TrackingEventType.ON_THE_WAY,
                        "Rescue Team On The Way",
                        "A rescue team is travelling to your location."
                    )

                RescuePacket.STATUS_ARRIVED ->
                    Triple(
                        TrackingEventType.ARRIVED,
                        "Responder Arrived",
                        "The rescue team reported that it has arrived."
                    )

                RescuePacket.STATUS_RESCUED ->
                    Triple(
                        TrackingEventType.RESCUED,
                        "Rescue Completed",
                        "The rescue team marked this request as rescued."
                    )

                else -> return
            }

        trackingEventDao.insertEvent(
            TrackingEventEntity(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                type = presentation.first.name,
                title = presentation.second,
                description = presentation.third,
                timestamp = timestamp
            )
        )
    }


    private suspend fun processResponderLocation(
        incident: IncidentEntity,
        packet: RescuePacket
    ) {

        val citizenPacket =
            RescuePacket.fromJson(
                incident.packetJson
            ) ?: return

        val results =
            FloatArray(1)

        Location.distanceBetween(
            citizenPacket.latitude,
            citizenPacket.longitude,
            packet.latitude,
            packet.longitude,
            results
        )

        val distanceMeters =
            results[0]

        MeshServiceState.responderDistances.value =
            MeshServiceState.responderDistances.value +
                    (
                            incident.id to
                                    MeshServiceState.ResponderDistance(
                                        incidentId = incident.id,
                                        distanceMeters = distanceMeters,
                                        responderAccuracyMeters = packet.responderAccuracyMeters,
                                        updatedAt = packet.timestamp
                                    )
                            )

        val rescueIsActive =
            !trackingEventDao.hasEvent(
                incident.id,
                TrackingEventType.RESCUED.name
            )

        val responderIsOnTheWay =
            trackingEventDao.hasEvent(
                incident.id,
                TrackingEventType.ON_THE_WAY.name
            )

        val accuracyIsUsable =
            packet.responderAccuracyMeters != null &&
                    packet.responderAccuracyMeters <= 40f

        if (
            rescueIsActive &&
            responderIsOnTheWay &&
            accuracyIsUsable &&
            distanceMeters <= 80f
        ) {

            val inserted =
                trackingEventDao.insertEvent(
                    TrackingEventEntity(
                        id = UUID.randomUUID().toString(),
                        incidentId = incident.id,
                        type = TrackingEventType.RESPONDER_NEARBY.name,
                        title = "Responder Nearby",
                        description = "Rescue team is approximately ${distanceMeters.toInt()} m away.",
                        timestamp = packet.timestamp,
                        distanceMeters = distanceMeters.toDouble()
                    )
                )

            /*
             * The database uniqueness constraint is the notification gate.
             * A fluctuating GPS reading can never notify twice for an incident.
             */
            if (
                inserted != -1L
            ) {
                showResponderNearbyNotification(
                    incidentId = incident.id,
                    distanceMeters = distanceMeters
                )
            }
        }
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


    @SuppressLint("MissingPermission")
    private fun showResponderNearbyNotification(
        incidentId: String,
        distanceMeters: Float
    ) {

        if (
            !AppRequirements.hasNotificationPermission(this)
        ) {
            return
        }

        val roundedDistance =
            distanceMeters.toInt()

        val body =
            "Your rescue team is approximately $roundedDistance meters away. " +
                    "Stay where you are if it is safe to do so."

        val notification =
            NotificationCompat.Builder(
                this,
                TRACKING_CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Rescue Team Nearby")
                .setContentText(body)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(body)
                )
                .setContentIntent(createOpenAppIntent())
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

        NotificationManagerCompat.from(this).notify(
            incidentId.hashCode().and(0x7fffffff) xor 0x52455343,
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


        val trackingChannel =
            NotificationChannel(
                TRACKING_CHANNEL_ID,
                "Rescue Tracking",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Important status updates for your active rescue request."
                enableVibration(true)
            }


        manager.createNotificationChannel(
            relayChannel
        )


        manager.createNotificationChannel(
            emergencyChannel
        )


        manager.createNotificationChannel(
            trackingChannel
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
