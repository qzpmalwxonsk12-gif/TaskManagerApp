package com.skai.taskmanager.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.data.repository.TimePeriodRepository
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.model.TimePeriod
import com.skai.taskmanager.domain.scheduling.SchedulingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeriodEditorState(
    val editingId: Int? = null,
    val name: String = "",
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 10,
    val endMinute: Int = 0,
    val nameError: String? = null,
    val timeError: String? = null,
    val overlapError: String? = null,
    val assignedTasks: List<Task> = emptyList()
) {
    val isEditing: Boolean get() = editingId != null
    val isValid: Boolean get() = nameError == null && timeError == null && overlapError == null
    val startMinutes: Int get() = startHour * 60 + startMinute
    val endMinutes: Int get() = endHour * 60 + endMinute
}

data class ScheduleUiState(
    val periods: List<TimePeriod> = emptyList(),
    val isLoading: Boolean = true,
    val editor: PeriodEditorState? = null
) {
    val canDelete: Boolean get() = periods.size > 1
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val periodRepository: TimePeriodRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val editor = MutableStateFlow<PeriodEditorState?>(null)

    /** Always-current snapshot used for validation regardless of UI subscription. */
    private var periodsSnapshot: List<TimePeriod> = emptyList()

    /** Collects tasks for the period currently open in the editor. */
    private var assignedTasksJob: Job? = null

    init {
        viewModelScope.launch {
            periodRepository.observeAllPeriods().collect { periodsSnapshot = it }
        }
    }

    val uiState: StateFlow<ScheduleUiState> =
        combine(periodRepository.observeAllPeriods(), editor) { periods, editorState ->
            ScheduleUiState(periods = periods, isLoading = false, editor = editorState)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScheduleUiState()
        )

    // ---- List actions --------------------------------------------------------------------

    fun setEnabled(period: TimePeriod, enabled: Boolean) {
        viewModelScope.launch { periodRepository.setEnabled(period, enabled) }
    }

    fun deletePeriod(period: TimePeriod) {
        if (periodsSnapshot.size <= 1) return
        viewModelScope.launch {
            // Detach tasks so they become "Anytime" instead of dangling on a deleted period.
            taskRepository.clearPeriodAssignment(period.id)
            periodRepository.deletePeriod(period)
        }
    }

    /** Moves [period] one slot up or down and persists the new sortOrder for all periods. */
    fun move(period: TimePeriod, up: Boolean) {
        val ordered = periodsSnapshot.sortedBy { it.sortOrder }.toMutableList()
        val index = ordered.indexOfFirst { it.id == period.id }
        if (index < 0) return
        val target = if (up) index - 1 else index + 1
        if (target !in ordered.indices) return
        ordered.add(target, ordered.removeAt(index))
        viewModelScope.launch { periodRepository.reorder(ordered.map { it.id }) }
    }

    // ---- Editor --------------------------------------------------------------------------

    fun openNewPeriodEditor() {
        assignedTasksJob?.cancel()
        val lastEnd = periodsSnapshot.maxOfOrNull { it.endMinutes } ?: (9 * 60)
        val startM = lastEnd.coerceIn(0, 23 * 60)
        val endM = (startM + 60).coerceIn(0, 23 * 60 + 59)
        editor.value = PeriodEditorState(
            editingId = null,
            name = "",
            startHour = startM / 60,
            startMinute = startM % 60,
            endHour = endM / 60,
            endMinute = endM % 60
        )
    }

    fun openEditPeriodEditor(period: TimePeriod) {
        assignedTasksJob?.cancel()
        editor.value = PeriodEditorState(
            editingId = period.id,
            name = period.name,
            startHour = period.startHour,
            startMinute = period.startMinute,
            endHour = period.endHour,
            endMinute = period.endMinute
        )
        assignedTasksJob = viewModelScope.launch {
            taskRepository.observeTasksForPeriod(period.id).collect { tasks ->
                editor.update { it?.copy(assignedTasks = tasks) }
            }
        }
        revalidate()
    }

    fun closeEditor() {
        assignedTasksJob?.cancel()
        editor.value = null
    }

    fun onNameChange(name: String) {
        editor.update { it?.copy(name = name) }
        revalidate()
    }

    fun onStartTimeChange(hour: Int, minute: Int) {
        editor.update { it?.copy(startHour = hour, startMinute = minute) }
        revalidate()
    }

    fun onEndTimeChange(hour: Int, minute: Int) {
        editor.update { it?.copy(endHour = hour, endMinute = minute) }
        revalidate()
    }

    private fun revalidate() {
        val current = editor.value ?: return
        val result = SchedulingEngine.validate(
            name = current.name,
            startMinutes = current.startMinutes,
            endMinutes = current.endMinutes,
            existingPeriods = periodsSnapshot,
            editingId = current.editingId
        )
        editor.value = current.copy(
            nameError = result.nameError,
            timeError = result.timeError,
            overlapError = result.overlapError
        )
    }

    fun savePeriod() {
        revalidate()
        val current = editor.value ?: return
        if (!current.isValid) return

        viewModelScope.launch {
            val existing = current.editingId?.let { id -> periodsSnapshot.firstOrNull { it.id == id } }
            val period = if (existing != null) {
                existing.copy(
                    name = current.name.trim(),
                    startHour = current.startHour,
                    startMinute = current.startMinute,
                    endHour = current.endHour,
                    endMinute = current.endMinute
                )
            } else {
                TimePeriod(
                    id = 0,
                    name = current.name.trim(),
                    startHour = current.startHour,
                    startMinute = current.startMinute,
                    endHour = current.endHour,
                    endMinute = current.endMinute,
                    isEnabled = true,
                    sortOrder = periodRepository.nextSortOrder()
                )
            }
            periodRepository.savePeriod(period)
            closeEditor()
        }
    }
}
