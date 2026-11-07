package com.skai.taskmanager.domain.model

/**
 * Global app settings (single-row). Covers Session/Focus preferences and reminder options.
 */
data class AppSettings(
    val id: Int = SINGLETON_ID,
    /** User's chosen default for enabling Do Not Disturb while a session runs. */
    val dndDuringSession: Boolean = false,
    /** Whether the first-time DND prompt has been answered (or dismissed forever). */
    val dndPromptAnswered: Boolean = false,
    val autoCompleteOnTimerEnd: Boolean = false,
    /** Quick session-extension durations (minutes) used in popups and the session notification. */
    val ext1: Int = 5,
    val ext2: Int = 10,
    val ext3: Int = 15,
    /** Saved notification interruption filter to restore when a session ends, or null. */
    val savedDndFilter: Int? = null,

    // ---- Reminders ----
    /** Pop up a reminder notification when a timed task's time arrives. */
    val taskRemindersEnabled: Boolean = true,
    /** How long a task reminder stays on screen before auto-dismissing, in seconds. */
    val taskReminderSeconds: Int = DEFAULT_REMINDER_SECONDS,
    /** Periodically ping a notification reminding the user to check their notes. */
    val notePingEnabled: Boolean = false,
    /** Minutes between note pings. */
    val notePingMinutes: Int = DEFAULT_NOTE_PING_MINUTES,
    /** The text shown by the note ping (user-customizable prompt). */
    val notePingMessage: String = DEFAULT_NOTE_PING_MESSAGE,
    /** How many minutes before a focus session ends the "almost done" warning fires. */
    val sessionWarningMinutes: Int = DEFAULT_SESSION_WARNING_MINUTES,
    /** Minutes a snoozed task reminder waits before popping up again. */
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    /** When false, the task-list notification can be swiped away; it re-appears together with
     * each task reminder instead of being pinned permanently. */
    val persistentTaskList: Boolean = true
) {
    val extensions: List<Int> get() = listOf(ext1, ext2, ext3)

    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_REMINDER_SECONDS = 120
        const val DEFAULT_NOTE_PING_MINUTES = 30
        const val DEFAULT_NOTE_PING_MESSAGE = "Check your notes"
        const val DEFAULT_SESSION_WARNING_MINUTES = 5
        const val DEFAULT_SNOOZE_MINUTES = 10
        val DEFAULT = AppSettings()
    }
}
