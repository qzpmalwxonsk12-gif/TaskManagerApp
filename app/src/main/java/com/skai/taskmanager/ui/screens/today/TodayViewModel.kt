package com.skai.taskmanager.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.scheduling.TaskFilters
import com.skai.taskmanager.domain.scheduling.TaskOrdering
import com.skai.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val activeTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    /**
     * Emits once immediately and again at each midnight, so the "Today" view rolls over even if
     * the app is left open across the day boundary. Each tick also runs the repository's day
     * rollover: yesterday's finished tasks are archived, missed tasks are filed into History, and
     * recurring tasks get a fresh instance (nothing is deleted, and nothing shows up twice).
     */
    private val dayRolloverTicker: Flow<Unit> = flow {
        while (true) {
            repository.performDayRollover()
            emit(Unit)
            val untilMidnight = DateUtils.startOfTomorrow() - System.currentTimeMillis()
            delay(untilMidnight.coerceIn(60_000L, 24L * 60 * 60 * 1000))
        }
    }

    val uiState: StateFlow<TodayUiState> =
        combine(repository.observeAllTasks(), dayRolloverTicker) { tasks, _ -> buildState(tasks) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TodayUiState()
            )

    /**
     * "Today" = anything still to do that is due today, overdue, or undated, plus everything
     * completed today.
     */
    private fun buildState(tasks: List<Task>): TodayUiState {
        val endOfToday = DateUtils.endOfToday()

        val active = TaskOrdering.order(tasks.filter { TaskFilters.isActiveToday(it, endOfToday) })

        val completedToday = tasks
            .filter { TaskFilters.isCompletedToday(it) }
            .sortedByDescending { it.completedAt }

        return TodayUiState(
            activeTasks = active,
            completedTasks = completedToday,
            completedCount = completedToday.size,
            totalCount = active.size + completedToday.size,
            isLoading = false
        )
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            repository.setTaskCompleted(task, !task.isCompleted)
        }
    }

    /** Moves today's completed tasks into History now (kept in History, not deleted). */
    fun moveCompletedToHistory() {
        viewModelScope.launch {
            repository.archiveCompletedToday()
        }
    }
}
