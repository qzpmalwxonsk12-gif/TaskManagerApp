package com.skai.taskmanager.data.repository

import com.skai.taskmanager.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for tasks. The UI/ViewModels depend on this abstraction, not on Room.
 */
interface TaskRepository {

    fun observeAllTasks(): Flow<List<Task>>

    /** One-shot snapshot of all tasks (used by the notification's RemoteViews factory). */
    suspend fun getAllTasksOnce(): List<Task>

    /** Tasks assigned to a scheduling period (used by the Schedule editor). */
    fun observeTasksForPeriod(periodId: Int): Flow<List<Task>>

    /** Detaches all tasks from [periodId] (called when a period is deleted). */
    suspend fun clearPeriodAssignment(periodId: Int)

    /** Moves today's completed tasks out of Today into History (archives, never deletes). */
    suspend fun archiveCompletedToday()

    /**
     * Rolls the task list over to a new day. Idempotent, safe to call any time:
     *  - tasks completed on a previous day are archived (leave Today, stay in History);
     *  - unfinished tasks from previous days are archived as "missed" (visible in History);
     *  - each recurring task whose instance was missed gets exactly one fresh instance at its
     *    next occurrence, so routines continue without ever showing duplicates.
     */
    suspend fun performDayRollover()

    /** Updates a task's focus-session state (used by the SessionService). */
    suspend fun updateSession(
        taskId: Long,
        state: com.skai.taskmanager.domain.model.SessionState,
        startTime: Long?,
        endTime: Long?,
        remainingSeconds: Int?
    )

    /** Sets total scheduled focus seconds (original + extensions) for a session. */
    suspend fun setSessionScheduled(taskId: Long, seconds: Int)

    /** History entries: completed tasks plus archived (missed) tasks. */
    fun observeHistoryTasks(): Flow<List<Task>>

    fun observeTask(taskId: Long): Flow<Task?>

    suspend fun getTask(taskId: Long): Task?

    /**
     * Adds a task. If the task is recurring, a backing template is created first and the
     * inserted task becomes the first *instance* pointing to that template.
     * @return the id of the inserted task instance.
     */
    suspend fun addTask(task: Task): Long

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(task: Task)

    /**
     * Toggles completion of a single instance. Completing a recurring instance also generates
     * the next pending occurrence (if none already exists) so the pattern continues, without
     * mutating the template.
     */
    suspend fun setTaskCompleted(task: Task, completed: Boolean)

    /** Current "completed in a row" streak for a recurring task's template. */
    suspend fun getStreakForTemplate(templateId: Long): Int

    /**
     * Skips a task for the day: it leaves Today immediately and appears in History as
     * "Skipped". A recurring task gets its next occurrence (tomorrow for daily rules), so
     * the routine continues.
     */
    suspend fun skipTaskForToday(taskId: Long)
}
