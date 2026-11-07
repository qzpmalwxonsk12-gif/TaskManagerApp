package com.skai.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded focus-session completion. Statistics are pre-aggregated from this dedicated table
 * (via SQL GROUP BY), so the Stats screen never scans every completed task.
 */
@Entity(
    tableName = "session_stats",
    indices = [Index("completedAt"), Index("activityName")]
)
data class SessionStatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val activityName: String,
    val durationSeconds: Int,
    val completedAt: Long
)
