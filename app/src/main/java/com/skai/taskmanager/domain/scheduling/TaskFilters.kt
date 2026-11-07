package com.skai.taskmanager.domain.scheduling

import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.util.DateUtils

/**
 * The single definition of "what belongs on today's plate", shared by the Today screen and the
 * persistent notification so they can never drift apart. (The daily rollover in
 * [com.skai.taskmanager.data.repository.TaskRepository.performDayRollover] archives leftovers
 * from previous days, so "overdue" tasks only appear here until the next midnight.)
 */
object TaskFilters {

    /** Unfinished tasks for today: undated, due today, or still-overdue (until rollover files them). */
    fun isActiveToday(task: Task, endOfToday: Long = DateUtils.endOfToday()): Boolean =
        !task.isCompleted && !task.archived && (task.dueDate == null || task.dueDate <= endOfToday)

    /** Tasks completed today that haven't been moved to History yet. */
    fun isCompletedToday(task: Task): Boolean =
        task.isCompleted && !task.archived &&
            task.completedAt != null && DateUtils.isToday(task.completedAt)

    /** Everything the Today screen / notification shows: to-dos plus today's completions. */
    fun isOnTodayPlate(task: Task, endOfToday: Long = DateUtils.endOfToday()): Boolean =
        isActiveToday(task, endOfToday) || isCompletedToday(task)
}
