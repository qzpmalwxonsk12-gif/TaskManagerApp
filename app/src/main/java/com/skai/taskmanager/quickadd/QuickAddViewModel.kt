package com.skai.taskmanager.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.NoteRepository
import com.skai.taskmanager.data.repository.SettingsRepository
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.data.repository.TimePeriodRepository
import com.skai.taskmanager.domain.model.Note
import com.skai.taskmanager.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Backs the lightweight quick-add popup. A task with a session duration becomes a Session Task.
 */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val periodRepository: TimePeriodRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Creates a task (auto-assigned to the current period). If [durationMinutes] is set, it's a
     * Session Task. [onResult] reports `(isSessionTask, showDndPrompt)`.
     */
    fun saveTask(title: String, durationMinutes: Int?, onResult: (Boolean, Boolean) -> Unit) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            onResult(false, false)
            return
        }
        val isSession = durationMinutes != null && durationMinutes > 0
        viewModelScope.launch {
            val period = periodRepository.getCurrentPeriod(LocalTime.now())
            val settings = settingsRepository.getSettings()
            taskRepository.addTask(
                Task(
                    title = trimmed,
                    periodId = period?.id,
                    sessionDurationMinutes = if (isSession) durationMinutes else null,
                    enableDndDuringSession = settings.dndDuringSession,
                    autoCompleteOnTimerEnd = settings.autoCompleteOnTimerEnd
                )
            )
            onResult(isSession, isSession && !settings.dndPromptAnswered)
        }
    }

    /** All notes (pinned first) for the pin-notes picker opened from the ping notification. */
    val notes: StateFlow<List<Note>> = noteRepository.observeAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun togglePinned(note: Note) {
        viewModelScope.launch { noteRepository.setPinned(note.id, !note.pinned) }
    }

    fun saveNote(content: String, onDone: () -> Unit) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            onDone()
            return
        }
        viewModelScope.launch {
            noteRepository.addNote(trimmed)
            onDone()
        }
    }

    /** Records the first-time DND preference. */
    fun answerDndPrompt(enable: Boolean, neverAskAgain: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val s = settingsRepository.getSettings()
            settingsRepository.update(
                s.copy(
                    dndDuringSession = if (enable) true else s.dndDuringSession,
                    dndPromptAnswered = enable || neverAskAgain
                )
            )
            onDone()
        }
    }
}
