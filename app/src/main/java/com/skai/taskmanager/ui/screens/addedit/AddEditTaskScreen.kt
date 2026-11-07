package com.skai.taskmanager.ui.screens.addedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.domain.model.Category
import com.skai.taskmanager.domain.model.Priority
import com.skai.taskmanager.domain.model.RecurrenceType
import com.skai.taskmanager.ui.components.AlertPicker
import com.skai.taskmanager.ui.components.TimePickerDialog
import com.skai.taskmanager.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Task" else "Add Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text("Save", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                isError = uiState.titleError,
                supportingText = if (uiState.titleError) {
                    { Text("Title is required") }
                } else {
                    null
                }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") },
                minLines = 3
            )

            FieldLabel("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.category == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }

            FieldLabel("Priority")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { priority ->
                    FilterChip(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.onPriorityChange(priority) },
                        label = { Text(priority.displayName) }
                    )
                }
            }

            FieldLabel("Time")
            TaskTimeField(
                timeMinutes = uiState.timeMinutes,
                onPick = { showTimePicker = true },
                onClear = { viewModel.onTimeChange(null) }
            )

            if (uiState.timeMinutes != null) {
                FieldLabel("Reminder alert")
                AlertPicker(
                    selected = uiState.alertType,
                    customSoundUri = uiState.alertSoundUri,
                    onAlertChange = viewModel::onAlertTypeChange,
                    onCustomSoundPicked = viewModel::onCustomSoundPicked
                )
            }

            FieldLabel("Period")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.periodId == null,
                    onClick = { viewModel.onPeriodChange(null) },
                    label = { Text("Anytime") }
                )
                uiState.availablePeriods.forEach { period ->
                    FilterChip(
                        selected = uiState.periodId == period.id,
                        onClick = { viewModel.onPeriodChange(period.id) },
                        label = { Text(period.name) }
                    )
                }
            }

            FieldLabel("Due date")
            DueDateField(
                dueDate = uiState.dueDate,
                onDateSelected = viewModel::onDueDateChange
            )

            RecurrenceSection(
                isRecurring = uiState.isRecurring,
                recurrenceType = uiState.recurrenceType,
                customIntervalDays = uiState.customIntervalDays,
                onRecurringChange = viewModel::onRecurringChange,
                onRecurrenceTypeChange = viewModel::onRecurrenceTypeChange,
                onCustomIntervalChange = viewModel::onCustomIntervalChange
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isEditing) "Save changes" else "Add task")
            }
        }
    }

    if (showTimePicker) {
        val initial = uiState.timeMinutes ?: 9 * 60
        TimePickerDialog(
            title = "Task time",
            initialHour = initial / 60,
            initialMinute = initial % 60,
            onConfirm = { hour, minute ->
                viewModel.onTimeChange(hour * 60 + minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun TaskTimeField(
    timeMinutes: Int?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = timeMinutes?.let { DateUtils.formatMinutesOfDay(it) } ?: "No time",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (timeMinutes != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (timeMinutes != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear time")
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDateField(
    dueDate: Long?,
    onDateSelected: (Long?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = dueDate?.let { DateUtils.formatDate(it) } ?: "No due date",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dueDate != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (dueDate != null) {
                IconButton(onClick = { onDateSelected(null) }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear due date")
                }
            }
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let { DateUtils.localMillisToUtcMillis(it) }
                ?: DateUtils.localMillisToUtcMillis(System.currentTimeMillis())
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = datePickerState.selectedDateMillis
                    onDateSelected(picked?.let { DateUtils.utcMillisToLocalStartOfDay(it) })
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurrenceSection(
    isRecurring: Boolean,
    recurrenceType: RecurrenceType,
    customIntervalDays: Int,
    onRecurringChange: (Boolean) -> Unit,
    onRecurrenceTypeChange: (RecurrenceType) -> Unit,
    onCustomIntervalChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                FieldLabel("Repeat")
                Text(
                    text = "Make this a recurring task",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isRecurring, onCheckedChange = onRecurringChange)
        }

        if (isRecurring) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceType.entries.forEach { type ->
                    FilterChip(
                        selected = recurrenceType == type,
                        onClick = { onRecurrenceTypeChange(type) },
                        label = { Text(type.displayName) }
                    )
                }
            }

            if (recurrenceType == RecurrenceType.CUSTOM) {
                OutlinedTextField(
                    value = if (customIntervalDays > 0) customIntervalDays.toString() else "",
                    onValueChange = { input ->
                        val parsed = input.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
                        onCustomIntervalChange(parsed)
                    },
                    label = { Text("Repeat every (days)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
