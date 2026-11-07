package com.skai.taskmanager.data.repository

import com.skai.taskmanager.data.local.dao.ProfileDao
import com.skai.taskmanager.data.mapper.toDomain
import com.skai.taskmanager.data.mapper.toEntity
import com.skai.taskmanager.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    fun observeProfile(): Flow<Profile>
    suspend fun save(profile: Profile)
}

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val dao: ProfileDao
) : ProfileRepository {

    override fun observeProfile(): Flow<Profile> =
        dao.observe().map { it?.toDomain() ?: Profile.DEFAULT }

    override suspend fun save(profile: Profile) =
        dao.upsert(profile.copy(id = Profile.SINGLETON_ID).toEntity())
}
