package com.skai.taskmanager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles every alarm and notification action for the task board — completions, skip/snooze,
 * task reminders, note pings, period transitions, the midnight rollover and the keep-alive.
 * Each event does its (milliseconds-long) work via [TaskBoard] inside the `goAsync` window and
 * lets the process go back to sleep: no foreground service is involved.
 *
 * Dependencies come through a Hilt entry point (rather than `@AndroidEntryPoint` field
 * injection) so this stays a plain BroadcastReceiver with no base-class requirements.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationRepositoryEntryPoint::class.java
        )
        val board = entryPoint.taskBoard()
        val repository = entryPoint.taskRepository()

        // onReceive runs on the main thread; hand off to IO and keep the process alive meanwhile.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationActions.ACTION_COMPLETE_TASK,
                    NotificationActions.ACTION_UNCOMPLETE_TASK -> {
                        val taskId = intent.getLongExtra(NotificationActions.EXTRA_TASK_ID, -1L)
                        if (taskId > 0L) {
                            repository.getTask(taskId)?.let { task ->
                                repository.setTaskCompleted(
                                    task,
                                    intent.action == NotificationActions.ACTION_COMPLETE_TASK
                                )
                            }
                            board.refresh()
                        }
                    }

                    NotificationActions.ACTION_PERIOD_TRANSITION -> board.onPeriodTransition()

                    NotificationActions.ACTION_TASK_REMINDER -> board.onTaskReminder(
                        intent.getIntExtra(NotificationActions.EXTRA_REMINDER_MINUTE, -1)
                    )

                    NotificationActions.ACTION_NOTE_PING -> board.onNotePing()

                    NotificationActions.ACTION_DAY_ROLLOVER -> board.onDayRollover()

                    // Reminder dismissed (swipe/tap/timeout) — silence its sound and vibration.
                    NotificationActions.ACTION_STOP_ALERT -> board.stopAlert()

                    NotificationActions.ACTION_SKIP_TASKS -> board.onSkipTasks(
                        intent.getLongArrayExtra(NotificationActions.EXTRA_TASK_IDS)
                    )

                    NotificationActions.ACTION_SNOOZE_TASKS -> board.onSnoozeTasks(
                        intent.getLongArrayExtra(NotificationActions.EXTRA_TASK_IDS)
                    )

                    NotificationActions.ACTION_SNOOZED_REMINDER -> board.onSnoozedReminder(
                        intent.getLongArrayExtra(NotificationActions.EXTRA_TASK_IDS)
                    )

                    // Keep-alive tick, or the persistent notification was dismissed — re-render
                    // (a cleared dismissible list stays cleared; widgets always refresh).
                    NotificationActions.ACTION_REFRESH,
                    NotificationActions.ACTION_REVIVE -> board.refresh()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
