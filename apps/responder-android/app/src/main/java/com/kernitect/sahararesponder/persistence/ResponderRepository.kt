package com.kernitect.sahararesponder.persistence

import androidx.room.withTransaction
import com.kernitect.sahararesponder.mesh.AckSendState
import com.kernitect.sahararesponder.model.*

data class RestoredOperationalState(
    val incidents: List<ResponderIncident>, val claims: List<IncidentClaim>,
    val events: List<Pair<String, RescueLifecycleEvent>>, val outgoing: List<OutgoingPacketEntity>,
    val processed: List<ProcessedPacketEntity>,
)

class ResponderRepository(private val database: ResponderDatabase) {
    private val dao = database.dao()

    suspend fun restore() = RestoredOperationalState(
        incidents = dao.incidents().map { ResponderIncident(it.incidentId, it.priority, it.message, it.latitude, it.longitude, it.sosTimestamp, it.hopCount, it.lifecycleStatus, it.receivedAt) },
        claims = dao.claims().map { IncidentClaim(it.packetId, it.incidentId, it.responderId, it.teamId, it.teamName, it.callsign, it.deviceId, it.district, it.latitude, it.longitude, it.timestamp) },
        events = dao.events().mapNotNull { entity -> RescueLifecycle.parse(entity.status)?.let {
            entity.incidentId to RescueLifecycleEvent(it, entity.timestamp, entity.teamId, entity.teamName, entity.callsign, entity.sourcePacketId)
        } },
        outgoing = dao.outgoing().map(::normalizeRecoveredOutgoing),
        processed = dao.processed(),
    )

    suspend fun saveIncident(incident: ResponderIncident) = dao.upsertIncident(incident.entity())
    suspend fun saveClaim(claim: IncidentClaim) = dao.upsertClaim(claim.entity())
    suspend fun saveEvent(incidentId: String, event: RescueLifecycleEvent) = dao.upsertEvent(event.entity(incidentId))
    suspend fun saveProcessed(packetId: String, incidentId: String, type: String) =
        dao.upsertProcessed(ProcessedPacketEntity(packetId, incidentId, type, System.currentTimeMillis()))

    suspend fun cloudSummary(): com.kernitect.sahararesponder.sync.CloudSyncSummary {
        val states = dao.cloudRecords().map { com.kernitect.sahararesponder.sync.normalizeCloudState(it.backendSyncState) }
        return com.kernitect.sahararesponder.sync.CloudSyncSummary(states.count { it == "PENDING" }, states.count { it == "SYNCING" }, states.count { it == "FAILED" })
    }

    suspend fun saveOutgoing(packet: MeshOutgoingPacket, state: AckSendState, failure: String? = null, attempted: Boolean = false) {
        val old = dao.outgoing().firstOrNull { it.packetId == packet.id }
        dao.upsertOutgoing(OutgoingPacketEntity(packet.id, packet.incidentId, packet.type, packet.toJson(),
            old?.createdAt ?: System.currentTimeMillis(), state.name, (old?.attemptCount ?: 0) + if (attempted) 1 else 0,
            if (attempted) System.currentTimeMillis() else old?.lastAttemptAt, failure))
    }

    suspend fun persistAcceptance(incident: ResponderIncident, claim: IncidentClaim, event: RescueLifecycleEvent,
        claimPacket: RescueClaimPacket, ackPacket: RescueAckPacket) = database.withTransaction {
        dao.upsertIncident(incident.entity()); dao.upsertClaim(claim.entity()); dao.upsertEvent(event.entity(incident.id))
        dao.upsertProcessed(ProcessedPacketEntity(claim.packetId, incident.id, RescueClaimPacket.TYPE, System.currentTimeMillis()))
        dao.upsertOutgoing(OutgoingPacketEntity(claimPacket.id, incident.id, claimPacket.type, claimPacket.toJson(), claimPacket.timestamp, AckSendState.IDLE.name))
        dao.upsertOutgoing(OutgoingPacketEntity(ackPacket.id, incident.id, ackPacket.type, ackPacket.toJson(), ackPacket.timestamp, AckSendState.IDLE.name))
    }

    suspend fun persistTransition(incident: ResponderIncident, event: RescueLifecycleEvent, packet: RescueStatusPacket) = database.withTransaction {
        dao.upsertIncident(incident.entity()); dao.upsertEvent(event.entity(incident.id))
        dao.upsertProcessed(ProcessedPacketEntity(packet.id, incident.id, packet.type, System.currentTimeMillis()))
        dao.upsertOutgoing(OutgoingPacketEntity(packet.id, incident.id, packet.type, packet.toJson(), packet.timestamp, AckSendState.IDLE.name))
    }

    private fun ResponderIncident.entity() = IncidentEntity(id, priority, message, latitude, longitude, timestamp, receivedAt, hopCount, 5, status)
    private fun IncidentClaim.entity() = ClaimEntity(packetId, incidentId, responderId, teamId, teamName, callsign, deviceId, district, latitude, longitude, timestamp)
    private fun RescueLifecycleEvent.entity(incidentId: String) = LifecycleEventEntity(incidentId, status.name, timestamp, teamId, teamName, callsign, sourcePacketId)

}

private val transientSendStates = setOf(AckSendState.SEARCHING.name, AckSendState.CONNECTING.name, AckSendState.SENDING.name)

fun normalizeRecoveredOutgoing(entity: OutgoingPacketEntity): OutgoingPacketEntity =
    if (entity.sendState in transientSendStates) entity.copy(sendState = AckSendState.FAILED.name, failureReason = "Send interrupted by app restart") else entity
