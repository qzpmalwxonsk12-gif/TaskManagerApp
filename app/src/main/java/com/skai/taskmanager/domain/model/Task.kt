package com.skai.taskmanager.domain.model

/**
 * Domain representation of a task. This is what the UI layer works with; the data layer
 * maps it to/from [com.skai.taskmanager.data.local.entity.TaskEntity].
 *
 * Recurring tasks: a recurring task is created from a *template*
 * ([com.skai.taskmanager.data.local.entity.TaskTemplateEntity]) which stores the
 * repeating pattern. Each concrete occurrence is a separate [Task] *instance* that points
 * back to its template via [templateId]. Skipping/postponing/completing a single instance
 * therefore never mutates the underlying pattern.
 */
data class Task(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: Category = Category.DEFAULT,
    val priority: Priority = Priority.DEFAULT,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    /** Id of the [com.skai.taskmanager.data.local.entity.TaskTemplateEntity] this
     * instance was generated from, or null for one-off tasks. */
    val templateId: Long? = null,
    /** The scheduling period this task is assigned to, or null for an "Anytime" task. */
    val periodId: Int? = null,
    /** Time of day for the task as minutes since midnight (e.g. 13:00 = 780), or null if untimed.
     * Timed tasks are slotted into the list at their time. */
    val timeMinutes: Int? = null,
    /** How the task-time reminder announces itself (sound/vibration profile). */
    val alertType: TaskAlert = TaskAlert.DEFAULT,
    /** User-picked sound file for [TaskAlert.CUSTOM]; shared by all of this task's alerts. */
    val alertSoundUri: String? = null,
    /** When true, a completed task has been moved out of "Today" into History (kept, not deleted). */
    val archived: Boolean = false,
    /** True when the user skipped this occurrence for the day (archived, shown as "Skipped"). */
    val skipped: Boolean = false,

    // ---- Session Task (Focus Mode) fields. All optional; a task with no duration is a normal task.
    val sessionDurationMinutes: Int? = null,
    val sessionState: SessionState = SessionState.IDLE,
    /** Epoch millis the session was first started. */
    val sessionStartTime: Long? = null,
    /** Epoch millis the running countdown will reach zero (RUNNING only). */
    val sessionEndTime: Long? = null,
    /** Frozen remaining seconds while PAUSED (and last-known remaining otherwise). */
    val sessionRemainingSeconds: Int? = null,
    val enableDndDuringSession: Boolean = false,
    val autoCompleteOnTimerEnd: Boolean = false,
    val notificationProfile: String? = null,
    /** Alert played at the session's "almost done" warning. */
    val sessionWarningAlert: TaskAlert = TaskAlert.DEFAULT_SESSION_WARNING,
    /** Alert played when the session timer reaches zero. */
    val sessionEndAlert: TaskAlert = TaskAlert.DEFAULT_SESSION_END,
    /** Total scheduled focus seconds = original duration + all extensions (excludes paused time).
     * Used to record the final focused duration in statistics. */
    val sessionScheduledSeconds: Int? = null
) {
    val hasDueDate: Boolean get() = dueDate != null
    val hasTime: Boolean get() = timeMinutes != null

    /** A task is a Session Task once it has a duration. */
    val isSessionTask: Boolean get() = sessionDurationMinutes != null
}
