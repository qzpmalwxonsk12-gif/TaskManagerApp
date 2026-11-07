package com.skai.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.skai.taskmanager.data.local.entity.TaskEntity
import com.skai.taskmanager.data.local.entity.TaskTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ---- Task instances ------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    /** One-shot snapshot of all tasks. Used by the notification's RemoteViews factory, which
     * needs a synchronous read off the binder thread. */
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    /** Tasks assigned to a given scheduling period (used by the Schedule editor). */
    @Query("SELECT * FROM tasks WHERE periodId = :periodId ORDER BY isCompleted ASC, createdAt DESC")
    fun observeTasksForPeriod(periodId: Int): Flow<List<TaskEntity>>

    /** Detaches tasks from a period that is being deleted, turning them into "Anytime" tasks. */
    @Query("UPDATE tasks SET periodId = NULL WHERE periodId = :periodId")
    suspend fun clearPeriodAssignment(periodId: Int)

    /** Archives tasks completed within [start]..[end] so they leave Today but stay in History. */
    @Query(
        "UPDATE tasks SET archived = 1 " +
            "WHERE isCompleted = 1 AND archived = 0 AND completedAt BETWEEN :start AND :end"
    )
    suspend fun archiveCompletedBetween(start: Long, end: Long)

    // ---- Day rollover --------------------------------------------------------------------

    /** Archives tasks completed on a previous day (they leave Today, stay in History). */
    @Query(
        "UPDATE tasks SET archived = 1 " +
            "WHERE isCompleted = 1 AND archived = 0 AND completedAt < :startOfToday"
    )
    suspend fun archiveCompletedBefore(startOfToday: Long)

    /**
     * Unfinished tasks left over from previous days: dated tasks that are now overdue, plus
     * undated recurring instances created before today (they were on a previous day's plate).
     * Undated one-off tasks are deliberately excluded — they are an ongoing backlog.
     */
    @Query(
        "SELECT * FROM tasks WHERE isCompleted = 0 AND archived = 0 AND (" +
            "(dueDate IS NOT NULL AND dueDate < :startOfToday) OR " +
            "(dueDate IS NULL AND isRecurring = 1 AND createdAt < :startOfToday))"
    )
    suspend fun getUnfinishedBefore(startOfToday: Long): List<TaskEntity>

    /** Archives the given tasks (used to file missed tasks into History at day rollover). */
    @Query("UPDATE tasks SET archived = 1 WHERE id IN (:taskIds)")
    suspend fun archiveTasks(taskIds: List<Long>)

    /** Files a task into History as skipped-for-today. */
    @Query("UPDATE tasks SET archived = 1, skipped = 1 WHERE id = :taskId")
    suspend fun markSkipped(taskId: Long)

    /** Active instances of a template that are already scheduled for today or later. Prevents
     * the rollover from creating a duplicate occurrence. */
    @Query(
        "SELECT COUNT(*) FROM tasks WHERE templateId = :templateId " +
            "AND isCompleted = 0 AND archived = 0 " +
            "AND (dueDate >= :startOfToday OR (dueDate IS NULL AND createdAt >= :startOfToday))"
    )
    suspend fun countUpcomingInstancesForTemplate(templateId: Long, startOfToday: Long): Int

    /** Updates only a task's focus-session fields (used by the SessionService). */
    @Query(
        "UPDATE tasks SET sessionState = :state, sessionStartTime = :startTime, " +
            "sessionEndTime = :endTime, sessionRemainingSeconds = :remaining WHERE id = :taskId"
    )
    suspend fun updateSession(
        taskId: Long,
        state: String,
        startTime: Long?,
        endTime: Long?,
        remaining: Int?
    )

    /** Sets the running total of scheduled focus seconds (original + extensions). */
    @Query("UPDATE tasks SET sessionScheduledSeconds = :seconds WHERE id = :taskId")
    suspend fun setSessionScheduled(taskId: Long, seconds: Int)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    /** Everything that belongs in History: completed tasks plus archived (missed) ones. */
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 OR archived = 1 ORDER BY completedAt DESC")
    fun observeHistoryTasks(): Flow<List<TaskEntity>>

    /**
     * Completed instances for a given template, newest first. Used to compute streaks for
     * recurring tasks in the History screen.
     */
    @Query(
        "SELECT * FROM tasks WHERE templateId = :templateId AND isCompleted = 1 " +
            "ORDER BY completedAt DESC"
    )
    suspend fun getCompletedInstancesForTemplate(templateId: Long): List<TaskEntity>

    /** Number of not-yet-completed, still-visible instances for a template. Used to avoid
     * generating a duplicate next occurrence when one is already pending. (Archived misses
     * don't count — they live in History and shouldn't block the pattern from continuing.) */
    @Query(
        "SELECT COUNT(*) FROM tasks WHERE templateId = :templateId " +
            "AND isCompleted = 0 AND archived = 0"
    )
    suspend fun countActiveInstancesForTemplate(templateId: Long): Int

    // ---- Recurring templates -------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TaskTemplateEntity)

    @Query("SELECT * FROM task_templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: Long): TaskTemplateEntity?

    @Query("DELETE FROM task_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: Long)
}
