package com.skai.taskmanager.domain.scheduling

import com.skai.taskmanager.domain.model.Priority
import com.skai.taskmanager.domain.model.Task

/**
 * Smart ordering for a task list:
 *  1. High-priority **untimed** tasks first (urgent things to surface, e.g. "call mom").
 *  2. **Timed** tasks next, in chronological order (1pm lunch, 2pm walking, 3pm work).
 *  3. Everything else (untimed, non-high) after, by priority then title.
 *  4. Completed tasks always sink to the bottom.
 *
 * Example: an incomplete High "call mom" (untimed) + lunch@13:00, walking@14:00, work@15:00
 * orders as: call mom, lunch, walking, work.
 */
object TaskOrdering {

    private const val RANK_HIGH_UNTIMED = 0
    private const val RANK_TIMED = 1
    private const val RANK_OTHER = 2

    private fun rank(task: Task): Int = when {
        task.isCompleted -> RANK_OTHER
        task.priority == Priority.HIGH && task.timeMinutes == null -> RANK_HIGH_UNTIMED
        task.timeMinutes != null -> RANK_TIMED
        else -> RANK_OTHER
    }

    val comparator: Comparator<Task> = compareBy<Task> { it.isCompleted }
        .thenBy { rank(it) }
        .thenBy { it.timeMinutes ?: Int.MAX_VALUE }
        .thenBy { it.priority.sortWeight }
        .thenBy { it.title.lowercase() }

    fun order(tasks: List<Task>): List<Task> = tasks.sortedWith(comparator)
}
