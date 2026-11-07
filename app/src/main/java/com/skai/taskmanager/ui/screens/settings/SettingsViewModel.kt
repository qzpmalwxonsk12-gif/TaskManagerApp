package com.skai.taskmanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.SettingsRepository
import com.skai.taskmanager.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val taskBoard: com.skai.taskmanager.notification.TaskBoard
) : ViewModel() {

    val appearance = com.skai.taskmanager.ui.theme.AppearancePreferences.state

    fun setAppStyle(style: com.skai.taskmanager.ui.theme.AppStyle) =
        com.skai.taskmanager.ui.theme.AppearancePreferences.setApp(context, style)

    fun setWidgetStyle(style: com.skai.taskmanager.ui.theme.WidgetStyle) {
        com.skai.taskmanager.ui.theme.AppearancePreferences.setWidget(context, style)
        viewModelScope.launch { taskBoard.refreshWidgetAppearance() }
    }

    val settings: StateFlow<AppSettings> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.DEFAULT)

    fun setDndDuringSession(enabled: Boolean) = update { it.copy(dndDuringSession = enabled) }

    fun setAutoComplete(enabled: Boolean) = update { it.copy(autoCompleteOnTimerEnd = enabled) }

    fun setExtensions(ext1: Int, ext2: Int, ext3: Int) = update {
        it.copy(
            ext1 = ext1.coerceIn(1, 999),
            ext2 = ext2.coerceIn(1, 999),
            ext3 = ext3.coerceIn(1, 999)
        )
    }

    fun setSessionWarningMinutes(minutes: Int) = update {
        it.copy(sessionWarningMinutes = minutes.coerceIn(1, 60))
    }

    fun setTaskRemindersEnabled(enabled: Boolean) = update { it.copy(taskRemindersEnabled = enabled) }

    fun setTaskReminderSeconds(seconds: Int) = update {
        it.copy(taskReminderSeconds = seconds.coerceIn(10, 600))
    }

    fun setNotePingEnabled(enabled: Boolean) = update { it.copy(notePingEnabled = enabled) }

    fun setSnoozeMinutes(minutes: Int) = update {
        it.copy(snoozeMinutes = minutes.coerceIn(1, 720))
    }

    fun setPersistentTaskList(enabled: Boolean) = update { it.copy(persistentTaskList = enabled) }

    fun setNotePing(intervalMinutes: Int, message: String) = update {
        it.copy(
            notePingMinutes = intervalMinutes.coerceIn(1, 24 * 60),
            notePingMessage = message.trim().ifBlank { AppSettings.DEFAULT_NOTE_PING_MESSAGE }
        )
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repository.update(transform(repository.getSettings()))
        }
    }
}
