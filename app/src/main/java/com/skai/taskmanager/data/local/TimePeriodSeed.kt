package com.skai.taskmanager.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The default day periods seeded on first launch and during the v1→v2 migration, so existing
 * users keep the original Morning/Afternoon/Evening/Night behaviour with zero changes.
 *
 * Seeding is done with raw SQL (rather than the DAO) because it must run both inside Room's
 * `onCreate` callback and inside the migration, where only a [SupportSQLiteDatabase] is available.
 */
object TimePeriodSeed {

    private data class Seed(
        val name: String,
        val startHour: Int,
        val endHour: Int,
        val endMinute: Int,
        val sortOrder: Int
    )

    // Night runs to 23:59 so late-evening tasks (e.g. 22:00) always have a home; periods are
    // half-open [start, end), so anything up to 23:58 falls inside it.
    private val DEFAULTS = listOf(
        Seed("Morning", 6, 12, 0, 0),
        Seed("Afternoon", 12, 17, 0, 1),
        Seed("Evening", 17, 20, 0, 2),
        Seed("Night", 20, 23, 59, 3)
    )

    fun insertDefaults(db: SupportSQLiteDatabase) {
        DEFAULTS.forEach { seed ->
            db.execSQL(
                "INSERT INTO time_periods " +
                    "(name, startHour, startMinute, endHour, endMinute, isEnabled, sortOrder) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(seed.name, seed.startHour, 0, seed.endHour, seed.endMinute, 1, seed.sortOrder)
            )
        }
    }
}
