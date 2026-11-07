package com.skai.taskmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.skai.taskmanager.data.local.dao.NoteDao
import com.skai.taskmanager.data.local.dao.ProfileDao
import com.skai.taskmanager.data.local.dao.SessionStatDao
import com.skai.taskmanager.data.local.dao.SettingsDao
import com.skai.taskmanager.data.local.dao.TaskDao
import com.skai.taskmanager.data.local.dao.TimePeriodDao
import com.skai.taskmanager.data.local.entity.NoteEntity
import com.skai.taskmanager.data.local.entity.ProfileEntity
import com.skai.taskmanager.data.local.entity.SessionStatEntity
import com.skai.taskmanager.data.local.entity.SettingsEntity
import com.skai.taskmanager.data.local.entity.TaskEntity
import com.skai.taskmanager.data.local.entity.TaskTemplateEntity
import com.skai.taskmanager.data.local.entity.TimePeriodEntity

@Database(
    entities = [
        TaskEntity::class,
        TaskTemplateEntity::class,
        TimePeriodEntity::class,
        NoteEntity::class,
        ProfileEntity::class,
        SettingsEntity::class,
        SessionStatEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun timePeriodDao(): TimePeriodDao
    abstract fun noteDao(): NoteDao
    abstract fun profileDao(): ProfileDao
    abstract fun settingsDao(): SettingsDao
    abstract fun sessionStatDao(): SessionStatDao

    companion object {
        const val DATABASE_NAME = "task_manager.db"
    }
}
