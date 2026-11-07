package com.skai.taskmanager.notification

import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.model.TimePeriod
import com.skai.taskmanager.domain.scheduling.SchedulingEngine
import com.skai.taskmanager.domain.scheduling.TaskFilters
import com.skai.taskmanager.domain.scheduling.TaskOrdering
import com.skai.taskmanager.util.DateUtils
import java.time.Instant
import java.time.ZoneId

/**
 * The computed, ready-to-render notification content for the current moment. Built purely from
 * the tasks, the configured periods, and the clock — so the foreground service (collapsed view)
 * and the RemoteViews factory (the list) derive identical content from the single source of
 * truth, the database.
 *
 * If no period is active (a gap in the schedule, or all periods disabled/deleted), it gracefully
 * falls back to "Today's Tasks" with a "No Active Period" label rather than failing.
 */
data class PeriodContent(
    /** The active period, or null when none applies (fallback mode). */
    val activePeriod: TimePeriod?,
    val title: String,
    val label: String?,
    val periodTasks: List<Task>,
    val completedCount: Int,
    val totalCount: Int
) {
    val hasActivePeriod: Boolean get() = activePeriod != null
    val progressText: String get() = "$completedCount/$totalCount"
    val previewText: String
        get() = periodTasks.firstOrNull { !it.isCompleted }?.title
            ?: if (totalCount == 0) "No tasks for now" else "All done"
}

object NotificationContent {

    const val FALLBACK_TITLE = "Today's Tasks"
    const val NO_ACTIVE_PERIOD_LABEL = "No Active Period"

    fun build(
        allTasks: List<Task>,
        periods: List<TimePeriod>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): PeriodContent {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val active = SchedulingEngine.activePeriod(periods, now.toLocalTime())

        // Same "today's plate" definition as the Today screen (TaskFilters), so the notification
        // and the app always show the same tasks.
        val endOfToday = DateUtils.endOfToday()
        val todays = allTasks.filter { TaskFilters.isOnTodayPlate(it, endOfToday) }

        // When a period is active, narrow to that period's tasks plus "Anytime" (null) tasks.
        val scoped = if (active != null) {
            todays.filter { it.periodId == active.id || it.periodId == null }
        } else {
            todays
        }

        // Incomplete tasks first, completed ones sink to the bottom (TaskOrdering already does
        // this). The service renders up to a cap: while everything fits, completing a task just
        // moves it to the bottom; only once there are more tasks than fit do completed ones get
        // pushed off the end to make room for the remaining to-dos.
        val ordered = TaskOrdering.order(scoped)

        return PeriodContent(
            activePeriod = active,
            title = active?.name ?: FALLBACK_TITLE,
            label = if (active == null) NO_ACTIVE_PERIOD_LABEL else null,
            periodTasks = ordered,
            completedCount = scoped.count { it.isCompleted },
            totalCount = scoped.size
        )
    }
}
