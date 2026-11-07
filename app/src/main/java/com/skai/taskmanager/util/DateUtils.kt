package com.skai.taskmanager.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Date helpers built on java.time (available without desugaring at minSdk 26). All "today"
 * calculations use the device's default time zone.
 */
object DateUtils {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfToday(): Long =
        LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfToday(): Long =
        LocalDate.now().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

    /** Epoch millis of the upcoming midnight (start of tomorrow) — the day-rollover boundary. */
    fun startOfTomorrow(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Start-of-day epoch millis for Monday of the current week. */
    fun startOfWeek(): Long =
        LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Start-of-day epoch millis for the first day of the current month. */
    fun startOfMonth(): Long =
        LocalDate.now().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Start-of-day epoch millis for [epochMillis]'s date — handy when persisting a picked date. */
    fun startOfDay(epochMillis: Long): Long =
        toLocalDate(epochMillis).atStartOfDay(zone).toInstant().toEpochMilli()

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun isToday(epochMillis: Long): Boolean = toLocalDate(epochMillis) == LocalDate.now()

    fun isOverdue(epochMillis: Long): Boolean = toLocalDate(epochMillis).isBefore(LocalDate.now())

    fun isFuture(epochMillis: Long): Boolean = toLocalDate(epochMillis).isAfter(LocalDate.now())

    fun formatDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().format(dateFormatter)

    fun formatDateTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dateTimeFormatter)

    /** Friendly relative label for a due date: Today / Tomorrow / Yesterday / formatted date. */
    fun formatDueDateLabel(epochMillis: Long): String {
        val date = toLocalDate(epochMillis)
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(dateFormatter)
        }
    }

    /** Day-only relative label: Today / Yesterday / formatted date. */
    fun formatDayLabel(epochMillis: Long): String {
        val date = toLocalDate(epochMillis)
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(dateFormatter)
        }
    }

    /** Friendly relative label for a completion timestamp. */
    fun formatCompletedLabel(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return "${formatDayLabel(epochMillis)} at ${time.format(timeFormatter)}"
    }

    fun daysBetween(fromMillis: Long, toMillis: Long): Long =
        abs(toLocalDate(toMillis).toEpochDay() - toLocalDate(fromMillis).toEpochDay())

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())

    /** Formats minutes-since-midnight as a localized time, e.g. 780 -> "1:00 PM". */
    fun formatMinutesOfDay(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(clamped / 60, clamped % 60).format(timeFormatter)
    }

    // --- Material3 DatePicker interop -----------------------------------------------------
    // The Compose DatePicker works in UTC midnight; we persist local start-of-day. These two
    // helpers convert between the two so a picked date never drifts by a day.

    /** Convert a UTC-midnight value (from the DatePicker) to local start-of-day millis. */
    fun utcMillisToLocalStartOfDay(utcMillis: Long): Long {
        val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("UTC")).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** Convert a stored local millis to the UTC-midnight value the DatePicker expects. */
    fun localMillisToUtcMillis(localMillis: Long): Long {
        val date = toLocalDate(localMillis)
        return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
}
