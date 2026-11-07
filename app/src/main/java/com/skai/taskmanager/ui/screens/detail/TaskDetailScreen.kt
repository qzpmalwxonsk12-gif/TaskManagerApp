package com.skai.taskmanager.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.ui.components.AlertPicker
import com.skai.taskmanager.ui.components.CategoryChip
import com.skai.taskmanager.ui.components.PriorityChip
import com.skai.taskmanager.ui.components.RecurrenceChip
import com.skai.taskmanager.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit,
    onEditTask: (Long) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.task?.let { task ->
                        IconButton(onClick = { onEditTask(task.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit task")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete task")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val task = uiState.task
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            task == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "This task no longer exists.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                TaskDetailContent(
                    task = task,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete task?") },
            text = { Text("This task will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTask()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskDetailContent(
    task: Task,
    viewModel: TaskDetailViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(task.category)
            PriorityChip(task.priority)
            if (task.isRecurring && task.recurrenceRule != null) {
                RecurrenceChip(task.recurrenceRule.describe())
            }
        }

        CompleteToggleButton(isCompleted = task.isCompleted, onClick = viewModel::toggleComplete)

        HorizontalDivider()

        if (task.description.isNotBlank()) {
            DetailSection(label = "Description") {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        InfoRow(
            label = "Time",
            value = task.timeMinutes?.let { DateUtils.formatMinutesOfDay(it) } ?: "Any time"
        )
        if (task.timeMinutes != null) {
            InfoRow(label = "Reminder alert", value = task.alertType.displayName)
        }
        InfoRow(label = "Due date", value = task.dueDate?.let { DateUtils.formatDate(it) } ?: "None")
        InfoRow(
            label = "Recurring",
            value = if (task.isRecurring && task.recurrenceRule != null) {
                task.recurrenceRule.describe()
            } else {
                "No"
            }
        )
        InfoRow(label = "Created", value = DateUtils.formatDateTime(task.createdAt))
        task.completedAt?.let {
            InfoRow(label = "Completed", value = DateUtils.formatDateTime(it))
        }

        if (task.isSessionTask) {
            HorizontalDivider()
            DetailSection(label = "Session alerts") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Almost-done warning",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AlertPicker(
                        selected = task.sessionWarningAlert,
                        customSoundUri = task.alertSoundUri,
                        onAlertChange = viewModel::setSessionWarningAlert,
                        onCustomSoundPicked = viewModel::setWarningCustomSound
                    )
                    Text(
                        text = "When the timer ends",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AlertPicker(
                        selected = task.sessionEndAlert,
                        customSoundUri = task.alertSoundUri,
                        onAlertChange = viewModel::setSessionEndAlert,
                        onCustomSoundPicked = viewModel::setEndCustomSound
                    )
                }
            }
        }
    }
}

@Composable
private fun CompleteToggleButton(
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    if (isCompleted) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text("Completed — tap to undo", modifier = Modifier.padding(start = 8.dp))
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors()
        ) {
            Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null)
            Text("Mark as complete", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun DetailSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
