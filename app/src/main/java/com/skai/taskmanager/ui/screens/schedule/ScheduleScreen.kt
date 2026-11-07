package com.skai.taskmanager.ui.screens.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.domain.model.TimePeriod
import com.skai.taskmanager.ui.components.EmptyState
import com.skai.taskmanager.ui.components.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAddTaskForPeriod: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openNewPeriodEditor,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Period") }
            )
        }
    ) { innerPadding ->
        if (!uiState.isLoading && uiState.periods.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Schedule,
                title = "No periods yet",
                subtitle = "Add periods to shape your day. Tasks can be assigned to a period, or left as Anytime.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.periods, key = { _, period -> period.id }) { index, period ->
                    PeriodRow(
                        period = period,
                        isFirst = index == 0,
                        isLast = index == uiState.periods.lastIndex,
                        canDelete = uiState.canDelete,
                        onToggle = { viewModel.setEnabled(period, it) },
                        onEdit = { viewModel.openEditPeriodEditor(period) },
                        onDelete = { viewModel.deletePeriod(period) },
                        onMoveUp = { viewModel.move(period, up = true) },
                        onMoveDown = { viewModel.move(period, up = false) }
                    )
                }
            }
        }
    }

    uiState.editor?.let { editor ->
        PeriodEditorSheet(
            state = editor,
            onDismiss = viewModel::closeEditor,
            onNameChange = viewModel::onNameChange,
            onStartTimeChange = viewModel::onStartTimeChange,
            onEndTimeChange = viewModel::onEndTimeChange,
            onSave = viewModel::savePeriod,
            onAddTaskForPeriod = onAddTaskForPeriod
        )
    }
}

@Composable
private fun PeriodRow(
    period: TimePeriod,
    isFirst: Boolean,
    isLast: Boolean,
    canDelete: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = period.name,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (period.isEnabled) null else TextDecoration.LineThrough,
                        color = if (period.isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = period.rangeLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = period.isEnabled, onCheckedChange = onToggle)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit period")
                }
                IconButton(onClick = onDelete, enabled = canDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete period",
                        tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private enum class TimeField { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodEditorSheet(
    state: PeriodEditorState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onStartTimeChange: (Int, Int) -> Unit,
    onEndTimeChange: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onAddTaskForPeriod: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var picker by remember { mutableStateOf<TimeField?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (state.isEditing) "Edit period" else "New period",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { message -> { Text(message) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeFieldCard(
                    label = "Start",
                    hour = state.startHour,
                    minute = state.startMinute,
                    onClick = { picker = TimeField.START },
                    modifier = Modifier.weight(1f)
                )
                TimeFieldCard(
                    label = "End",
                    hour = state.endHour,
                    minute = state.endMinute,
                    onClick = { picker = TimeField.END },
                    modifier = Modifier.weight(1f)
                )
            }
            state.timeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            state.overlapError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (state.isEditing && state.editingId != null) {
                HorizontalDivider()
                Text("Tasks assigned to this period", style = MaterialTheme.typography.titleMedium)
                if (state.assignedTasks.isEmpty()) {
                    Text(
                        "No tasks yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    state.assignedTasks.forEach { task ->
                        Text(
                            text = "•  ${task.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                TextButton(onClick = { onAddTaskForPeriod(state.editingId) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add task to this period", modifier = Modifier.padding(start = 4.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = onSave,
                    enabled = state.isValid,
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Save") }
            }
        }
    }

    when (picker) {
        TimeField.START -> TimePickerDialog(
            title = "Start time",
            initialHour = state.startHour,
            initialMinute = state.startMinute,
            onConfirm = { h, m -> onStartTimeChange(h, m); picker = null },
            onDismiss = { picker = null }
        )
        TimeField.END -> TimePickerDialog(
            title = "End time",
            initialHour = state.endHour,
            initialMinute = state.endMinute,
            onConfirm = { h, m -> onEndTimeChange(h, m); picker = null },
            onDismiss = { picker = null }
        )
        null -> Unit
    }
}

@Composable
private fun TimeFieldCard(
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.titleMedium)
        }
    }
}
