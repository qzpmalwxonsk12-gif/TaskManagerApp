package com.skai.taskmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(defaultValue = "0")
    val dndDuringSession: Boolean,
    @ColumnInfo(defaultValue = "0")
    val dndPromptAnswered: Boolean,
    @ColumnInfo(defaultValue = "0")
    val autoCompleteOnTimerEnd: Boolean,
    @ColumnInfo(defaultValue = "5")
    val ext1: Int,
    @ColumnInfo(defaultValue = "10")
    val ext2: Int,
    @ColumnInfo(defaultValue = "15")
    val ext3: Int,
    val savedDndFilter: Int?,
    @ColumnInfo(defaultValue = "1")
    val taskRemindersEnabled: Boolean,
    @ColumnInfo(defaultValue = "120")
    val taskReminderSeconds: Int,
    @ColumnInfo(defaultValue = "0")
    val notePingEnabled: Boolean,
    @ColumnInfo(defaultValue = "30")
    val notePingMinutes: Int,
    @ColumnInfo(defaultValue = "'Check your notes'")
    val notePingMessage: String,
    @ColumnInfo(defaultValue = "5")
    val sessionWarningMinutes: Int,
    @ColumnInfo(defaultValue = "10")
    val snoozeMinutes: Int,
    @ColumnInfo(defaultValue = "1")
    val persistentTaskList: Boolean
)
