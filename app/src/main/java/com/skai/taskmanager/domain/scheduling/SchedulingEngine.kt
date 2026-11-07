package com.skai.taskmanager.domain.scheduling

import com.skai.taskmanager.domain.model.TimePeriod
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure, stateless core of the scheduling subsystem. Every part of the app that reasons about
 * "which period are we in" or "when does the next period boundary occur" goes through here, so
 * future features (Session Tasks, Timetables, Focus Mode, Automatic Day Progression, Calendar
 * Integration, …) can build on a single, well-defined engine rather than ad-hoc logic.
 */
object SchedulingEngine {

    /**
     * The enabled period that contains [time], or null if none does (a gap, or all disabled).
     * Periods are validated to be non-overlapping; if two ever do, the lowest [TimePeriod.sortOrder]
     * wins for determinism.
     */
    fun activePeriod(periods: List<TimePeriod>, time: LocalTime): TimePeriod? =
        periods.asSequence()
            .filter { it.isEnabled }
            .sortedBy { it.sortOrder }
            .firstOrNull { it.contains(time) }

    /** Whether two periods overlap as half-open ranges (touching boundaries do not count). */
    fun overlaps(aStartM: Int, aEndM: Int, bStartM: Int, bEndM: Int): Boolean =
        aStartM < bEndM && bStartM < aEndM

    /**
     * The first existing period (other than [excludeId]) whose range overlaps the candidate
     * range, or null if the candidate fits cleanly. Disabled periods are included so re-enabling
     * can never produce a conflict.
     */
    fun firstOverlap(
        periods: List<TimePeriod>,
        candidateStartM: Int,
        candidateEndM: Int,
        excludeId: Int?
    ): TimePeriod? = periods.firstOrNull {
        it.id != excludeId && overlaps(it.startMinutes, it.endMinutes, candidateStartM, candidateEndM)
    }

    /**
     * Epoch millis of the next period boundary (any enabled period's start or end) strictly after
     * [nowMillis]. Used to schedule the notification's transition alarm. Falls back to the earliest
     * boundary tomorrow, or +24h if there are no enabled periods at all.
     */
    fun nextBoundaryMillis(
        periods: List<TimePeriod>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val today = now.toLocalDate()

        val boundaries = periods.asSequence()
            .filter { it.isEnabled }
            .flatMap { sequenceOf(it.startTime, it.endTime) }
            .distinct()
            .sorted()
            .toList()

        if (boundaries.isEmpty()) return nowMillis + 24L * 60L * 60L * 1000L

        for (boundary in boundaries) {
            val candidate = today.atTime(boundary).atZone(zone).toInstant().toEpochMilli()
            if (candidate > nowMillis) return candidate
        }
        return today.plusDays(1).atTime(boundaries.first()).atZone(zone).toInstant().toEpochMilli()
    }

    /** Validates a candidate period, returning a structured result with any field errors. */
    fun validate(
        name: String,
        startMinutes: Int,
        endMinutes: Int,
        existingPeriods: List<TimePeriod>,
        editingId: Int?
    ): ValidationResult {
        val trimmed = name.trim()
        val nameError = when {
            trimmed.isEmpty() -> "Name is required"
            trimmed.length > TimePeriod.MAX_NAME_LENGTH ->
                "Name must be ${TimePeriod.MAX_NAME_LENGTH} characters or fewer"
            else -> null
        }
        val timeError = if (endMinutes <= startMinutes) "End time must be after start time" else null
        val overlapError = if (timeError == null) {
            firstOverlap(existingPeriods, startMinutes, endMinutes, editingId)
                ?.let { "Overlaps with \"${it.name}\"" }
        } else {
            null
        }
        return ValidationResult(nameError, timeError, overlapError)
    }

    data class ValidationResult(
        val nameError: String? = null,
        val timeError: String? = null,
        val overlapError: String? = null
    ) {
        val isValid: Boolean get() = nameError == null && timeError == null && overlapError == null
    }
}
