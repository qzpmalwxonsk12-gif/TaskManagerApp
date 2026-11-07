package com.skai.taskmanager.data.mapper

import com.skai.taskmanager.data.local.entity.TaskEntity
import com.skai.taskmanager.data.local.entity.TaskTemplateEntity
import com.skai.taskmanager.domain.model.RecurrenceRule
import com.skai.taskmanager.domain.model.SessionState
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.model.TaskAlert

/** Maps a persisted [TaskEntity] to the domain [Task]. */
fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    dueDate = dueDate,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    isRecurring = isRecurring,
    recurrenceRule = recurrenceType?.let { RecurrenceRule(it, recurrenceIntervalDays) },
    templateId = templateId,
    periodId = periodId,
    timeMinutes = timeMinutes,
    alertType = TaskAlert.fromName(alertType),
    alertSoundUri = alertSoundUri,
    archived = archived,
    skipped = skipped,
    sessionDurationMinutes = sessionDurationMinutes,
    sessionState = SessionState.fromName(sessionState),
    sessionStartTime = sessionStartTime,
    sessionEndTime = sessionEndTime,
    sessionRemainingSeconds = sessionRemainingSeconds,
    enableDndDuringSession = enableDndDuringSession,
    autoCompleteOnTimerEnd = autoCompleteOnTimerEnd,
    notificationProfile = notificationProfile,
    sessionWarningAlert = TaskAlert.fromName(sessionWarningAlertType),
    sessionEndAlert = TaskAlert.fromName(sessionEndAlertType),
    sessionScheduledSeconds = sessionScheduledSeconds
)

/** Maps a domain [Task] to its persisted [TaskEntity]. */
fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    dueDate = dueDate,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    isRecurring = isRecurring,
    recurrenceType = recurrenceRule?.type,
    recurrenceIntervalDays = recurrenceRule?.intervalDays ?: 1,
    templateId = templateId,
    periodId = periodId,
    timeMinutes = timeMinutes,
    alertType = alertType.name,
    alertSoundUri = alertSoundUri,
    archived = archived,
    skipped = skipped,
    sessionDurationMinutes = sessionDurationMinutes,
    sessionState = sessionState.name,
    sessionStartTime = sessionStartTime,
    sessionEndTime = sessionEndTime,
    sessionRemainingSeconds = sessionRemainingSeconds,
    enableDndDuringSession = enableDndDuringSession,
    autoCompleteOnTimerEnd = autoCompleteOnTimerEnd,
    notificationProfile = notificationProfile,
    sessionWarningAlertType = sessionWarningAlert.name,
    sessionEndAlertType = sessionEndAlert.name,
    sessionScheduledSeconds = sessionScheduledSeconds
)

/** Builds the recurring template that backs a recurring [Task]. */
fun Task.toTemplateEntity(templateId: Long = 0L): TaskTemplateEntity {
    val rule = requireNotNull(recurrenceRule) {
        "Cannot build a template for a non-recurring task"
    }
    return TaskTemplateEntity(
        id = templateId,
        title = title,
        description = description,
        category = category,
        priority = priority,
        recurrenceType = rule.type,
        recurrenceIntervalDays = rule.intervalDays,
        createdAt = createdAt,
        isActive = true
    )
}

fun List<TaskEntity>.toDomain(): List<Task> = map { it.toDomain() }
