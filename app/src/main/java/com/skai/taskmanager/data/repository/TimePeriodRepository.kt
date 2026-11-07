package com.skai.taskmanager.data.repository

import com.skai.taskmanager.domain.model.TimePeriod
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/**
 * Single source of truth for scheduling periods. Mirrors the existing repository style so the
 * notification service, ViewModels and future scheduling features all depend on this abstraction.
 */
interface TimePeriodRepository {

    fun observeAllPeriods(): Flow<List<TimePeriod>>

    suspend fun getAllPeriodsOnce(): List<TimePeriod>

    suspend fun getPeriod(id: Int): TimePeriod?

    /** The currently active, enabled period for [time], or null (gap / all disabled). */
    suspend fun getCurrentPeriod(time: LocalTime): TimePeriod?

    /** Inserts or updates a period; returns its id. */
    suspend fun savePeriod(period: TimePeriod): Int

    suspend fun deletePeriod(period: TimePeriod)

    suspend fun setEnabled(period: TimePeriod, enabled: Boolean)

    /** Persists a new ordering: each id's index in [orderedIds] becomes its sortOrder. */
    suspend fun reorder(orderedIds: List<Int>)

    /** sortOrder to use for a newly added period (appended at the end). */
    suspend fun nextSortOrder(): Int

    suspend fun count(): Int
}
