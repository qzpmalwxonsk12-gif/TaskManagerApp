package com.skai.taskmanager.di

import com.skai.taskmanager.data.repository.NoteRepository
import com.skai.taskmanager.data.repository.NoteRepositoryImpl
import com.skai.taskmanager.data.repository.ProfileRepository
import com.skai.taskmanager.data.repository.ProfileRepositoryImpl
import com.skai.taskmanager.data.repository.SettingsRepository
import com.skai.taskmanager.data.repository.SettingsRepositoryImpl
import com.skai.taskmanager.data.repository.StatsRepository
import com.skai.taskmanager.data.repository.StatsRepositoryImpl
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.data.repository.TaskRepositoryImpl
import com.skai.taskmanager.data.repository.TimePeriodRepository
import com.skai.taskmanager.data.repository.TimePeriodRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTimePeriodRepository(impl: TimePeriodRepositoryImpl): TimePeriodRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository
}
