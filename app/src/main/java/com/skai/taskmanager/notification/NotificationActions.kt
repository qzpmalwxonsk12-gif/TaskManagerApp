package com.skai.taskmanager.notification

/**
 * Centralized constants for the persistent task notification: channel, ids, intent actions and
 * extras. Keeping them in one place avoids drift between the service, the receiver and the
 * RemoteViews factory.
 */
object NotificationActions {

    const val CHANNEL_ID = "task_notification_channel"
    const val CHANNEL_NAME = "Tasks"
    const val CHANNEL_DESCRIPTION = "Persistent task list"

    // Separate high-importance channels for the pop-up reminders, so they alert (heads-up)
    // without ever touching the silent persistent notification above. The reminder channel is
    // itself soundless: each task's own alert profile (chime/melody/alarm/custom) is played by
    // AlertPlayer, which a fixed channel sound would conflict with.
    const val REMINDER_CHANNEL_ID = "task_reminder_channel_v2"
    const val REMINDER_CHANNEL_NAME = "Task reminders"
    const val REMINDER_CHANNEL_DESCRIPTION = "Pops up when a timed task is due"

    /** Pre-alert-profiles channel id; deleted on startup so old installs migrate cleanly. */
    const val LEGACY_REMINDER_CHANNEL_ID = "task_reminder_channel"
    const val NOTE_PING_CHANNEL_ID = "note_ping_channel"
    const val NOTE_PING_CHANNEL_NAME = "Notes reminder"
    const val NOTE_PING_CHANNEL_DESCRIPTION = "Periodic reminder to check your notes"

    const val NOTIFICATION_ID = 1001

    /** Task-reminder pop-up (auto-dismisses). Distinct from the persistent notification. */
    const val TASK_REMINDER_NOTIFICATION_ID = 1003

    /** Note-ping pop-up (auto-dismisses). */
    const val NOTE_PING_NOTIFICATION_ID = 1004

    // Broadcast actions handled by NotificationActionReceiver
    const val ACTION_COMPLETE_TASK = "com.skai.taskmanager.action.COMPLETE_TASK"
    const val ACTION_UNCOMPLETE_TASK = "com.skai.taskmanager.action.UNCOMPLETE_TASK"
    const val ACTION_PERIOD_TRANSITION = "com.skai.taskmanager.action.PERIOD_TRANSITION"

    /** Fired when the notification is dismissed, or periodically — brings the notification back. */
    const val ACTION_REVIVE = "com.skai.taskmanager.action.REVIVE"

    /** Alarm: a timed task's reminder time has arrived. */
    const val ACTION_TASK_REMINDER = "com.skai.taskmanager.action.TASK_REMINDER"

    /** Alarm: the periodic note ping is due. */
    const val ACTION_NOTE_PING = "com.skai.taskmanager.action.NOTE_PING"

    /** Alarm: midnight passed — run the day rollover (archive yesterday, refresh recurring). */
    const val ACTION_DAY_ROLLOVER = "com.skai.taskmanager.action.DAY_ROLLOVER"

    /** A reminder notification was dismissed — stop its sound/vibration. */
    const val ACTION_STOP_ALERT = "com.skai.taskmanager.action.STOP_ALERT"

    /** Reminder button: skip these tasks for today (filed in History as "Skipped"). */
    const val ACTION_SKIP_TASKS = "com.skai.taskmanager.action.SKIP_TASKS"

    /** Reminder button: snooze these tasks — re-remind after the user's snooze duration. */
    const val ACTION_SNOOZE_TASKS = "com.skai.taskmanager.action.SNOOZE_TASKS"

    /** Alarm: a snoozed reminder is due again. */
    const val ACTION_SNOOZED_REMINDER = "com.skai.taskmanager.action.SNOOZED_REMINDER"

    // Activity action: open the app (whole-notification tap)
    const val ACTION_OPEN_APP = "com.skai.taskmanager.action.OPEN_APP"

    // Explicit board re-render request (same handling as REVIVE)
    const val ACTION_REFRESH = "com.skai.taskmanager.action.REFRESH"

    const val EXTRA_TASK_ID = "com.skai.taskmanager.extra.TASK_ID"

    /** The minute-of-day a task-reminder alarm was scheduled for. */
    const val EXTRA_REMINDER_MINUTE = "com.skai.taskmanager.extra.REMINDER_MINUTE"

    /** The ids of the tasks a reminder (or its Skip/Snooze buttons) refers to. */
    const val EXTRA_TASK_IDS = "com.skai.taskmanager.extra.TASK_IDS"

    // Distinct PendingIntent request codes so they don't collide.
    const val REQ_OPEN_APP = 100
    const val REQ_ADD_TASK = 101
    const val REQ_ADD_NOTE = 102
    const val REQ_PERIOD_TRANSITION = 104
    const val REQ_REVIVE = 105
    const val REQ_KEEPALIVE = 106
    const val REQ_TASK_REMINDER = 107
    const val REQ_NOTE_PING = 108
    const val REQ_DAY_ROLLOVER = 109
    const val REQ_OPEN_NOTES = 110
    const val REQ_STOP_ALERT = 111
    const val REQ_PIN_NOTE = 112
    const val REQ_SKIP_TASKS = 113
    const val REQ_SNOOZE_TASKS = 114
    const val REQ_SNOOZED_REMINDER = 115

    /** How often to re-assert the notification in case it was cleared. */
    const val KEEPALIVE_INTERVAL_MS = 15 * 60 * 1000L

    /** How long the period-change transition summary is shown before the new list appears. */
    const val TRANSITION_DURATION_MS = 4_000L
}
