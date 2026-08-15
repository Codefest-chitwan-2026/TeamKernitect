package com.kernitect.saharaandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kernitect.saharaandroid.data.local.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    /*
     * Notifications/history.
     *
     * Newest received incident first.
     */
    @Query(
        """
        SELECT * FROM incidents
        ORDER BY receivedAt DESC
        """
    )
    fun observeAllIncidents():
            Flow<List<IncidentEntity>>


    /*
     * Save an incoming packet.
     *
     * IGNORE prevents the same packet ID from
     * appearing twice if it somehow arrives again.
     *
     * It also avoids resetting isRead back to false
     * for an already stored incident.
     */
    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insertIncident(
        incident: IncidentEntity
    )


    /*
     * Bell unread badge.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM incidents
        WHERE isRead = 0
        """
    )
    fun observeUnreadCount():
            Flow<Int>


    /*
     * Opening Notifications marks all
     * currently stored incidents as read.
     */
    @Query(
        """
        UPDATE incidents
        SET isRead = 1
        """
    )
    suspend fun markAllAsRead()


    /*
     * Useful later for testing/resetting.
     */
    @Query(
        """
        DELETE FROM incidents
        """
    )
    suspend fun deleteAll()
}