package com.kernitect.saharaandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.kernitect.saharaandroid.data.local.entity.PublicAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicAlertDao {

    @Query(
        """
        SELECT * FROM public_alerts
        ORDER BY severity DESC
        """
    )
    fun observeAllAlerts():
            Flow<List<PublicAlertEntity>>
}