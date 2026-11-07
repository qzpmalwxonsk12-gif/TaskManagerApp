package com.skai.taskmanager.domain.model

/**
 * How a task announces itself — used by the task-time reminder pop-up and by the focus-session
 * warning/end alerts. Four built-in profiles plus a user-picked custom sound.
 *
 * [severity] orders profiles by loudness so that when several tasks fire at once, the reminder
 * plays the strongest one.
 */
enum class TaskAlert(val displayName: String, val description: String, val severity: Int) {
    NONE("Silent", "No sound or vibration", 0),
    VIBRATE("Vibrate", "A short buzz", 1),
    CHIME("Chime", "Vibration plus a soft chime", 2),
    MELODY("Melody", "Long vibration and a short melody", 3),
    ALARM("Alarm", "Loud alarm with constant vibration", 5),
    CUSTOM("Custom sound", "A sound file you pick", 4);

    companion object {
        /** Default for task-time reminders (closest to the old default-notification behavior). */
        val DEFAULT = CHIME

        /** Default for the session 5-minute warning (a nudge, not an alarm). */
        val DEFAULT_SESSION_WARNING = VIBRATE

        /** Default for the session-end alert. */
        val DEFAULT_SESSION_END = CHIME

        fun fromName(name: String?): TaskAlert =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
