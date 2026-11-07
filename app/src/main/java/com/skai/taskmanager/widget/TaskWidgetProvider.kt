package com.skai.taskmanager.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.skai.taskmanager.data.repository.NoteRepository
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.data.repository.TimePeriodRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget provider. Rendering lives in [TaskWidget]; this class only reacts to
 * system callbacks (placement, resize, periodic update) and the page-switch button, fetching a
 * fresh data snapshot each time. Live updates while data changes come from the notification
 * service, which re-renders the widgets whenever its Flows emit.
 */
class TaskWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun taskRepository(): TaskRepository
        fun timePeriodRepository(): TimePeriodRepository
        fun noteRepository(): NoteRepository
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        newOptions: Bundle
    ) {
        refresh(context)
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        TaskWidget.clearPage(context, widgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TaskWidget.ACTION_SWITCH_PAGE) {
            val widgetId = intent.getIntExtra(TaskWidget.EXTRA_WIDGET_ID, -1)
            if (widgetId != -1) TaskWidget.cyclePage(context, widgetId)
            refresh(context)
        } else {
            super.onReceive(context, intent)
        }
    }

    /** Fetches a one-shot snapshot off the main thread and re-renders every widget. */
    private fun refresh(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                TaskWidget.updateAll(
                    context,
                    tasks = entryPoint.taskRepository().getAllTasksOnce(),
                    periods = entryPoint.timePeriodRepository().getAllPeriodsOnce(),
                    notes = entryPoint.noteRepository().observeAllNotes().first()
                )
            } finally {
                pending.finish()
            }
        }
    }
}
