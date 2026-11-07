package com.skai.taskmanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import com.skai.taskmanager.ui.theme.AppearancePreferences
import com.skai.taskmanager.ui.theme.WidgetStyle
import com.skai.taskmanager.MainActivity
import com.skai.taskmanager.R
import com.skai.taskmanager.domain.model.Note
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.model.TimePeriod
import com.skai.taskmanager.domain.scheduling.TaskFilters
import com.skai.taskmanager.notification.NotificationActions
import com.skai.taskmanager.notification.NotificationContent
import com.skai.taskmanager.quickadd.QuickAddActivity
import com.skai.taskmanager.util.DateUtils

/**
 * Renders the home-screen widget — a compact mirror of the persistent notification. Three pages
 * (cycled by the switch button, remembered per widget):
 *  - **Tasks**: the current period's to-dos, tap a row to complete it;
 *  - **Done**: everything completed today, tap a row to un-complete it;
 *  - **Notes**: all notes, pinned first.
 * The add button is page-specific (+ Task / + Note; hidden on Done).
 */
object TaskWidget {

    const val ACTION_SWITCH_PAGE = "com.skai.taskmanager.widget.SWITCH_PAGE"
    const val EXTRA_WIDGET_ID = "com.skai.taskmanager.widget.WIDGET_ID"

    private const val PREFS = "task_widget"
    private const val PAGE_TASKS = 0
    private const val PAGE_DONE = 1
    private const val PAGE_NOTES = 2
    private const val PAGE_COUNT = 3

    fun cyclePage(context: Context, widgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = (prefs.getInt(pageKey(widgetId), PAGE_TASKS) + 1) % PAGE_COUNT
        prefs.edit().putInt(pageKey(widgetId), next).apply()
    }

