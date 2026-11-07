package com.skai.taskmanager.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.data.repository.TimePeriodRepository
import com.skai.taskmanager.domain.model.Category
import com.skai.taskmanager.domain.model.Priority
import com.skai.taskmanager.domain.model.RecurrenceRule
import com.skai.taskmanager.domain.model.RecurrenceType
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.model.TaskAlert
import com.skai.taskmanager.domain.model.TimePeriod
import com.skai.taskmanager.domain.scheduling.SchedulingEngine
import com.skai.taskmanager.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val taskId: Long = Screen.AddEdit.NO_TASK_ID,
    val title: String = "",
    val description: String = "",
    val category: Category = Category.DEFAULT,
    val priority: Priority = Priority.DEFAULT,
    val dueDate: Long? = null,
    val timeMinutes: Int? = null,
    val alertType: TaskAlert = TaskAlert.DEFAULT,
    val alertSoundUri: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val customIntervalDays: Int = 2,
    val periodId: Int? = null,
    val availablePeriods: List<TimePeriod> = emptyList(),
    val titleError: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
) {
    val isEditing: Boolean get() = taskId != Screen.AddEdit.NO_TASK_ID
}

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val periodRepository: TimePeriodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long =
        savedStateHandle.get<Long>(Screen.AddEdit.ARG_TASK_ID) ?: Screen.AddEdit.NO_TASK_ID

    private val initialPeriodId: Int? =
        savedStateHandle.get<Int>(Screen.AddEdit.ARG_PERIOD_ID)
            ?.takeIf { it != Screen.AddEdit.NO_PERIOD_ID }

    private val initialRecurring: Boolean =
        savedStateHandle.get<Boolean>(Screen.AddEdit.ARG_RECURRING) ?: false

    /** The task being edited, kept so we preserve fields the form doesn't expose
     * (createdAt, completion state, template link). */
    private var originalTask: Task? = null

    private val _uiState = MutableStateFlow(
        AddEditUiState(
            taskId = taskId,
            periodId = initialPeriodId,
            isRecurring = initialRecurring,
            isLoading = taskId != Screen.AddEdit.NO_TASK_ID
        )
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        // Period options for the assignment selector.
        viewModelScope.launch {
            periodRepository.observeAllPeriods().collect { periods ->
                _uiState.update { it.copy(availablePeriods = periods) }
            }
        }

        if (taskId != Screen.AddEdit.NO_TASK_ID) {
            viewModelScope.launch {
                val task = repository.getTask(taskId)
                if (task != null) {
                    originalTask = task
                    _uiState.update {
                        it.copy(
                            title = task.title,
                            description = task.description,
                            category = task.category,
                            priority = task.priority,
                            dueDate = task.dueDate,
                            timeMinutes = task.timeMinutes,
                            alertType = task.alertType,
                            alertSoundUri = task.alertSoundUri,
                            isRecurring = task.isRecurring,
                            recurrenceType = task.recurrenceRule?.type ?: RecurrenceType.DAILY,
                            customIntervalDays = task.recurrenceRule?.intervalDays ?: 2,
                            periodId = task.periodId,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, titleError = false) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onCategoryChange(category: Category) = _uiState.update { it.copy(category = category) }

    fun onPriorityChange(priority: Priority) = _uiState.update { it.copy(priority = priority) }

    fun onDueDateChange(dueDate: Long?) = _uiState.update { it.copy(dueDate = dueDate) }

    fun onPeriodChange(periodId: Int?) = _uiState.update { it.copy(periodId = periodId) }

    /** The period containing a time of day, or null (gap / no periods). */
    private fun periodForTime(minutes: Int, periods: List<TimePeriod>): Int? =
        SchedulingEngine.activePeriod(periods, LocalTime.of(minutes / 60, minutes % 60))?.id

    /**
     * Sets (or clears) the task's time of day. Giving a task a time automatically slots it into the
     * period that contains that time, so it "organizes itself" into the right part of the day.
     */
    fun onTimeChange(minutes: Int?) = _uiState.update { state ->
        state.copy(
            timeMinutes = minutes,
            periodId = if (minutes != null) periodForTime(minutes, state.availablePeriods) else state.periodId
        )
    }

    fun onAlertTypeChange(alert: TaskAlert) = _uiState.update { it.copy(alertType = alert) }

    /** Stores the user-picked custom sound; selecting one switches the alert to CUSTOM. */
    fun onCustomSoundPicked(uri: String) = _uiState.update {
        it.copy(alertType = TaskAlert.CUSTOM, alertSoundUri = uri)
    }

    fun onRecurringChange(enabled: Boolean) = _uiState.update { it.copy(isRecurring = enabled) }

    fun onRecurrenceTypeChange(type: RecurrenceType) =
        _uiState.update { it.copy(recurrenceType = type) }

    fun onCustomIntervalChange(days: Int) =
        _uiState.update { it.copy(customIntervalDays = days) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }

        val rule = if (state.isRecurring) {
            RecurrenceRule(state.recurrenceType, state.customIntervalDays.coerceAtLeast(1))
        } else {
            null
        }

        viewModelScope.launch {
            // A timed task is auto-assigned to the period that contains its time. Fetch the
            // periods directly if the UI's list hasn't arrived yet (saving very quickly used to
            // leave the task without a period — e.g. a 22:00 task landing outside "Night").
            val periods = state.availablePeriods.ifEmpty { periodRepository.getAllPeriodsOnce() }
            val resolvedPeriodId = state.timeMinutes?.let { periodForTime(it, periods) }
                ?: state.periodId
            val original = originalTask
            if (original != null) {
                repository.updateTask(
                    original.copy(
                        title = state.title.trim(),
                        description = state.description.trim(),
                        category = state.category,
                        priority = state.priority,
                        dueDate = state.dueDate,
                        timeMinutes = state.timeMinutes,
                        alertType = state.alertType,
                        alertSoundUri = state.alertSoundUri,
                        isRecurring = state.isRecurring,
                        recurrenceRule = rule,
                        templateId = if (state.isRecurring) original.templateId else null,
                        periodId = resolvedPeriodId
                    )
                )
            } else {
                repository.addTask(
                    Task(
                        title = state.title.trim(),
                        description = state.description.trim(),
                        category = state.category,
                        priority = state.priority,
                        dueDate = state.dueDate,
                        timeMinutes = state.timeMinutes,
                        alertType = state.alertType,
                        alertSoundUri = state.alertSoundUri,
                        isRecurring = state.isRecurring,
                        recurrenceRule = rule,
                        periodId = resolvedPeriodId
                    )
                )
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
