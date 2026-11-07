package com.skai.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.skai.taskmanager.data.local.entity.SessionStatEntity
import com.skai.taskmanager.domain.model.ActivityStat
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStatDao {

    @Insert
    suspend fun insert(stat: SessionStatEntity)

    /**
     * Aggregated focus time per activity within a time window, sorted by most time first. The
     * grouping/summing is done by SQLite, so the screen doesn't scan individual completions.
     */
    @Query(
        "SELECT activityName AS name, SUM(durationSeconds) AS totalSeconds " +
            "FROM session_stats WHERE completedAt BETWEEN :start AND :end " +
            "GROUP BY activityName ORDER BY totalSeconds DESC"
    )
    fun observeBetween(start: Long, end: Long): Flow<List<ActivityStat>>
}
