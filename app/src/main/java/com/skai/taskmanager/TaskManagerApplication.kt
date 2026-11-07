package com.skai.taskmanager

import android.app.Application
import com.skai.taskmanager.notification.TaskBoard
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt's code generation, including a base
 * class that serves as the application-level dependency container.
 *
 * Starting the [TaskBoard] observers here means every process start — app launch, a notification
 * button, an alarm, boot — brings the task-list notification, widgets and alarms up to date,
 * with no long-running service required.
 */
@HiltAndroidApp
class TaskManagerApplication : Application() {

    @Inject
    lateinit var taskBoard: TaskBoard

    override fun onCreate() {
        super.onCreate()
        com.skai.taskmanager.ui.theme.AppearancePreferences.initialize(this)
        taskBoard.startObserving()
    }
}
