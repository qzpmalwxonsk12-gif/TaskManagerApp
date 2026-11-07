package com.skai.taskmanager.data.repository

import com.skai.taskmanager.data.local.dao.SessionStatDao
import com.skai.taskmanager.data.local.entity.SessionStatEntity
import com.skai.taskmanager.domain.model.ActivityStat
import com.skai.taskmanager.domain.model.StatRange
import com.skai.taskmanager.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Statistics over completed focus sessions. Aggregation is delegated to SQL; this repository only
 * resolves the time window for a [StatRange] and records completions. Adding new ranges (yearly,
 * custom) is just another window calculation — no schema or query changes needed.
 */
interface StatsRepository {
    fun observeStats(range: StatRange): Flow<List<ActivityStat>>
    suspend fun recordCompletion(activityName: String, durationSeconds: Int, completedAt: Long)
}

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val dao: SessionStatDao
) : StatsRepository {

    override fun observeStats(range: StatRange): Flow<List<ActivityStat>> {
        val start = when (range) {
            StatRange.DAILY -> DateUtils.startOfToday()
            StatRange.WEEKLY -> DateUtils.startOfWeek()
            StatRange.MONTHLY -> DateUtils.startOfMonth()
        }
        return dao.observeBetween(start, DateUtils.endOfToday())
    }

    override suspend fun recordCompletion(activityName: String, durationSeconds: Int, completedAt: Long) {
        val name = activityName.trim()
        if (name.isEmpty() || durationSeconds <= 0) return
        dao.insert(
            SessionStatEntity(
                activityName = name,
                durationSeconds = durationSeconds,
                completedAt = completedAt
            )
        )
    }
}
