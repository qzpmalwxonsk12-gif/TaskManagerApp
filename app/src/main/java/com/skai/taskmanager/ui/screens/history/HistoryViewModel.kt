package com.skai.taskmanager.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** One day's worth of history: tasks completed that day plus tasks missed that day. */
data class HistoryDay(
    val key: Long,
    val label: String,
    val tasks: List<Task>
) {
    val completedCount: Int get() = tasks.count { it.isCompleted }
    val missedCount: Int get() = tasks.size - completedCount
}

data class HistoryUiState(
    val days: List<HistoryDay> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val hasAnyHistory: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> =
        combine(repository.observeHistoryTasks(), query) { tasks, q ->
            buildState(tasks, q)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )

    fun onQueryChange(value: String) = query.update { value }

    /** The day a history entry belongs to: when it was completed, or (for a missed task) the
     * day it was planned for. */
    private fun historyDayMillis(task: Task): Long =
        task.completedAt ?: task.dueDate ?: task.createdAt

    private fun buildState(tasks: List<Task>, q: String): HistoryUiState {
        val matched = if (q.isBlank()) {
            tasks
        } else {
            tasks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true)
            }
        }

        val days = matched
            .groupBy { DateUtils.toLocalDate(historyDayMillis(it)) }
            .toList()
            .sortedByDescending { (date, _) -> date.toEpochDay() }
            .map { (date, dayTasks) ->
                val sorted = dayTasks.sortedWith(
                    compareBy<Task> { !it.isCompleted }.thenByDescending { historyDayMillis(it) }
                )
                HistoryDay(
                    key = date.toEpochDay(),
                    label = DateUtils.formatDayLabel(historyDayMillis(sorted.first())),
                    tasks = sorted
                )
            }

        return HistoryUiState(
            days = days,
            query = q,
            isLoading = false,
            hasAnyHistory = tasks.isNotEmpty()
        )
    }
}
