package com.skai.taskmanager.ui.screens.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimetableUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * The Timetable shows the user's recurring tasks (their routine), ordered by time of day. Tasks
 * added from here are created as recurring, so they repeat automatically.
 */
@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    val uiState: StateFlow<TimetableUiState> = repository.observeAllTasks()
        .map { tasks ->
            val timetable = tasks
                .filter { it.isRecurring && !it.isCompleted && !it.archived }
                .distinctBy { it.templateId ?: it.id }
                .sortedWith(
                    compareBy({ it.timeMinutes ?: Int.MAX_VALUE }, { it.title.lowercase() })
                )
            TimetableUiState(tasks = timetable, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimetableUiState())

    fun toggleComplete(task: Task) {
        viewModelScope.launch { repository.setTaskCompleted(task, !task.isCompleted) }
    }
}
