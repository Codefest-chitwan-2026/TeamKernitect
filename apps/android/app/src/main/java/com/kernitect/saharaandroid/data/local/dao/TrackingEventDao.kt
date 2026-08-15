package com.kernitect.saharaandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kernitect.saharaandroid.data.local.entity.TrackingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingEventDao {

    @Query(
        """
        SELECT * FROM tracking_events
        WHERE incidentId = :incidentId
        ORDER BY timestamp ASC
        """
    )
    fun observeEventsForIncident(
        incidentId: String
    ): Flow<List<TrackingEventEntity>>

    @Query(
        """
        SELECT * FROM tracking_events
        ORDER BY timestamp ASC
        """
    )
    fun observeAllEvents():
            Flow<List<TrackingEventEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(
        event: TrackingEventEntity
    ): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM tracking_events
            WHERE incidentId = :incidentId AND type = :type
        )
        """
    )
    suspend fun hasEvent(
        incidentId: String,
        type: String
    ): Boolean

    @Query(
        """
        DELETE FROM tracking_events
        WHERE incidentId = :incidentId
        """
    )
    suspend fun deleteEventsForIncident(
        incidentId: String
    )
}
