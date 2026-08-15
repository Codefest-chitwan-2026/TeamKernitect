package com.kernitect.sahararesponder

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kernitect.sahararesponder.ble.ResponderBleAdvertiser
import com.kernitect.sahararesponder.ble.ResponderBleServer
import com.kernitect.sahararesponder.model.RescuePacket
import com.kernitect.sahararesponder.model.ResponderIncident
import com.kernitect.sahararesponder.model.RescueAckPacket
import com.kernitect.sahararesponder.model.ResponderTeamProfile
import com.kernitect.sahararesponder.model.ResponderRegistration
import com.kernitect.sahararesponder.model.ResponderRegistrationStatus
import com.kernitect.sahararesponder.model.RescueClaimPacket
import com.kernitect.sahararesponder.model.IncidentClaim
import com.kernitect.sahararesponder.model.IncidentOwnership
import com.kernitect.sahararesponder.model.ownershipOf
import com.kernitect.sahararesponder.model.RescueLifecycle
import com.kernitect.sahararesponder.model.RescueStatusEvent
import com.kernitect.sahararesponder.model.RescueStatusPacket
import com.kernitect.sahararesponder.model.latestStatusByTeam
import com.kernitect.sahararesponder.model.RescueLifecycleEvent
import com.kernitect.sahararesponder.model.recordLifecycleEvent
import com.kernitect.sahararesponder.identity.ResponderIdentityStore
import com.kernitect.sahararesponder.network.ResponderApiClient
import com.kernitect.sahararesponder.mesh.AckRecord
import com.kernitect.sahararesponder.mesh.AckSendState
import com.kernitect.sahararesponder.mesh.ClaimRecord
import com.kernitect.sahararesponder.mesh.StatusRecord
import com.kernitect.sahararesponder.mesh.ResponderMeshSender
import com.kernitect.sahararesponder.location.ResponderLocation
import com.kernitect.sahararesponder.location.ResponderLocationProvider
import com.kernitect.sahararesponder.ui.components.ResponderBottomBar
import com.kernitect.sahararesponder.ui.navigation.ResponderDestination
import com.kernitect.sahararesponder.ui.screens.active.ActiveRescuesScreen
import com.kernitect.sahararesponder.ui.screens.home.ResponderHomeScreen
import com.kernitect.sahararesponder.ui.screens.incident.IncidentDetailsScreen
import com.kernitect.sahararesponder.ui.screens.map.ResponderMapScreen
import com.kernitect.sahararesponder.ui.screens.setup.PendingApprovalScreen
import com.kernitect.sahararesponder.ui.screens.setup.RejectedRegistrationScreen
import com.kernitect.sahararesponder.ui.screens.setup.ResponderRegistrationScreen
import com.kernitect.sahararesponder.ui.theme.SaharaResponderTheme
import org.osmdroid.config.Configuration
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private var meshStatus by mutableStateOf("Starting RESCUEMESH")
    private val incidents = mutableStateListOf<ResponderIncident>()
    private val seenPacketIds = mutableSetOf<String>()
    private val ackRecords = mutableStateMapOf<String, AckRecord>()
    private val claimRecords = mutableStateMapOf<String, ClaimRecord>()
    private val claimsByIncident = mutableStateMapOf<String, List<IncidentClaim>>()
    private val seenClaimIds = mutableSetOf<String>()
    private val statusRecords = mutableStateMapOf<String, StatusRecord>()
    private val statusesByIncident = mutableStateMapOf<String, List<RescueStatusEvent>>()
    private val seenStatusIds = mutableSetOf<String>()
    private val lifecycleEventsByIncident = mutableStateMapOf<String, List<RescueLifecycleEvent>>()
    private var receiverStarted = false
    private var activityStarted = false
    private var responderLocation by mutableStateOf<ResponderLocation?>(null)
    private var locationStatus by mutableStateOf("Responder location permission required")
    private var teamProfile by mutableStateOf<ResponderTeamProfile?>(null)
    private var identityError by mutableStateOf<String?>(null)
    private var registration by mutableStateOf<ResponderRegistration?>(null)
    private var registrationLoading by mutableStateOf(false)
    private var registrationError by mutableStateOf<String?>(null)
    private lateinit var bleServer: ResponderBleServer
    private lateinit var bleAdvertiser: ResponderBleAdvertiser
    private lateinit var locationProvider: ResponderLocationProvider
    private lateinit var meshSender: ResponderMeshSender
    private lateinit var identityStore: ResponderIdentityStore
    private val responderApiClient = ResponderApiClient()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (hasRequiredPermissions()) startReceiverIfReady() else {
            Log.w(TAG, "Bluetooth permission missing")
            meshStatus = "Bluetooth permission required"
        }
        ensureLocationPermission()
    }

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) {
            locationStatus = "Locating responder…"
            if (activityStarted) locationProvider.start()
        } else locationStatus = "Responder location permission required"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        Configuration.getInstance().userAgentValue = packageName
        identityStore = ResponderIdentityStore(applicationContext)
        teamProfile = identityStore.loadApprovedProfile()
        registration = identityStore.loadRegistration()
            ?: ResponderRegistration(deviceId = identityStore.getOrCreateDeviceId())
        bleAdvertiser = ResponderBleAdvertiser(applicationContext, ::postStatus)
        bleServer = ResponderBleServer(
            context = applicationContext,
            onMessageReceived = ::handlePacket,
            onStatusChanged = ::postStatus,
            onReady = { runOnUiThread { bleAdvertiser.start() } },
        )
        locationProvider = ResponderLocationProvider(
            context = applicationContext,
            onLocation = { location -> runOnUiThread {
                responderLocation = location
                locationStatus = "Responder located • ±${location.accuracyMeters.toInt()} m"
            } },
            onStatus = { status -> runOnUiThread { locationStatus = status } },
        )
        meshSender = ResponderMeshSender(applicationContext) { packet, state, failureReason ->
            runOnUiThread {
                if (packet.type == RescueClaimPacket.TYPE) {
                    val existing = claimRecords[packet.incidentId] ?: return@runOnUiThread
                    claimRecords[packet.incidentId] = existing.copy(state = state, failureReason = failureReason)
                } else if (packet.type == RescueStatusPacket.TYPE) {
                    val existing = statusRecords[packet.id] ?: return@runOnUiThread
                    statusRecords[packet.id] = existing.copy(state = state, failureReason = failureReason)
                    Log.i(TAG, if (state == AckSendState.SENT_TO_MESH) "Status written to mesh neighbor: ${packet.id}" else "Status ${state.name}: ${packet.id}")
                } else {
                    val existing = ackRecords[packet.incidentId] ?: return@runOnUiThread
                    ackRecords[packet.incidentId] = existing.copy(state = state, failureReason = failureReason)
                }
            }
        }
        setContent {
            SaharaResponderTheme {
                val configuredTeam = teamProfile
                if (configuredTeam == null) {
                    val account = registration ?: ResponderRegistration(identityStore.getOrCreateDeviceId())
                    when (account.status) {
                        ResponderRegistrationStatus.UNREGISTERED -> ResponderRegistrationScreen(
                            deviceId = account.deviceId,
                            initial = account,
                            loading = registrationLoading,
                            error = registrationError,
                            onSubmit = ::submitRegistration,
                        )
                        ResponderRegistrationStatus.PENDING -> PendingApprovalScreen(
                            registration = account,
                            loading = registrationLoading,
                            error = registrationError,
                            onCheckStatus = ::checkRegistrationStatus,
                        )
                        ResponderRegistrationStatus.REJECTED -> RejectedRegistrationScreen(
                            registration = account,
                            loading = registrationLoading,
                            error = registrationError,
                            onRetry = { registration = account.copy(status = ResponderRegistrationStatus.UNREGISTERED) },
                            onCheckStatus = ::checkRegistrationStatus,
                        )
                        ResponderRegistrationStatus.APPROVED -> ResponderRegistrationScreen(
                            deviceId = account.deviceId,
                            initial = account,
                            loading = registrationLoading,
                            error = "Approved response is missing official team information. Contact SAHARA command.",
                            onSubmit = ::submitRegistration,
                        )
                    }
                } else {
                var destination by remember { mutableStateOf(ResponderDestination.HOME) }
                var selectedIncident by remember { mutableStateOf<ResponderIncident?>(null) }
                var focusedMapIncident by remember { mutableStateOf<ResponderIncident?>(null) }
                BackHandler(enabled = focusedMapIncident != null) { focusedMapIncident = null }
                BackHandler(enabled = focusedMapIncident == null && selectedIncident != null) { selectedIncident = null }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (selectedIncident == null && focusedMapIncident == null) {
                            ResponderBottomBar(destination) { destination = it }
                        }
                    },
                ) { padding ->
                    val focused = focusedMapIncident
                    val selected = selectedIncident?.let { chosen -> incidents.firstOrNull { it.id == chosen.id } ?: chosen }
                    if (focused != null) {
                        ResponderMapScreen(
                            incidents = incidents,
                            responderLocation = responderLocation,
                            locationStatus = locationStatus,
                            focusedIncident = focused,
                            onBack = { focusedMapIncident = null },
                            modifier = Modifier.padding(padding),
                        )
                    } else if (selected != null) {
                        IncidentDetailsScreen(
                            incident = selected,
                            responderLocation = responderLocation,
                            locationStatus = locationStatus,
                            onBack = { selectedIncident = null },
                            onOpenMap = { focusedMapIncident = selected },
                            ackRecord = ackRecords[selected.id],
                            claimRecord = claimRecords[selected.id],
                            claims = claimsByIncident[selected.id].orEmpty(),
                            statusEvents = statusesByIncident[selected.id].orEmpty(),
                            statusRecord = latestStatusRecord(selected.id),
                            lifecycleEvents = lifecycleEventsByIncident[selected.id].orEmpty(),
                            teamProfile = configuredTeam,
                            identityError = identityError,
                            onAcceptRescue = {
                                val accepted = acceptIncident(selected)
                                selectedIncident = accepted
                            },
                            onRetryAck = { retryAck(selected.id) },
                            onRetryClaim = { retryClaim(selected.id) },
                            onAdvanceLifecycle = { next -> advanceLifecycle(selected, next) },
                            onRetryStatus = { retryStatus(selected.id) },
                            modifier = Modifier.padding(padding),
                        )
                    } else {
                        when (destination) {
                            ResponderDestination.HOME -> ResponderHomeScreen(
                                meshStatus = meshStatus,
                                incidents = incidents,
                                onViewDetails = { selectedIncident = it },
                                responderLocated = responderLocation != null,
                                onOpenMap = { destination = ResponderDestination.MAP },
                                teamProfile = configuredTeam,
                                claimsByIncident = claimsByIncident,
                                lifecycleEventsByIncident = lifecycleEventsByIncident,
                                modifier = Modifier.padding(padding),
                            )
                            ResponderDestination.MAP -> ResponderMapScreen(
                                incidents = incidents,
                                responderLocation = responderLocation,
                                locationStatus = locationStatus,
                                modifier = Modifier.padding(padding),
                            )
                            ResponderDestination.ACTIVE -> ActiveRescuesScreen(
                                incidents = incidents.filter {
                                    ownershipOf(claimsByIncident[it.id].orEmpty(), configuredTeam.teamId) in
                                        setOf(IncidentOwnership.CLAIMED_BY_ME, IncidentOwnership.CONFLICT) && it.status in ACTIVE_STATUSES
                                },
                                teamProfile = configuredTeam,
                                claimsByIncident = claimsByIncident,
                                lifecycleEventsByIncident = lifecycleEventsByIncident,
                                onViewDetails = { selectedIncident = it },
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                }
                }
            }
        }
        if (teamProfile != null) startOperationalServices()
    }

    private fun startOperationalServices() {
        val bluetoothDialogNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasAllBluetoothPermissions()
        ensurePermissionsAndStart()
        if (!bluetoothDialogNeeded) ensureLocationPermission()
    }

    private fun submitRegistration(request: ResponderRegistration) {
        if (registrationLoading) return
        registrationLoading = true
        registrationError = null
        responderApiClient.register(request) { result ->
            registrationLoading = false
            result.onSuccess(::applyRegistration).onFailure {
                registrationError = "Unable to reach SAHARA registration service. ${it.message.orEmpty()}"
            }
        }
    }

    private fun checkRegistrationStatus() {
        val current = registration ?: return
        if (registrationLoading) return
        registrationLoading = true
        registrationError = null
        responderApiClient.checkStatus(current.deviceId) { result ->
            registrationLoading = false
            result.onSuccess(::applyRegistration).onFailure {
                registrationError = "Unable to reach SAHARA registration service. ${it.message.orEmpty()}"
            }
        }
    }

    private fun applyRegistration(account: ResponderRegistration) {
        identityStore.saveRegistration(account)
        registration = account
        val approved = account.approvedProfile()
        if (account.status == ResponderRegistrationStatus.APPROVED && approved == null) {
            registrationError = "Approval response did not include a complete official team assignment."
            return
        }
        if (approved != null) {
            teamProfile = approved
            identityError = null
            startOperationalServices()
        }
    }

    override fun onStart() {
        super.onStart()
        activityStarted = true
        if (teamProfile != null && ::locationProvider.isInitialized && hasLocationPermission()) locationProvider.start()
    }

    override fun onStop() {
        activityStarted = false
        if (::locationProvider.isInitialized) locationProvider.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (teamProfile != null && ::bleServer.isInitialized && hasRequiredPermissions()) startReceiverIfReady()
    }

    private fun ensurePermissionsAndStart() {
        if (hasRequiredPermissions()) startReceiverIfReady()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasAllBluetoothPermissions()) {
            meshStatus = "Bluetooth permission required"
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN))
        }
    }

    private fun ensureLocationPermission() {
        if (hasLocationPermission()) {
            locationStatus = "Locating responder…"
            if (activityStarted) locationProvider.start()
        } else {
            locationStatus = "Responder location permission required"
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun startReceiverIfReady() {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null) {
            meshStatus = "Bluetooth unavailable"
            return
        }
        if (!adapter.isEnabled) {
            meshStatus = "Bluetooth disabled"
            return
        }
        if (receiverStarted) return
        receiverStarted = true
        meshStatus = "Starting RESCUEMESH"
        bleServer.start()
    }

    private fun handlePacket(raw: String) {
        val type = runCatching { JSONObject(raw).optString("type") }.getOrNull()
        if (type == RescueClaimPacket.TYPE) {
            handleClaim(raw)
            return
        }
        if (type == RescueStatusPacket.TYPE) {
            handleStatus(raw)
            return
        }
        if (type != RescuePacket.TYPE_SOS) {
            Log.d(TAG, "Unknown packet type ignored: $type")
            return
        }
        val packet = RescuePacket.fromJson(raw)
        if (packet == null) {
            Log.w(TAG, "Malformed JSON ignored")
            return
        }
        if (packet.type != RescuePacket.TYPE_SOS) {
            Log.d(TAG, "Unsupported packet type ignored: ${packet.type}")
            return
        }
        runOnUiThread {
            if (!seenPacketIds.add(packet.id)) {
                Log.d(TAG, "Duplicate packet ignored: ${packet.id}")
                return@runOnUiThread
            }
            Log.i(TAG, "SOS parsed: ${packet.id}, priority=${packet.priority}")
            incidents.add(0, ResponderIncident.fromPacket(packet))
            latestStatusByTeam(statusesByIncident[packet.id].orEmpty()).values
                .sortedBy { RescueLifecycle.parse(it.status)?.rank }
                .forEach { applyDisplayedStatus(packet.id, it.teamId, it.status) }
            meshStatus = "SOS received"
        }
    }

    private fun handleClaim(raw: String) {
        val packet = RescueClaimPacket.fromJson(raw) ?: run {
            Log.w(TAG, "Malformed RESCUE_CLAIM ignored")
            return
        }
        runOnUiThread {
            if (!seenClaimIds.add(packet.id)) return@runOnUiThread
            claimsByIncident[packet.incidentId] = claimsByIncident[packet.incidentId].orEmpty() + packet.asClaim()
            recordEvent(packet.incidentId, RescueLifecycle.ACCEPTED.name, packet.timestamp, packet.teamId, packet.teamName, packet.callsign, packet.id)
            latestStatusByTeam(statusesByIncident[packet.incidentId].orEmpty())[packet.teamId]?.let {
                applyDisplayedStatus(packet.incidentId, packet.teamId, it.status)
            }
            Log.i(TAG, "Claim received: ${packet.id} for ${packet.incidentId}")
            if (packet.canRelay() && hasOutgoingScanPermission()) meshSender.send(packet.nextHop())
        }
    }

    private fun handleStatus(raw: String) {
        val packet = RescueStatusPacket.fromJson(raw) ?: run {
            Log.w(TAG, "Malformed RESCUE_STATUS ignored")
            return
        }
        runOnUiThread {
            if (!seenStatusIds.add(packet.id)) {
                Log.d(TAG, "Duplicate status ignored: ${packet.id}")
                return@runOnUiThread
            }
            val existing = statusesByIncident[packet.incidentId].orEmpty()
            val prior = latestStatusByTeam(existing)[packet.teamId]
            if (prior != null && !RescueLifecycle.progresses(prior.status, packet.status)) {
                Log.i(TAG, "Ignored lifecycle regression ${packet.status} after ${prior.status} for ${packet.incidentId}")
            } else {
                statusesByIncident[packet.incidentId] = existing + packet.asEvent()
                recordEvent(packet.incidentId, packet.status, packet.timestamp, packet.teamId, packet.teamName, packet.callsign, packet.id)
                applyDisplayedStatus(packet.incidentId, packet.teamId, packet.status)
                Log.i(TAG, "Received RESCUE_STATUS ${packet.id}: ${packet.status}")
            }
            if (packet.canRelay() && hasOutgoingScanPermission()) meshSender.send(packet.nextHop())
        }
    }

    private fun applyDisplayedStatus(incidentId: String, packetTeamId: String, status: String) {
        val index = incidents.indexOfFirst { it.id == incidentId }
        if (index < 0) return
        val localTeamId = teamProfile?.teamId
        val ownership = localTeamId?.let { ownershipOf(claimsByIncident[incidentId].orEmpty(), it) }
        if ((packetTeamId == localTeamId || ownership == IncidentOwnership.CLAIMED_BY_OTHER) &&
            RescueLifecycle.progresses(incidents[index].status, status)
        ) incidents[index] = incidents[index].copy(status = status)
    }

    private fun acceptIncident(incident: ResponderIncident): ResponderIncident {
        if (incident.status != "NEW") return incident
        val configuredTeam = teamProfile ?: run {
            identityError = "Responder team identity is not configured."
            Log.e(TAG, "Cannot accept incident without responder team identity")
            return incident
        }
        val index = incidents.indexOfFirst { it.id == incident.id }
        if (index < 0) return incident
        if (ownershipOf(claimsByIncident[incident.id].orEmpty(), configuredTeam.teamId) != IncidentOwnership.UNCLAIMED) return incident
        identityError = null
        val accepted = incident.copy(status = "ACCEPTED")
        incidents[index] = accepted
        Log.i(TAG, "Incident accepted: ${incident.id}")
        val claimRecord = claimRecords.getOrPut(incident.id) {
            ClaimRecord(RescueClaimPacket.create(accepted, responderLocation, configuredTeam)).also {
                seenClaimIds.add(it.packet.id)
                claimsByIncident[incident.id] = claimsByIncident[incident.id].orEmpty() + it.packet.asClaim()
                Log.i(TAG, "Claim created: ${it.packet.id}")
            }
        }
        recordEvent(
            incident.id, RescueLifecycle.ACCEPTED.name, claimRecord.packet.timestamp,
            configuredTeam.teamId, configuredTeam.teamName, configuredTeam.callsign, claimRecord.packet.id,
        )
        val record = ackRecords.getOrPut(incident.id) {
            AckRecord(RescueAckPacket.create(accepted, responderLocation, configuredTeam)).also {
                Log.i(TAG, "ACK created: ${it.packet.id}")
            }
        }
        sendClaim(claimRecord)
        sendAck(record)
        return accepted
    }

    private fun advanceLifecycle(incident: ResponderIncident, next: RescueLifecycle) {
        val profile = teamProfile ?: run {
            identityError = "Approved responder identity is required to update rescue status."
            return
        }
        val current = RescueLifecycle.parse(incident.status) ?: return
        val ownership = ownershipOf(claimsByIncident[incident.id].orEmpty(), profile.teamId)
        if (next != current.next() || ownership !in setOf(IncidentOwnership.CLAIMED_BY_ME, IncidentOwnership.CONFLICT)) return
        val index = incidents.indexOfFirst { it.id == incident.id }
        if (index < 0) return
        val updated = incidents[index].copy(status = next.name)
        incidents[index] = updated
        val packet = RescueStatusPacket.create(updated, next, responderLocation, profile)
        seenStatusIds.add(packet.id)
        statusesByIncident[incident.id] = statusesByIncident[incident.id].orEmpty() + packet.asEvent()
        recordEvent(incident.id, next.name, packet.timestamp, profile.teamId, profile.teamName, profile.callsign, packet.id)
        statusRecords[packet.id] = StatusRecord(packet)
        Log.i(TAG, "Incident ${incident.id} -> ${next.name}; created ${packet.id}")
        sendStatus(statusRecords.getValue(packet.id))
    }

    private fun recordEvent(
        incidentId: String, status: String, timestamp: Long, teamId: String,
        teamName: String, callsign: String, sourcePacketId: String,
    ) {
        val lifecycle = RescueLifecycle.parse(status) ?: return
        lifecycleEventsByIncident[incidentId] = recordLifecycleEvent(
            lifecycleEventsByIncident[incidentId].orEmpty(),
            RescueLifecycleEvent(lifecycle, timestamp, teamId, teamName, callsign, sourcePacketId),
        )
    }

    private fun latestStatusRecord(incidentId: String): StatusRecord? =
        statusRecords.values.filter { it.packet.incidentId == incidentId }.maxByOrNull { it.packet.timestamp }

    private fun retryStatus(incidentId: String) {
        latestStatusRecord(incidentId)?.let {
            Log.i(TAG, "Retrying ${it.packet.id}")
            sendStatus(it)
        }
    }

    private fun sendStatus(record: StatusRecord) {
        if (!hasOutgoingScanPermission()) {
            statusRecords[record.packet.id] = record.copy(state = AckSendState.FAILED, failureReason = "Bluetooth scan permission required")
            Log.w(TAG, "Status send failed: ${record.packet.id}")
            return
        }
        Log.i(TAG, "Sending status ${record.packet.id}")
        meshSender.send(record.packet)
    }

    private fun retryClaim(incidentId: String) {
        claimRecords[incidentId]?.let(::sendClaim)
    }

    private fun sendClaim(record: ClaimRecord) {
        if (!hasOutgoingScanPermission()) {
            claimRecords[record.packet.incidentId] = record.copy(state = AckSendState.FAILED, failureReason = "Bluetooth scan permission required")
            return
        }
        meshSender.send(record.packet)
    }

    private fun retryAck(incidentId: String) {
        val record = ackRecords[incidentId] ?: return
        Log.i(TAG, "ACK retry requested: ${record.packet.id}")
        sendAck(record)
    }

    private fun sendAck(record: AckRecord) {
        if (!hasOutgoingScanPermission()) {
            ackRecords[record.packet.incidentId] = record.copy(
                state = AckSendState.FAILED,
                failureReason = "Bluetooth scan permission required",
            )
            return
        }
        meshSender.send(record.packet)
    }

    private fun postStatus(status: String) = runOnUiThread { meshStatus = status }

    private fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAllBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return hasRequiredPermissions() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasOutgoingScanPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else hasLocationPermission()

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        if (::bleAdvertiser.isInitialized) bleAdvertiser.stop()
        if (::bleServer.isInitialized) bleServer.stop()
        if (::locationProvider.isInitialized) locationProvider.stop()
        if (::meshSender.isInitialized) meshSender.close()
        receiverStarted = false
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SaharaResponder"
        val ACTIVE_STATUSES = setOf("ACCEPTED", "ON_THE_WAY", "NEARBY", "ARRIVED")
    }
}
