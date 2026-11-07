package com.skai.taskmanager.session

/** Constants shared by the SessionService, its notification, and the session popups. */
object SessionActions {

    const val SESSION_CHANNEL_ID = "session_channel"
    const val SESSION_CHANNEL_NAME = "Focus session"
    const val ALERT_CHANNEL_ID = "session_alert_channel"
    const val ALERT_CHANNEL_NAME = "Session alerts"

    const val SESSION_NOTIFICATION_ID = 2001
    const val ALERT_NOTIFICATION_ID = 2002

    const val ACTION_START = "com.skai.taskmanager.session.START"
    const val ACTION_PAUSE = "com.skai.taskmanager.session.PAUSE"
    const val ACTION_RESUME = "com.skai.taskmanager.session.RESUME"
    const val ACTION_COMPLETE = "com.skai.taskmanager.session.COMPLETE"
    const val ACTION_EXTEND = "com.skai.taskmanager.session.EXTEND"
    const val ACTION_RESTART = "com.skai.taskmanager.session.RESTART"
    const val ACTION_STOP = "com.skai.taskmanager.session.STOP"
    const val ACTION_SYNC = "com.skai.taskmanager.session.SYNC"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_MINUTES = "extra_minutes"

    // Popup activity extras
    const val EXTRA_POPUP_TYPE = "extra_popup_type"
    const val POPUP_WARNING = "warning"
    const val POPUP_COMPLETE = "complete"

}
