package com.skai.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.skai.taskmanager.data.local.entity.TimePeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimePeriodDao {

    @Query("SELECT * FROM time_periods ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<TimePeriodEntity>>

    @Query("SELECT * FROM time_periods ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<TimePeriodEntity>

    @Query("SELECT * FROM time_periods WHERE id = :id")
    suspend fun getById(id: Int): TimePeriodEntity?

    @Query("SELECT COUNT(*) FROM time_periods")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM time_periods")
    suspend fun maxSortOrder(): Int

    @Upsert
    suspend fun upsert(period: TimePeriodEntity): Long

    @Upsert
    suspend fun upsertAll(periods: List<TimePeriodEntity>)

    @Delete
    suspend fun delete(period: TimePeriodEntity)
}
