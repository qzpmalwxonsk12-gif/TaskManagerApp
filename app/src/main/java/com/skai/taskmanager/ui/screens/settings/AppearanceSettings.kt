package com.skai.taskmanager.ui.screens.settings

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.ui.components.AppCardShape
import com.skai.taskmanager.ui.components.appCardColors
import com.skai.taskmanager.ui.theme.*
import com.skai.taskmanager.widget.TaskWidget

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    AppearanceSettingsContent(appearance, viewModel::setAppStyle, viewModel::setWidgetStyle)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsContent(appearance: Appearance, onAppStyle: (AppStyle) -> Unit, onWidgetStyle: (WidgetStyle) -> Unit) {
    Card(shape = AppCardShape, border = com.skai.taskmanager.ui.components.appCardBorder(), colors = appCardColors()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Make it yours", style = MaterialTheme.typography.headlineMedium)
            Text("Choose your atmosphere. Your tasks stay just as they are.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("App appearance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppStyle.entries.forEach { style ->
                    OutlinedCard(onClick = { onAppStyle(style) }, modifier = Modifier.weight(1f),
                        border = BorderStroke(if (appearance.app == style) 2.dp else 1.dp,
                            if (appearance.app == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.outlinedCardColors(containerColor = if (appearance.app == style)
                            MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(style.label, style = MaterialTheme.typography.titleMedium)
                            Text(if (style == AppStyle.STUDIO) "Warm & considered" else "Familiar Material You",
                                style = MaterialTheme.typography.bodySmall)
                            RadioButton(selected = appearance.app == style, onClick = { onAppStyle(style) })
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("Widget collection", style = MaterialTheme.typography.titleMedium)
            Text("Applies to all your home-screen widgets, independently of the app style.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetStyle.entries.forEach { style ->
                    FilterChip(selected = appearance.widget == style,
                        onClick = { onWidgetStyle(style) }, label = { Text(style.label) })
                }
            }
            Text(appearance.widget.description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            WidgetPreview(appearance.widget)
            Text("Preview · sample tasks", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WidgetPreview(style: WidgetStyle) {
    val context = LocalContext.current
    val samples = remember {
        listOf(Task(id = 1, title = "A little time for yourself"),
            Task(id = 2, title = "Make something lovely"),
            Task(id = 3, title = "Water the plants", isCompleted = true, completedAt = System.currentTimeMillis()))
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        factory = { FrameLayout(it).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS } },
        update = { host ->
            host.removeAllViews()
            val views = TaskWidget.createViews(context, -1, 0, samples, emptyList(), emptyList(), style, 300)
            val child = views.apply(context, host)
            disablePreviewActions(child)
            host.addView(child, FrameLayout.LayoutParams(-1, -1))
        }
    )
}

private fun disablePreviewActions(view: View) {
    view.setOnClickListener(null)
    view.isClickable = false
    view.isFocusable = false
    if (view is ViewGroup) for (i in 0 until view.childCount) disablePreviewActions(view.getChildAt(i))
}
