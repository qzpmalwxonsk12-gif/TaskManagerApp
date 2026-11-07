package com.skai.taskmanager.data.mapper

import com.skai.taskmanager.data.local.entity.SettingsEntity
import com.skai.taskmanager.domain.model.AppSettings

fun SettingsEntity.toDomain(): AppSettings = AppSettings(
    id = id,
    dndDuringSession = dndDuringSession,
    dndPromptAnswered = dndPromptAnswered,
    autoCompleteOnTimerEnd = autoCompleteOnTimerEnd,
    ext1 = ext1,
    ext2 = ext2,
    ext3 = ext3,
    savedDndFilter = savedDndFilter,
    taskRemindersEnabled = taskRemindersEnabled,
    taskReminderSeconds = taskReminderSeconds,
    notePingEnabled = notePingEnabled,
    notePingMinutes = notePingMinutes,
    notePingMessage = notePingMessage,
    sessionWarningMinutes = sessionWarningMinutes,
    snoozeMinutes = snoozeMinutes,
    persistentTaskList = persistentTaskList
)

fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(
    id = id,
    dndDuringSession = dndDuringSession,
    dndPromptAnswered = dndPromptAnswered,
    autoCompleteOnTimerEnd = autoCompleteOnTimerEnd,
    ext1 = ext1,
    ext2 = ext2,
    ext3 = ext3,
    savedDndFilter = savedDndFilter,
    taskRemindersEnabled = taskRemindersEnabled,
    taskReminderSeconds = taskReminderSeconds,
    notePingEnabled = notePingEnabled,
    notePingMinutes = notePingMinutes,
    notePingMessage = notePingMessage,
    sessionWarningMinutes = sessionWarningMinutes,
    snoozeMinutes = snoozeMinutes,
    persistentTaskList = persistentTaskList
)