    fun clearPage(context: Context, widgetIds: IntArray) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        widgetIds.forEach { editor.remove(pageKey(it)) }
        editor.apply()
    }

    /** Re-renders every placed widget from the given data snapshot. */
    fun updateAll(context: Context, tasks: List<Task>, periods: List<TimePeriod>, notes: List<Note>) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
        ids.forEach { id ->
            manager.updateAppWidget(id, build(context, manager, id, tasks, periods, notes))
        }
    }

    // ---- Rendering -------------------------------------------------------------------------

    private fun build(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        tasks: List<Task>,
        periods: List<TimePeriod>,
        notes: List<Note>
    ): RemoteViews {
        val page = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(pageKey(widgetId), PAGE_TASKS)
        return createViews(context, widgetId, page, tasks, periods, notes,
            AppearancePreferences.read(context).widget,
            manager.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 240))
    }

    /** Used by both the launcher and the live Settings preview. */
    fun createViews(
        context: Context, widgetId: Int, page: Int, tasks: List<Task>, periods: List<TimePeriod>,
        notes: List<Note>, style: WidgetStyle, heightDp: Int
    ): RemoteViews {
        val classic = style == WidgetStyle.CLASSIC
        val palette = style.palette()
        val rv = RemoteViews(context.packageName,
            if (classic) R.layout.widget_task_classic else if (style == WidgetStyle.NEON) R.layout.widget_task_neon else R.layout.widget_task_styled)
        if (!classic) {
            rv.setImageViewResource(R.id.widget_art, palette.background)
            rv.setTextViewText(R.id.widget_eyebrow, palette.caption)
            rv.setTextColor(R.id.widget_eyebrow, palette.accent)
            rv.setTextColor(R.id.widget_title, palette.ink)
            rv.setTextColor(R.id.widget_progress, palette.accent)
            rv.setTextColor(R.id.widget_empty, palette.muted)
            rv.setTextColor(R.id.widget_overflow, palette.muted)
            if (heightDp < 220) {
                rv.setViewVisibility(R.id.widget_eyebrow, View.GONE)
                rv.setTextViewTextSize(R.id.widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
            }
            if (style == WidgetStyle.PAPER && heightDp >= 260) {
                val density = context.resources.displayMetrics.density
                rv.setViewPadding(R.id.widget_content, (12 * density).toInt(), (12 * density).toInt(),
                    (12 * density).toInt(), (48 * density).toInt())
            }
            listOf(R.id.widget_switch, R.id.widget_add).forEach {
                rv.setTextColor(it, palette.ink)
                rv.setInt(it, "setBackgroundResource", palette.button)
            }
        }

        rv.setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent(context))
        rv.setOnClickPendingIntent(R.id.widget_switch, switchPagePendingIntent(context, widgetId))
        rv.removeAllViews(R.id.widget_container)

        val maxRows = widgetRowBudget(heightDp, context.resources.configuration.fontScale, style)
        when (page) {
            PAGE_DONE -> renderDonePage(context, rv, tasks, maxRows, style)
            PAGE_NOTES -> renderNotesPage(context, rv, notes, maxRows, style)
            else -> renderTasksPage(context, rv, tasks, periods, maxRows, style)
        }
        return rv
    }

    private fun renderTasksPage(
        context: Context,
        rv: RemoteViews,
        tasks: List<Task>,
        periods: List<TimePeriod>,
        maxRows: Int,
        style: WidgetStyle
    ) {
        val content = NotificationContent.build(tasks, periods)
        rv.setTextViewText(R.id.widget_title, content.title)
        rv.setTextViewText(R.id.widget_progress, content.progressText)
        rv.setTextViewText(R.id.widget_switch, "▸ Done")
        rv.setViewVisibility(R.id.widget_add, View.VISIBLE)
        rv.setTextViewText(R.id.widget_add, "+ Task")
        rv.setOnClickPendingIntent(
            R.id.widget_add,
            quickAddPendingIntent(context, QuickAddActivity.MODE_TASK, NotificationActions.REQ_ADD_TASK)
        )

        val rows = content.periodTasks.take(maxRows)
        setEmpty(rv, content.periodTasks.isEmpty(), "A little breathing room.\nNo tasks for now.")
        showOverflow(context, rv, content.periodTasks.size, maxRows, style)
        rows.forEach { task -> rv.addView(R.id.widget_container, taskRow(context, task, style)) }
    }

    private fun renderDonePage(context: Context, rv: RemoteViews, tasks: List<Task>, maxRows: Int, style: WidgetStyle) {
        val done = tasks.filter { TaskFilters.isCompletedToday(it) }.sortedByDescending { it.completedAt }
        rv.setTextViewText(R.id.widget_title, "Done today")
        rv.setTextViewText(R.id.widget_progress, "${done.size}")
        rv.setTextViewText(R.id.widget_switch, "▸ Notes")
        // Nothing sensible to add on this page.
        rv.setViewVisibility(R.id.widget_add, View.GONE)

        setEmpty(rv, done.isEmpty(), "One small step at a time.\nYour finished tasks go here.")
        showOverflow(context, rv, done.size, maxRows, style)
        done.take(maxRows).forEach { task -> rv.addView(R.id.widget_container, taskRow(context, task, style)) }
    }

    private fun renderNotesPage(context: Context, rv: RemoteViews, notes: List<Note>, maxRows: Int, style: WidgetStyle) {
        rv.setTextViewText(R.id.widget_title, "Notes")
        rv.setTextViewText(R.id.widget_progress, "${notes.size}")
        rv.setTextViewText(R.id.widget_switch, "▸ Tasks")
        rv.setViewVisibility(R.id.widget_add, View.VISIBLE)
        rv.setTextViewText(R.id.widget_add, "+ Note")
        rv.setOnClickPendingIntent(
            R.id.widget_add,
            quickAddPendingIntent(context, QuickAddActivity.MODE_NOTE, NotificationActions.REQ_ADD_NOTE)
        )

        setEmpty(rv, notes.isEmpty(), "A place for your thoughts.\nAdd your first note below.")
        showOverflow(context, rv, notes.size, maxRows, style)
        notes.take(maxRows).forEach { note ->
            val row = styledRow(context, style)
            val prefix = if (note.pinned) "📌 " else ""
            row.setTextViewText(R.id.task_row_title, "$prefix${note.content.replace('\n', ' ')}")
            row.setImageViewResource(R.id.task_row_check, R.drawable.ic_note_dot)
            row.setContentDescription(R.id.task_row_root, "Open note: ${note.content}")
            row.setOnClickPendingIntent(R.id.task_row_root, openAppPendingIntent(context))
            rv.addView(R.id.widget_container, row)
        }
    }

    /** Same row style and tap-to-toggle behaviour as the notification list. */
    private fun taskRow(context: Context, task: Task, style: WidgetStyle): RemoteViews {
        val row = styledRow(context, style)
        val prefix = task.timeMinutes?.let { "${DateUtils.formatMinutesOfDay(it)}  " } ?: ""
        val label = if (style == WidgetStyle.CLASSIC) "$prefix${task.title}" else task.title
        if (style != WidgetStyle.CLASSIC) {
            val meta = listOfNotNull(task.timeMinutes?.let { DateUtils.formatMinutesOfDay(it) },
                task.sessionDurationMinutes?.let { "${it}m focus" }).joinToString(" · ")
            row.setTextViewText(R.id.widget_row_meta, meta)
            row.setViewVisibility(R.id.widget_row_meta, if (meta.isEmpty()) View.GONE else View.VISIBLE)
            row.setTextColor(R.id.widget_row_meta, style.palette().muted)
        }
        val text = if (task.isCompleted && style != WidgetStyle.CLASSIC) SpannableString(label).apply {
            setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else label
        row.setTextViewText(R.id.task_row_title, text)
        row.setContentDescription(R.id.task_row_root,
            "${if (task.isCompleted) "Mark incomplete" else "Complete"}: ${task.title}")
        row.setImageViewResource(
            R.id.task_row_check,
            if (task.isCompleted) R.drawable.ic_task_check else R.drawable.ic_task_uncheck
        )
        val action = if (task.isCompleted) {
            NotificationActions.ACTION_UNCOMPLETE_TASK
        } else {
            NotificationActions.ACTION_COMPLETE_TASK
        }
        val intent = Intent(context, com.skai.taskmanager.notification.NotificationActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("taskmanager://task/${task.id}"))
            .putExtra(NotificationActions.EXTRA_TASK_ID, task.id)
        row.setOnClickPendingIntent(
            R.id.task_row_root,
            PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        return row
    }

    private fun setEmpty(rv: RemoteViews, empty: Boolean, message: String) {
        rv.setViewVisibility(R.id.widget_empty, if (empty) View.VISIBLE else View.GONE)
        if (empty) rv.setTextViewText(R.id.widget_empty, message)
    }

    private fun styledRow(context: Context, style: WidgetStyle): RemoteViews {
        val classic = style == WidgetStyle.CLASSIC
        return RemoteViews(context.packageName,
            if (classic) R.layout.notification_task_row else R.layout.widget_styled_row).apply {
            if (!classic) {
                val palette = style.palette()
                setTextColor(R.id.task_row_title, palette.ink)
                setInt(R.id.task_row_check, "setColorFilter", palette.accent)
                setInt(R.id.task_row_root, "setBackgroundResource", palette.row)
            }
        }
    }

    private fun showOverflow(context: Context, rv: RemoteViews, count: Int, shown: Int, style: WidgetStyle) {
        if (style == WidgetStyle.CLASSIC) return
        val remaining = (count - shown).coerceAtLeast(0)
        rv.setTextViewText(R.id.widget_overflow, if (remaining > 0) "+$remaining more · open app" else "")
        rv.setOnClickPendingIntent(R.id.widget_overflow, openAppPendingIntent(context))
    }

    // ---- PendingIntents ----------------------------------------------------------------------

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = NotificationActions.ACTION_OPEN_APP
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            NotificationActions.REQ_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun switchPagePendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, TaskWidgetProvider::class.java)
            .setAction(ACTION_SWITCH_PAGE)
            .setData(Uri.parse("taskmanager://widget/$widgetId"))
            .putExtra(EXTRA_WIDGET_ID, widgetId)
        return PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun quickAddPendingIntent(context: Context, mode: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            QuickAddActivity.intent(context, mode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun pageKey(widgetId: Int) = "page_$widgetId"
}
