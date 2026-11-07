package com.skai.taskmanager.data.repository

import com.skai.taskmanager.data.local.dao.TaskDao
import com.skai.taskmanager.data.mapper.toDomain
import com.skai.taskmanager.data.mapper.toEntity
import com.skai.taskmanager.data.mapper.toTemplateEntity
import com.skai.taskmanager.domain.model.RecurrenceRule
import com.skai.taskmanager.domain.model.SessionState
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao
) : TaskRepository {

    override fun observeAllTasks(): Flow<List<Task>> =
        dao.observeAllTasks().map { it.toDomain() }

    override suspend fun getAllTasksOnce(): List<Task> =
        dao.getAllTasksOnce().toDomain()

    override fun observeTasksForPeriod(periodId: Int): Flow<List<Task>> =
        dao.observeTasksForPeriod(periodId).map { it.toDomain() }

    override suspend fun clearPeriodAssignment(periodId: Int) =
        dao.clearPeriodAssignment(periodId)

    override suspend fun archiveCompletedToday() =
        dao.archiveCompletedBetween(DateUtils.startOfToday(), DateUtils.endOfToday())

    override suspend fun performDayRollover() {
        val startOfToday = DateUtils.startOfToday()

        // 1) Yesterday's completed tasks leave Today automatically (they stay in History).
        dao.archiveCompletedBefore(startOfToday)

        // 2) Unfinished tasks from previous days are filed into History as missed…
        val missed = dao.getUnfinishedBefore(startOfToday).toDomain()
        if (missed.isEmpty()) return
        dao.archiveTasks(missed.map { it.id })

        // 3) …and each recurring routine gets exactly one fresh instance so it shows up again
        // (never two: stale instances were just archived, and templates that already have an
        // upcoming instance are skipped).
        val rolledTemplates = mutableSetOf<Long>()
        for (task in missed) {
            val rule = task.recurrenceRule ?: continue
            if (!task.isRecurring) continue
            val templateKey = task.templateId ?: -task.id
            if (!rolledTemplates.add(templateKey)) continue
            if (task.templateId != null &&
                dao.countUpcomingInstancesForTemplate(task.templateId, startOfToday) > 0
            ) {
                continue
            }
            dao.insertTask(
                freshInstanceOf(task, dueDate = nextOccurrenceOnOrAfter(task.dueDate, rule, startOfToday))
            )
        }
    }

    override suspend fun skipTaskForToday(taskId: Long) {
        val task = dao.getTaskById(taskId)?.toDomain() ?: return
        if (task.isCompleted || task.archived) return
        dao.markSkipped(task.id)

        // Keep the routine going: the skipped occurrence is replaced by the next one
        // (strictly after today, so it doesn't just pop straight back).
        val rule = task.recurrenceRule
        if (task.isRecurring && rule != null) {
            val startOfTomorrow = DateUtils.startOfTomorrow()
            val hasUpcoming = task.templateId != null &&
                dao.countUpcomingInstancesForTemplate(task.templateId, startOfTomorrow) > 0
            if (!hasUpcoming) {
                dao.insertTask(
                    freshInstanceOf(task, dueDate = nextOccurrenceOnOrAfter(task.dueDate, rule, startOfTomorrow))
                )
            }
        }
    }

    /** First occurrence on/after [startOfToday], stepping from the missed [dueDate] by the
     * rule's interval so the cadence is preserved (an undated instance restarts today). */
    private fun nextOccurrenceOnOrAfter(dueDate: Long?, rule: RecurrenceRule, startOfToday: Long): Long {
        val intervalMillis = rule.nextIntervalDays() * MILLIS_PER_DAY
        var next = dueDate?.plus(intervalMillis) ?: startOfToday
        while (next < startOfToday) next += intervalMillis
        return DateUtils.startOfDay(next)
    }

    /** A brand-new pending instance of [task]: completion, archive and session state all reset. */
    private fun freshInstanceOf(task: Task, dueDate: Long?) = task.copy(
        id = 0L,
        isCompleted = false,
        completedAt = null,
        createdAt = System.currentTimeMillis(),
        dueDate = dueDate,
        archived = false,
        skipped = false,
        sessionState = SessionState.IDLE,
        sessionStartTime = null,
        sessionEndTime = null,
        sessionRemainingSeconds = null,
        sessionScheduledSeconds = null
    ).toEntity()

    override suspend fun updateSession(
        taskId: Long,
        state: SessionState,
        startTime: Long?,
        endTime: Long?,
        remainingSeconds: Int?
    ) = dao.updateSession(taskId, state.name, startTime, endTime, remainingSeconds)

    override suspend fun setSessionScheduled(taskId: Long, seconds: Int) =
        dao.setSessionScheduled(taskId, seconds)

    override fun observeHistoryTasks(): Flow<List<Task>> =
        dao.observeHistoryTasks().map { it.toDomain() }

    override fun observeTask(taskId: Long): Flow<Task?> =
        dao.observeTaskById(taskId).map { it?.toDomain() }

    override suspend fun getTask(taskId: Long): Task? =
        dao.getTaskById(taskId)?.toDomain()

    override suspend fun addTask(task: Task): Long {
        return if (task.isRecurring && task.recurrenceRule != null) {
            val templateId = dao.insertTemplate(task.toTemplateEntity())
            dao.insertTask(task.copy(templateId = templateId).toEntity())
        } else {
            dao.insertTask(task.toEntity())
        }
    }

    override suspend fun updateTask(task: Task) {
        var toSave = task
        val rule = task.recurrenceRule
        if (task.isRecurring && rule != null) {
            val templateId = task.templateId
            if (templateId == null) {
                // Task was made recurring during this edit — create its backing template now.
                val newTemplateId = dao.insertTemplate(task.toTemplateEntity())
                toSave = task.copy(templateId = newTemplateId)
            } else {
                // Keep the pattern's content in sync, but never its per-instance schedule
                // (templates carry no due date, so postponing one instance can't leak in).
                dao.getTemplateById(templateId)?.let { existing ->
                    dao.updateTemplate(task.toTemplateEntity(existing.id))
                }
            }
        }
        dao.updateTask(toSave.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        dao.deleteTask(task.toEntity())
    }

    override suspend fun setTaskCompleted(task: Task, completed: Boolean) {
        val now = System.currentTimeMillis()
        val updated = task.copy(
            isCompleted = completed,
            completedAt = if (completed) now else null
        )
        dao.updateTask(updated.toEntity())

        val templateId = task.templateId
        val rule = task.recurrenceRule
        if (completed && task.isRecurring && templateId != null && rule != null) {
            // Only roll the pattern forward if there isn't already a pending occurrence.
            if (dao.countActiveInstancesForTemplate(templateId) == 0) {
                val base = task.dueDate ?: now
                val nextDue = DateUtils.startOfDay(base + rule.nextIntervalDays() * MILLIS_PER_DAY)
                dao.insertTask(freshInstanceOf(task, dueDate = nextDue))
            }
        }
    }

    override suspend fun getStreakForTemplate(templateId: Long): Int {
        val template = dao.getTemplateById(templateId) ?: return 0
        val completions = dao.getCompletedInstancesForTemplate(templateId)
            .mapNotNull { it.completedAt }
            .sortedDescending()
        if (completions.isEmpty()) return 0

        val expectedDays = RecurrenceRule(
            template.recurrenceType,
            template.recurrenceIntervalDays
        ).nextIntervalDays()
        // Generous tolerance so an early/late completion doesn't break the streak.
        val toleranceMillis = expectedDays * 2L * MILLIS_PER_DAY

        var streak = 1
        for (i in 1 until completions.size) {
            val gap = completions[i - 1] - completions[i]
            if (gap <= toleranceMillis) streak++ else break
        }
        return streak
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
