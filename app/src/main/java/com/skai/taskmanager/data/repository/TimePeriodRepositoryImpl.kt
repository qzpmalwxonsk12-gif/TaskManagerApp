package com.skai.taskmanager.data.repository

import com.skai.taskmanager.data.local.dao.TimePeriodDao
import com.skai.taskmanager.data.mapper.toDomain
import com.skai.taskmanager.data.mapper.toEntity
import com.skai.taskmanager.domain.model.TimePeriod
import com.skai.taskmanager.domain.scheduling.SchedulingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimePeriodRepositoryImpl @Inject constructor(
    private val dao: TimePeriodDao
) : TimePeriodRepository {

    override fun observeAllPeriods(): Flow<List<TimePeriod>> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getAllPeriodsOnce(): List<TimePeriod> =
        dao.getAllOnce().toDomain()

    override suspend fun getPeriod(id: Int): TimePeriod? =
        dao.getById(id)?.toDomain()

    override suspend fun getCurrentPeriod(time: LocalTime): TimePeriod? =
        SchedulingEngine.activePeriod(getAllPeriodsOnce(), time)

    override suspend fun savePeriod(period: TimePeriod): Int {
        val rowId = dao.upsert(period.toEntity())
        // upsert returns the rowId for inserts; for updates the existing id is authoritative.
        return if (period.id == 0) rowId.toInt() else period.id
    }

    override suspend fun deletePeriod(period: TimePeriod) {
        dao.delete(period.toEntity())
    }

    override suspend fun setEnabled(period: TimePeriod, enabled: Boolean) {
        dao.upsert(period.copy(isEnabled = enabled).toEntity())
    }

    override suspend fun reorder(orderedIds: List<Int>) {
        val byId = dao.getAllOnce().associateBy { it.id }
        val reordered = orderedIds.mapIndexedNotNull { index, id ->
            byId[id]?.copy(sortOrder = index)
        }
        dao.upsertAll(reordered)
    }

    override suspend fun nextSortOrder(): Int = dao.maxSortOrder() + 1

    override suspend fun count(): Int = dao.count()
}
