package com.skai.taskmanager.notification

import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.data.repository.TimePeriodRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for framework-constructed components (BroadcastReceivers) that need the
 * task board and repositories. Resolved via
 * [dagger.hilt.android.EntryPointAccessors.fromApplication].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationRepositoryEntryPoint {
    fun taskBoard(): TaskBoard
    fun taskRepository(): TaskRepository
    fun timePeriodRepository(): TimePeriodRepository
}
