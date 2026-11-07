package com.skai.taskmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.skai.taskmanager.domain.model.SessionState
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.session.SessionActions
import com.skai.taskmanager.session.SessionService
import kotlinx.coroutines.delay

/**
 * A Today row for a Session Task: shows a ⏱ timer indicator and a prominent state-aware action
 * (Start / Pause / Resume / Done) that drives the SessionService directly.
 */
@Composable
fun SessionTaskRow(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(task.id, task.sessionState, task.sessionEndTime) {
        if (task.sessionState == SessionState.RUNNING) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppCardShape,
        border = com.skai.taskmanager.ui.components.appCardBorder(), colors = appCardColors()
    ) {
        Row(
            modifier = Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle
                    else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Toggle complete",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = sessionLabel(task, now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            SessionActionButton(
                state = task.sessionState,
                onAction = { action -> SessionService.start(context, action, task.id) }
            )
        }
    }
}

@Composable
private fun SessionActionButton(state: SessionState, onAction: (String) -> Unit) {
    when (state) {
        SessionState.IDLE ->
            Button(onClick = { onAction(SessionActions.ACTION_START) }) { Text("Start") }
        SessionState.RUNNING ->
            OutlinedButton(onClick = { onAction(SessionActions.ACTION_PAUSE) }) { Text("Pause") }
        SessionState.PAUSED ->
            Button(onClick = { onAction(SessionActions.ACTION_RESUME) }) { Text("Resume") }
        SessionState.FINISHED ->
            Button(onClick = { onAction(SessionActions.ACTION_COMPLETE) }) { Text("Done") }
    }
}

private fun sessionLabel(task: Task, now: Long): String = when (task.sessionState) {
    SessionState.IDLE -> "⏱ ${task.sessionDurationMinutes} min focus"
    SessionState.RUNNING -> {
        val rem = (((task.sessionEndTime ?: now) - now) / 1000).coerceAtLeast(0)
        "${formatSeconds(rem)} left"
    }
    SessionState.PAUSED -> "Paused • ${formatSeconds((task.sessionRemainingSeconds ?: 0).toLong())}"
    SessionState.FINISHED -> "Time's up"
}

private fun formatSeconds(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%d:%02d", m, sec)
}
