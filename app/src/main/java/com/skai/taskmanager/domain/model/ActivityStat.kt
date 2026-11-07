package com.skai.taskmanager.domain.model

/**
 * Aggregated focus time for one activity (grouped by Session Task title) over a period.
 * Populated directly from a Room aggregate query.
 */
data class ActivityStat(
    val name: String,
    val totalSeconds: Long
)

/** Time window for the Statistics screen. Designed to be extended (yearly, custom ranges, …). */
enum class StatRange {
    DAILY,
    WEEKLY,
    MONTHLY
}
