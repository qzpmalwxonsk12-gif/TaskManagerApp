package com.skai.taskmanager.data.repository

import com.skai.taskmanager.data.local.dao.SettingsDao
import com.skai.taskmanager.data.mapper.toDomain
import com.skai.taskmanager.data.mapper.toEntity
import com.skai.taskmanager.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun update(settings: AppSettings)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        dao.observe().map { it?.toDomain() ?: AppSettings.DEFAULT }

    override suspend fun getSettings(): AppSettings =
        dao.getOnce()?.toDomain() ?: AppSettings.DEFAULT

    override suspend fun update(settings: AppSettings) =
        dao.upsert(settings.copy(id = AppSettings.SINGLETON_ID).toEntity())
}
