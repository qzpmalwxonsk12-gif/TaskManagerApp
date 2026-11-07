package com.skai.taskmanager.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.skai.taskmanager.domain.model.TaskAlert

/**
 * Chip row for choosing a [TaskAlert] profile. Selecting "Custom sound" opens the system audio
 * picker (read permission is persisted so the sound keeps working after reboots).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertPicker(
    selected: TaskAlert,
    customSoundUri: String?,
    onAlertChange: (TaskAlert) -> Unit,
    onCustomSoundPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onCustomSoundPicked(uri.toString())
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskAlert.entries.forEach { alert ->
                FilterChip(
                    selected = selected == alert,
                    onClick = {
                        if (alert == TaskAlert.CUSTOM && customSoundUri == null) {
                            // No sound picked yet — choose one first; the pick selects CUSTOM.
                            soundPicker.launch(arrayOf("audio/*"))
                        } else {
                            onAlertChange(alert)
                        }
                    },
                    label = { Text(alert.displayName) }
                )
            }
        }
        Text(
            text = selected.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (selected == TaskAlert.CUSTOM) {
            TextButton(onClick = { soundPicker.launch(arrayOf("audio/*")) }) {
                Icon(Icons.Outlined.MusicNote, contentDescription = null)
                Text(
                    text = if (customSoundUri != null) "Change sound" else "Choose a sound",
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
