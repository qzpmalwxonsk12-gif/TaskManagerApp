package com.skai.taskmanager.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import com.skai.taskmanager.ui.theme.LocalStudioStyle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.ui.components.EmptyState
import com.skai.taskmanager.ui.components.ProgressCard
import com.skai.taskmanager.ui.components.SectionHeader
import com.skai.taskmanager.ui.components.SessionTaskRow
import com.skai.taskmanager.ui.components.TaskListItem
import com.skai.taskmanager.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val studio = LocalStudioStyle.current

    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (studio) "Your daily space" else greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (studio) "$greeting · ${DateUtils.formatDate(System.currentTimeMillis())}" else DateUtils.formatDate(System.currentTimeMillis()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (studio) {
                ExtendedFloatingActionButton(onClick = onAddTask,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }, text = { Text("New task") })
            } else {
                FloatingActionButton(onClick = onAddTask) {
                    Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            }
        }
    ) { innerPadding ->
        if (!studio && !uiState.isLoading && uiState.totalCount == 0) {
            EmptyState(
                icon = Icons.Outlined.WbSunny,
                title = "Nothing for today",
                subtitle = "Tap + to add your first task and start a productive day.",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(if (studio) 12.dp else 8.dp)
        ) {
            item {
                ProgressCard(
                    completed = uiState.completedCount,
                    total = uiState.totalCount,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item { SectionHeader(text = "To do") }

            if (uiState.activeTasks.isEmpty()) {
                item {
                    Text(
                        text = if (uiState.totalCount == 0) "A fresh page. Add your first task." else "All done for today 🎉",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            } else {
                items(uiState.activeTasks, key = { it.id }) { task ->
                    if (task.isSessionTask) {
                        SessionTaskRow(
                            task = task,
                            onToggleComplete = { viewModel.toggleComplete(task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    } else {
                        TaskListItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleComplete(task) },
                            onClick = { onTaskClick(task.id) }
                        )
                    }
                }
            }

            if (uiState.completedTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(text = "Completed", modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::moveCompletedToHistory) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Move to history", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                items(uiState.completedTasks, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleComplete(task) },
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
}
