package com.skai.taskmanager.domain.model

/**
 * How often a recurring task repeats.
 *
 * For [CUSTOM] the actual cadence is defined by [RecurrenceRule.intervalDays].
 */
enum class RecurrenceType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom");
}

/**
 * The full recurrence specification for a task.
 *
 * The rule is stored even though full recurrence scheduling is not wired up in Stage 1.
 * [intervalDays] is only meaningful for [RecurrenceType.CUSTOM] but is always populated
 * so the value survives a round-trip through the database.
 */
data class RecurrenceRule(
    val type: RecurrenceType,
    val intervalDays: Int = 1
) {
    /** Number of days until the next occurrence, derived from [type]. */
    fun nextIntervalDays(): Int = when (type) {
        RecurrenceType.DAILY -> 1
        RecurrenceType.WEEKLY -> 7
        RecurrenceType.MONTHLY -> 30
        RecurrenceType.CUSTOM -> intervalDays.coerceAtLeast(1)
    }

    fun describe(): String = when (type) {
        RecurrenceType.CUSTOM -> "Every ${intervalDays.coerceAtLeast(1)} day(s)"
        else -> type.displayName
    }

    companion object {
        val DAILY = RecurrenceRule(RecurrenceType.DAILY)
    }
}
