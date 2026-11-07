package com.skai.taskmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.skai.taskmanager.domain.model.Category
import com.skai.taskmanager.domain.model.Priority
import com.skai.taskmanager.domain.model.RecurrenceType

/**
 * A concrete task occurrence (or a one-off task). For recurring tasks this is an *instance*
 * generated from a [TaskTemplateEntity]; the link is held in [templateId].
 *
 * The recurrence rule is duplicated onto the instance (via [recurrenceType] /
 * [recurrenceIntervalDays]) so that an individual occurrence carries its own rule, per spec,
 * even though scheduling is not fully wired in Stage 1.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index("isCompleted"),
        Index("dueDate"),
        Index("templateId")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val category: Category,
    val priority: Priority,
    val dueDate: Long?,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val createdAt: Long,
    val isRecurring: Boolean,
    val recurrenceType: RecurrenceType?,
    val recurrenceIntervalDays: Int,
    val templateId: Long?,
    /** The scheduling period this task belongs to, or null for an "Anytime" task. */
    val periodId: Int? = null,
    /** Time of day in minutes since midnight, or null if untimed. */
    val timeMinutes: Int? = null,
    /** [com.skai.taskmanager.domain.model.TaskAlert] name for the task-time reminder. */
    @ColumnInfo(defaultValue = "'CHIME'")
    val alertType: String = "CHIME",
    /** User-picked custom alert sound (content URI), or null. */
    val alertSoundUri: String? = null,
    /** Whether a completed task has been archived out of Today into History. */
    @ColumnInfo(defaultValue = "0")
    val archived: Boolean = false,
    /** Whether the user skipped this occurrence for the day. */
    @ColumnInfo(defaultValue = "0")
    val skipped: Boolean = false,

    // ---- Session Task fields ----
    val sessionDurationMinutes: Int? = null,
    @ColumnInfo(defaultValue = "'IDLE'")
    val sessionState: String = "IDLE",
    val sessionStartTime: Long? = null,
    val sessionEndTime: Long? = null,
    val sessionRemainingSeconds: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val enableDndDuringSession: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val autoCompleteOnTimerEnd: Boolean = false,
    val notificationProfile: String? = null,
    val sessionScheduledSeconds: Int? = null,
    @ColumnInfo(defaultValue = "'VIBRATE'")
    val sessionWarningAlertType: String = "VIBRATE",
    @ColumnInfo(defaultValue = "'CHIME'")
    val sessionEndAlertType: String = "CHIME"
)
