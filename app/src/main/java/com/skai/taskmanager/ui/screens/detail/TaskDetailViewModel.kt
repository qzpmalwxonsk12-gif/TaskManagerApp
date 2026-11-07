package com.skai.taskmanager.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.model.TaskAlert
import com.skai.taskmanager.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val repository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long =
        savedStateHandle.get<Long>(Screen.Detail.ARG_TASK_ID) ?: -1L

    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<TaskDetailUiState> =
        combine(repository.observeTask(taskId), deleted) { task, isDeleted ->
            TaskDetailUiState(task = task, isLoading = false, isDeleted = isDeleted)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TaskDetailUiState()
        )

    fun toggleComplete() {
        val task = uiState.value.task ?: return
        viewModelScope.launch { repository.setTaskCompleted(task, !task.isCompleted) }
    }

    fun deleteTask() {
        val task = uiState.value.task ?: return
        viewModelScope.launch {
            repository.deleteTask(task)
            deleted.value = true
        }
    }

    // ---- Session alert customization (applies immediately; no separate save step) ----------

    fun setSessionWarningAlert(alert: TaskAlert) = updateTask { it.copy(sessionWarningAlert = alert) }

    fun setSessionEndAlert(alert: TaskAlert) = updateTask { it.copy(sessionEndAlert = alert) }

    /** Custom sound picked for the warning alert — selects CUSTOM and stores the file. */
    fun setWarningCustomSound(uri: String) = updateTask {
        it.copy(sessionWarningAlert = TaskAlert.CUSTOM, alertSoundUri = uri)
    }

    /** Custom sound picked for the end alert — selects CUSTOM and stores the file. */
    fun setEndCustomSound(uri: String) = updateTask {
        it.copy(sessionEndAlert = TaskAlert.CUSTOM, alertSoundUri = uri)
    }

    private fun updateTask(transform: (Task) -> Task) {
        val task = uiState.value.task ?: return
        viewModelScope.launch { repository.updateTask(transform(task)) }
    }
}
