package com.kernitect.sahararesponder.persistence

import android.content.Context
import androidx.room.*

@Entity(tableName = "incidents")
data class IncidentEntity(@PrimaryKey val incidentId: String, val priority: String, val message: String,
    val latitude: Double, val longitude: Double, val sosTimestamp: Long, val receivedAt: Long,
    val hopCount: Int, val ttl: Int, val lifecycleStatus: String)

@Entity(tableName = "claims", indices = [Index("incidentId")])
data class ClaimEntity(@PrimaryKey val packetId: String, val incidentId: String, val responderId: String,
    val teamId: String, val teamName: String, val callsign: String, val deviceId: String, val district: String,
    val latitude: Double, val longitude: Double, val timestamp: Long)

@Entity(tableName = "lifecycle_events", primaryKeys = ["incidentId", "teamId", "status"], indices = [Index("incidentId")])
data class LifecycleEventEntity(val incidentId: String, val status: String, val timestamp: Long, val teamId: String,
    val teamName: String, val callsign: String, val sourcePacketId: String)

@Entity(tableName = "outgoing_mesh_packets", indices = [Index("incidentId")])
data class OutgoingPacketEntity(@PrimaryKey val packetId: String, val incidentId: String, val packetType: String,
    val payloadJson: String, val createdAt: Long, val sendState: String, val attemptCount: Int = 0, val lastAttemptAt: Long? = null,
    val failureReason: String? = null)

@Entity(tableName = "processed_packets", indices = [Index("incidentId")])
data class ProcessedPacketEntity(@PrimaryKey val packetId: String, val incidentId: String, val packetType: String, val processedAt: Long)

@Dao
interface ResponderDao {
    @Query("SELECT * FROM incidents ORDER BY receivedAt DESC") suspend fun incidents(): List<IncidentEntity>
    @Query("SELECT * FROM claims") suspend fun claims(): List<ClaimEntity>
    @Query("SELECT * FROM lifecycle_events ORDER BY timestamp") suspend fun events(): List<LifecycleEventEntity>
    @Query("SELECT * FROM outgoing_mesh_packets") suspend fun outgoing(): List<OutgoingPacketEntity>
    @Query("SELECT * FROM processed_packets") suspend fun processed(): List<ProcessedPacketEntity>
    @Upsert suspend fun upsertIncident(value: IncidentEntity)
    @Upsert suspend fun upsertClaim(value: ClaimEntity)
    @Upsert suspend fun upsertEvent(value: LifecycleEventEntity)
    @Upsert suspend fun upsertOutgoing(value: OutgoingPacketEntity)
    @Upsert suspend fun upsertProcessed(value: ProcessedPacketEntity)
}

@Database(entities = [IncidentEntity::class, ClaimEntity::class, LifecycleEventEntity::class,
    OutgoingPacketEntity::class, ProcessedPacketEntity::class], version = 1, exportSchema = false)
abstract class ResponderDatabase : RoomDatabase() {
    abstract fun dao(): ResponderDao
    companion object {
        @Volatile private var instance: ResponderDatabase? = null
        fun get(context: Context): ResponderDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ResponderDatabase::class.java, "sahara_responder.db")
                .build().also { instance = it }
        }
    }
}
