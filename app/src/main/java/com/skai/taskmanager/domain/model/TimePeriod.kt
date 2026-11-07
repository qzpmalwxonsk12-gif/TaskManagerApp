package com.skai.taskmanager.domain.model

import java.time.LocalTime

/**
 * A user-configurable slice of the day (e.g. "Morning"). This is the central unit of the
 * Stage 3 scheduling engine; tasks reference a period by [id] rather than by name so periods can
 * be renamed, reordered, enabled/disabled, or retimed without touching tasks.
 *
 * Periods are half-open ranges `[start, end)` within a single day (end must be after start, so a
 * period never wraps past midnight). Ordering for display and transitions is by [sortOrder].
 */
data class TimePeriod(
    val id: Int = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isEnabled: Boolean = true,
    val sortOrder: Int
) {
    val startMinutes: Int get() = startHour * 60 + startMinute
    val endMinutes: Int get() = endHour * 60 + endMinute

    val startTime: LocalTime get() = LocalTime.of(startHour.coerceIn(0, 23), startMinute.coerceIn(0, 59))
    val endTime: LocalTime get() = LocalTime.of(endHour.coerceIn(0, 23), endMinute.coerceIn(0, 59))

    /** Whether a wall-clock [time] falls within this period's half-open `[start, end)` range. */
    fun contains(time: LocalTime): Boolean {
        val m = time.hour * 60 + time.minute
        return m in startMinutes until endMinutes
    }

    fun rangeLabel(): String =
        "%02d:%02d–%02d:%02d".format(startHour, startMinute, endHour, endMinute)

    companion object {
        const val MAX_NAME_LENGTH = 20
    }
}
