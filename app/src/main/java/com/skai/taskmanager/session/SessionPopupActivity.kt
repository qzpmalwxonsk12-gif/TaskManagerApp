package com.skai.taskmanager.session

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.ui.theme.TaskManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Translucent, dismissible popup for the 5-minute warning and the completion prompt. Centered,
 * dims the background, never fullscreen, and auto-dismisses after a timeout. Buttons act on the
 * SessionService without opening the main app.
 */
@AndroidEntryPoint
class SessionPopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val type = intent.getStringExtra(SessionActions.EXTRA_POPUP_TYPE) ?: SessionActions.POPUP_WARNING
        val taskId = intent.getLongExtra(SessionActions.EXTRA_TASK_ID, -1L)
        val isComplete = type == SessionActions.POPUP_COMPLETE

        setContent {
            TaskManagerTheme {
                val viewModel: SessionPopupViewModel = hiltViewModel()
                val extensions by viewModel.extensions.collectAsStateWithLifecycle()

                SessionPopup(
                    isComplete = isComplete,
                    extensions = extensions,
                    autoDismissMillis = if (isComplete) 30_000L else 15_000L,
                    onExtend = { minutes ->
                        SessionService.extend(this, taskId, minutes)
                        finish()
                    },
                    onComplete = {
                        SessionService.start(this, SessionActions.ACTION_COMPLETE, taskId)
                        finish()
                    },
                    onRestart = {
                        SessionService.start(this, SessionActions.ACTION_RESTART, taskId)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        fun intent(context: Context, type: String, taskId: Long): Intent =
            Intent(context, SessionPopupActivity::class.java)
                .putExtra(SessionActions.EXTRA_POPUP_TYPE, type)
                .putExtra(SessionActions.EXTRA_TASK_ID, taskId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
}

@Composable
private fun SessionPopup(
    isComplete: Boolean,
    extensions: List<Int>,
    autoDismissMillis: Long,
    onExtend: (Int) -> Unit,
    onComplete: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    var customMode by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        delay(autoDismissMillis)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isComplete) "Session complete" else "Session almost finished",
                    style = MaterialTheme.typography.titleLarge
                )
                if (!isComplete) {
                    Text(
                        text = "5 minutes remaining — need more time?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (customMode) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { customMode = false }, modifier = Modifier.weight(1f)) {
                            Text("Back")
                        }
                        Button(
                            onClick = { customText.toIntOrNull()?.takeIf { it > 0 }?.let(onExtend) },
                            enabled = customText.toIntOrNull()?.let { it > 0 } == true,
                            modifier = Modifier.weight(1f)
                        ) { Text("Add") }
                    }
                } else {
                    // Quick extension buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        extensions.forEach { minutes ->
                            OutlinedButton(
                                onClick = { onExtend(minutes) },
                                modifier = Modifier.weight(1f)
                            ) { Text("+$minutes") }
                        }
                    }
                    TextButton(
                        onClick = { customMode = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Custom") }

                    if (isComplete) {
                        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                            Text("Complete task")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) {
                                Text("Restart")
                            }
                            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                Text("Dismiss")
                            }
                        }
                    } else {
                        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Continue")
                        }
                    }
                }
            }
        }
    }
}
