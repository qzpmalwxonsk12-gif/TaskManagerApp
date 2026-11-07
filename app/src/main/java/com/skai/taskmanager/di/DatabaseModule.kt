package com.skai.taskmanager.di

import android.content.Context
import androidx.room.Room
import com.skai.taskmanager.data.local.MIGRATION_1_2
import com.skai.taskmanager.data.local.MIGRATION_2_3
import com.skai.taskmanager.data.local.MIGRATION_3_4
import com.skai.taskmanager.data.local.MIGRATION_4_5
import com.skai.taskmanager.data.local.MIGRATION_5_6
import com.skai.taskmanager.data.local.MIGRATION_6_7
import com.skai.taskmanager.data.local.MIGRATION_7_8
import com.skai.taskmanager.data.local.MIGRATION_8_9
import com.skai.taskmanager.data.local.MIGRATION_9_10
import com.skai.taskmanager.data.local.MIGRATION_10_11
import com.skai.taskmanager.data.local.SEED_DEFAULTS_CALLBACK
import com.skai.taskmanager.data.local.TaskDatabase
import com.skai.taskmanager.data.local.dao.NoteDao
import com.skai.taskmanager.data.local.dao.ProfileDao
import com.skai.taskmanager.data.local.dao.SessionStatDao
import com.skai.taskmanager.data.local.dao.SettingsDao
import com.skai.taskmanager.data.local.dao.TaskDao
import com.skai.taskmanager.data.local.dao.TimePeriodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTaskDatabase(
        @ApplicationContext context: Context
    ): TaskDatabase = Room.databaseBuilder(
        context,
        TaskDatabase::class.java,
        TaskDatabase.DATABASE_NAME
    )
        .addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
            MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11
        )
        .addCallback(SEED_DEFAULTS_CALLBACK)
        // Migrations are preferred; destructive recreation is only a last-resort safety net.
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideTimePeriodDao(database: TaskDatabase): TimePeriodDao = database.timePeriodDao()

    @Provides
    @Singleton
    fun provideNoteDao(database: TaskDatabase): NoteDao = database.noteDao()

    @Provides
    @Singleton
    fun provideProfileDao(database: TaskDatabase): ProfileDao = database.profileDao()

    @Provides
    @Singleton
    fun provideSettingsDao(database: TaskDatabase): SettingsDao = database.settingsDao()

    @Provides
    @Singleton
    fun provideSessionStatDao(database: TaskDatabase): SessionStatDao = database.sessionStatDao()
}
