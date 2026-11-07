package com.skai.taskmanager.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.ui.components.AppCardShape
import com.skai.taskmanager.ui.components.appCardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var ext1 by remember { mutableStateOf("") }
    var ext2 by remember { mutableStateOf("") }
    var ext3 by remember { mutableStateOf("") }
    LaunchedEffect(settings.ext1, settings.ext2, settings.ext3) {
        ext1 = settings.ext1.toString()
        ext2 = settings.ext2.toString()
        ext3 = settings.ext3.toString()
    }

    var reminderSeconds by remember { mutableStateOf("") }
    LaunchedEffect(settings.taskReminderSeconds) {
        reminderSeconds = settings.taskReminderSeconds.toString()
    }

    var snoozeMinutes by remember { mutableStateOf("") }
    LaunchedEffect(settings.snoozeMinutes) {
        snoozeMinutes = settings.snoozeMinutes.toString()
    }

    var pingMinutes by remember { mutableStateOf("") }
    var pingMessage by remember { mutableStateOf("") }
    LaunchedEffect(settings.notePingMinutes, settings.notePingMessage) {
        pingMinutes = settings.notePingMinutes.toString()
        pingMessage = settings.notePingMessage
    }

    var warningMinutes by remember { mutableStateOf("") }
    LaunchedEffect(settings.sessionWarningMinutes) {
        warningMinutes = settings.sessionWarningMinutes.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppearanceSettings(viewModel)
            // ---- Reminders -------------------------------------------------------------
            SettingsGroup(title = "Reminders") {
                SettingSwitch(
                    title = "Task time reminders",
                    subtitle = "Pop up a notification when a task's time arrives.",
                    checked = settings.taskRemindersEnabled,
                    onCheckedChange = viewModel::setTaskRemindersEnabled
                )

                if (settings.taskRemindersEnabled) {
                    OutlinedTextField(
                        value = reminderSeconds,
                        onValueChange = { reminderSeconds = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reminder stays on screen (seconds)") },
                        supportingText = { Text("10–600 seconds; the pop-up then disappears by itself.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = snoozeMinutes,
                        onValueChange = { snoozeMinutes = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Snooze duration (minutes)") },
                        supportingText = { Text("How long the Snooze button waits before reminding again.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                SettingSwitch(
                    title = "Notes reminder",
                    subtitle = "Ping a notification on a schedule so you don't forget your notes.",
                    checked = settings.notePingEnabled,
                    onCheckedChange = viewModel::setNotePingEnabled
                )

                if (settings.notePingEnabled) {
                    OutlinedTextField(
                        value = pingMinutes,
                        onValueChange = { pingMinutes = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ping every (minutes)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = pingMessage,
                        onValueChange = { pingMessage = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reminder message") },
                        placeholder = { Text("Check your notes") },
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        viewModel.setTaskReminderSeconds(
                            reminderSeconds.toIntOrNull() ?: settings.taskReminderSeconds
                        )
                        viewModel.setSnoozeMinutes(
                            snoozeMinutes.toIntOrNull() ?: settings.snoozeMinutes
                        )
                        viewModel.setNotePing(
                            pingMinutes.toIntOrNull() ?: settings.notePingMinutes,
                            pingMessage
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save reminder settings") }
            }

            // ---- Notifications ---------------------------------------------------------
            SettingsGroup(title = "Notifications") {
                SettingSwitch(
                    title = "Persistent task list",
                    subtitle = "Keep the task-list notification pinned. Turn off to make it " +
                        "swipeable — it returns with each task reminder.",
                    checked = settings.persistentTaskList,
                    onCheckedChange = viewModel::setPersistentTaskList
                )
            }

            // ---- Focus sessions --------------------------------------------------------
            SettingsGroup(title = "Focus sessions") {
                SettingSwitch(
                    title = "Do Not Disturb during sessions",
                    subtitle = "Silence your phone while a focus session runs, then restore it.",
                    checked = settings.dndDuringSession,
                    onCheckedChange = viewModel::setDndDuringSession
                )

                // DND requires a system permission; offer a shortcut to grant it.
                val nm = context.getSystemService(NotificationManager::class.java)
                val hasDndAccess = runCatching {
                    nm?.isNotificationPolicyAccessGranted == true
                }.getOrDefault(false)
                if (settings.dndDuringSession && !hasDndAccess) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Grant Do Not Disturb access") }
                }

                SettingSwitch(
                    title = "Auto-complete on timer end",
                    subtitle = "If you don't respond when a session ends, mark the task complete.",
                    checked = settings.autoCompleteOnTimerEnd,
                    onCheckedChange = viewModel::setAutoComplete
                )

                OutlinedTextField(
                    value = warningMinutes,
                    onValueChange = { warningMinutes = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("“Almost done” warning (minutes before end)") },
                    supportingText = { Text("Each session task can choose its own warning sound.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // ---- Quick extensions ------------------------------------------------------
            SettingsGroup(title = "Quick extensions") {
                Text(
                    "Extension durations (minutes) shown in the session popups and notification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MinutesField(ext1, { ext1 = it }, Modifier.weight(1f))
                    MinutesField(ext2, { ext2 = it }, Modifier.weight(1f))
                    MinutesField(ext3, { ext3 = it }, Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        viewModel.setExtensions(
                            ext1.toIntOrNull() ?: settings.ext1,
                            ext2.toIntOrNull() ?: settings.ext2,
                            ext3.toIntOrNull() ?: settings.ext3
                        )
                        viewModel.setSessionWarningMinutes(
                            warningMinutes.toIntOrNull() ?: settings.sessionWarningMinutes
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save session settings") }
            }
        }
    }
}

/** A titled card grouping related settings, giving the screen clear visual structure. */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppCardShape,
        border = com.skai.taskmanager.ui.components.appCardBorder(), colors = appCardColors()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MinutesField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(3)) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
