package com.skai.taskmanager.quickadd

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.session.SessionService
import com.skai.taskmanager.ui.theme.TaskManagerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Translucent quick-add popup (task or note) reachable from the task notification. A task with a
 * Session Duration becomes a Session Task; the first such task prompts about Do Not Disturb.
 */
@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_TASK

        setContent {
            TaskManagerTheme {
                val viewModel: QuickAddViewModel = hiltViewModel()
                var showDndPrompt by remember { mutableStateOf(false) }

                if (showDndPrompt) {
                    DndPromptDialog(
                        onChoice = { enable, neverAsk ->
                            viewModel.answerDndPrompt(enable, neverAsk) { finish() }
                        }
                    )
                } else when (mode) {
                    MODE_NOTE -> QuickAddNoteDialog(
                        onSave = { text -> viewModel.saveNote(text) { finish() } },
                        onDismiss = { finish() }
                    )
                    MODE_PIN -> PinNotesDialog(
                        viewModel = viewModel,
                        onDismiss = { finish() }
                    )
                    else -> QuickAddTaskDialog(
                        onSave = { title, duration ->
                            viewModel.saveTask(title, duration) { isSession, needPrompt ->
                                if (isSession) SessionService.start(this)
                                if (needPrompt) showDndPrompt = true else finish()
                            }
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "quick_add_mode"
        const val MODE_TASK = "task"
        const val MODE_NOTE = "note"

        /** Pick which notes are pinned (opened from the note-ping notification). */
        const val MODE_PIN = "pin_note"

        fun intent(context: Context, mode: String): Intent =
            Intent(context, QuickAddActivity::class.java)
                .putExtra(EXTRA_MODE, mode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
}

@Composable
private fun QuickAddTaskDialog(
    onSave: (String, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("New task", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit).take(3) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Session duration (optional)") },
                    placeholder = { Text("minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(title, durationText.toIntOrNull()?.takeIf { it > 0 }) },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun QuickAddNoteDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("New note", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Write a note…") },
                    minLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(text) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save note") }
                }
            }
        }
    }
}

@Composable
private fun PinNotesDialog(
    viewModel: QuickAddViewModel,
    onDismiss: () -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Pin notes", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Pinned notes are shown inside the notes reminder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (notes.isEmpty()) {
                    Text(
                        "No notes yet — add one first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.togglePinned(note) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (note.pinned) {
                                        Icons.Filled.PushPin
                                    } else {
                                        Icons.Outlined.PushPin
                                    },
                                    contentDescription = if (note.pinned) "Unpin" else "Pin",
                                    tint = if (note.pinned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
private fun DndPromptDialog(
    onChoice: (enable: Boolean, neverAsk: Boolean) -> Unit
) {
    Dialog(onDismissRequest = { onChoice(false, false) }) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Enable Do Not Disturb during sessions?", style = MaterialTheme.typography.titleLarge)
                Text(
                    "When enabled, your phone automatically enters Do Not Disturb while a session " +
                        "is active and restores your previous state when it ends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { onChoice(true, false) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onChoice(false, false) }, modifier = Modifier.weight(1f)) {
                        Text("Not now")
                    }
                    TextButton(onClick = { onChoice(false, true) }, modifier = Modifier.weight(1f)) {
                        Text("Never ask")
                    }
                }
            }
        }
    }
}
