package com.skai.taskmanager.data.local

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: introduces the scheduling engine.
 *  - Creates the `time_periods` table (schema must match [TimePeriodEntity] exactly).
 *  - Adds the nullable `periodId` foreign-reference column to `tasks` (null = "Anytime").
 *  - Seeds the default Morning/Afternoon/Evening/Night periods so upgrading users see no change.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `time_periods` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`startHour` INTEGER NOT NULL, " +
                "`startMinute` INTEGER NOT NULL, " +
                "`endHour` INTEGER NOT NULL, " +
                "`endMinute` INTEGER NOT NULL, " +
                "`isEnabled` INTEGER NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL)"
        )
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `periodId` INTEGER")
        TimePeriodSeed.insertDefaults(db)
    }
}

/**
 * v2 → v3: adds the standalone `notes` table (notes are no longer stored as tasks).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`content` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}

/**
 * v3 → v4: adds the optional `timeMinutes` (minutes since midnight) column to tasks so a task can
 * carry a specific time of day.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `timeMinutes` INTEGER")
    }
}

/**
 * v4 → v5: adds the single-row `profile` table.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `profile` (" +
                "`id` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`bio` TEXT NOT NULL, " +
                "`photoPath` TEXT, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

/**
 * v5 → v6: adds the `archived` flag to tasks (used by "Move to history" — a completed task is
 * hidden from Today but kept in the database/History).
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * v6 → v7: Session Tasks (Focus Mode) — adds optional session fields to tasks and the single-row
 * `settings` table (session preferences + custom extension durations).
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionDurationMinutes` INTEGER")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionState` TEXT NOT NULL DEFAULT 'IDLE'")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionStartTime` INTEGER")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionEndTime` INTEGER")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionRemainingSeconds` INTEGER")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `enableDndDuringSession` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `autoCompleteOnTimerEnd` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `notificationProfile` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `settings` (" +
                "`id` INTEGER NOT NULL, " +
                "`dndDuringSession` INTEGER NOT NULL DEFAULT 0, " +
                "`dndPromptAnswered` INTEGER NOT NULL DEFAULT 0, " +
                "`autoCompleteOnTimerEnd` INTEGER NOT NULL DEFAULT 0, " +
                "`ext1` INTEGER NOT NULL DEFAULT 5, " +
                "`ext2` INTEGER NOT NULL DEFAULT 10, " +
                "`ext3` INTEGER NOT NULL DEFAULT 15, " +
                "`savedDndFilter` INTEGER, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

/**
 * v7 → v8: Statistics — adds the per-session scheduled-seconds column (to record final focused
 * duration) and the pre-aggregated `session_stats` table.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionScheduledSeconds` INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_stats` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`activityName` TEXT NOT NULL, " +
                "`durationSeconds` INTEGER NOT NULL, " +
                "`completedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_stats_completedAt` ON `session_stats` (`completedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_stats_activityName` ON `session_stats` (`activityName`)")
    }
}

/**
 * v8 → v9: Reminders — adds the task-reminder and note-ping preference columns to `settings`.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `taskRemindersEnabled` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `taskReminderSeconds` INTEGER NOT NULL DEFAULT 120")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `notePingEnabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `notePingMinutes` INTEGER NOT NULL DEFAULT 30")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `notePingMessage` TEXT NOT NULL DEFAULT 'Check your notes'")
    }
}

/**
 * v9 → v10: Alerts & pinned notes —
 *  - per-task alert profile (and optional custom sound) for the task-time reminder;
 *  - per-task alert profiles for the focus-session warning and end;
 *  - `pinned` flag on notes (pinned notes appear inside the note-ping notification);
 *  - configurable session warning lead time in settings.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `alertType` TEXT NOT NULL DEFAULT 'CHIME'")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `alertSoundUri` TEXT")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionWarningAlertType` TEXT NOT NULL DEFAULT 'VIBRATE'")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `sessionEndAlertType` TEXT NOT NULL DEFAULT 'CHIME'")
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `sessionWarningMinutes` INTEGER NOT NULL DEFAULT 5")
    }
}

/**
 * v10 → v11: Skip/Snooze & widget-era options —
 *  - `skipped` flag on tasks ("Skip" on a reminder files the occurrence as skipped);
 *  - snooze duration and the persistent/dismissible notification choice in settings;
 *  - repairs the default "Night" period: it used to end at 22:00, which left 22:00–midnight
 *    uncovered, so a 22:00 task fell outside every period. Now ends at 23:59 (only the
 *    untouched default is updated — a customized Night period is left alone).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `skipped` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `snoozeMinutes` INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `persistentTaskList` INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
            "UPDATE `time_periods` SET endHour = 23, endMinute = 59 " +
                "WHERE name = 'Night' AND endHour = 22 AND endMinute = 0"
        )
    }
}

/**
 * Seeds the default periods on a brand-new install (fresh database created directly at the latest
 * version, where the migrations above do not run).
 */
val SEED_DEFAULTS_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        TimePeriodSeed.insertDefaults(db)
    }
}
