package com.skai.taskmanager.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.skai.taskmanager.MainActivity
import com.skai.taskmanager.R
import com.skai.taskmanager.data.repository.NoteRepository
import com.skai.taskmanager.data.repository.SettingsRepository
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.data.repository.TimePeriodRepository
import com.skai.taskmanager.domain.model.AppSettings
import com.skai.taskmanager.domain.model.Note
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.domain.model.TimePeriod
import com.skai.taskmanager.domain.scheduling.SchedulingEngine
import com.skai.taskmanager.domain.scheduling.TaskFilters
import com.skai.taskmanager.quickadd.QuickAddActivity
import com.skai.taskmanager.util.DateUtils
import com.skai.taskmanager.widget.TaskWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The task "command center" — owns the task-list notification, the home-screen widgets, and all
 * of the app's alarms (task reminders, note pings, period transitions, midnight rollover,
 * keep-alive). **No foreground service**: everything here is event-driven —
 *
 *  - alarms and notification buttons arrive at [NotificationActionReceiver], which calls the
 *    handlers below and lets the process go back to sleep;
 *  - while the process is alive (app open, or cached after any event), [startObserving]'s Room
 *    Flow collectors re-render instantly on every data change;
 *  - notifications themselves persist independently of the process, so nothing is lost when
 *    Android reclaims memory — the next alarm/action/boot re-renders everything.
 *
 * The only foreground service left in the app is the focus-session timer
 * ([com.skai.taskmanager.session.SessionService]), which performs genuinely continuous,
 * user-visible work while a session runs.
 */
@Singleton
class TaskBoard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val periodRepository: TimePeriodRepository,
    private val settingsRepository: SettingsRepository,
    private val noteRepository: NoteRepository,
    private val alertPlayer: AlertPlayer
) {

    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)
    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val observing = AtomicBoolean(false)

    /** Until when the period-change summary replaces the task list (epoch millis). */
    @Volatile
    private var transitionUntil = 0L

    private data class Snapshot(
        val tasks: List<Task>,
        val periods: List<TimePeriod>,
        val settings: AppSettings,
        val notes: List<Note>
    )

    // ---- Lifecycle ---------------------------------------------------------------------------

    /**
     * Starts the process-lifetime Flow observers (idempotent) and performs an initial refresh.
     * Called from Application.onCreate, so it also runs whenever any receiver wakes the process.
     */
    fun startObserving() {
        if (!observing.compareAndSet(false, true)) return
        createChannels()
        scope.launch { refresh() }
        scope.launch {
            combine(
                taskRepository.observeAllTasks(),
                periodRepository.observeAllPeriods(),
                settingsRepository.observeSettings(),
                noteRepository.observeAllNotes()
            ) { tasks, periods, settings, notes -> Snapshot(tasks, periods, settings, notes) }
                .collectLatest { snapshot ->
                    render(snapshot)
                    scheduleAlarms(snapshot)
                }
        }
    }

    /**
     * Full refresh from a one-shot database snapshot: runs the (idempotent) day rollover,
     * re-renders the notification and widgets, and re-arms every alarm. Safe to call from any
     * event — this is how the board recovers after the process was killed or the device rebooted.
     */
    suspend fun refresh(forcePost: Boolean = false, rearmNotePing: Boolean = false) {
        createChannels()
        taskRepository.performDayRollover()
        val snapshot = snapshot()
        render(snapshot, forcePost)
        scheduleAlarms(snapshot, rearmNotePing)
    }

    private suspend fun snapshot() = Snapshot(
        tasks = taskRepository.getAllTasksOnce(),
        periods = periodRepository.getAllPeriodsOnce(),
        settings = settingsRepository.getSettings(),
        notes = noteRepository.observeAllNotes().first()
    )

    /** Appearance-only refresh: leaves task state, notifications, and alarm schedules alone. */
    suspend fun refreshWidgetAppearance() {
        val current = snapshot()
        TaskWidget.updateAll(context, current.tasks, current.periods, current.notes)
    }

    // ---- Rendering ---------------------------------------------------------------------------

    /**
     * Posts the task-list notification and updates the widgets. In dismissible mode a cleared
     * notification stays cleared on ordinary changes — it only returns when [forcePost]ed
     * (alongside a task reminder) or while it is still on screen.
     */
    private fun render(snapshot: Snapshot, forcePost: Boolean = false) {
        val persistent = snapshot.settings.persistentTaskList
        if (persistent || forcePost || isMainNotificationVisible()) {
            val content = NotificationContent.build(snapshot.tasks, snapshot.periods)
            notificationManager.notify(
                NotificationActions.NOTIFICATION_ID,
                buildNotification(content, persistent)
            )
        }
        TaskWidget.updateAll(context, snapshot.tasks, snapshot.periods, snapshot.notes)
    }

    private fun isMainNotificationVisible(): Boolean = runCatching {
        notificationManager.activeNotifications.any { it.id == NotificationActions.NOTIFICATION_ID }
    }.getOrDefault(false)

    // ---- Alarm scheduling ----------------------------------------------------------------

    private fun scheduleAlarms(snapshot: Snapshot, rearmNotePing: Boolean = false) {
        scheduleNextPeriodTransition(snapshot.periods)
        scheduleNextTaskReminder(snapshot.tasks, snapshot.settings)
        scheduleNotePing(snapshot.settings, rearmNotePing)
        scheduleDayRollover()
        scheduleKeepAlive()
    }

    /** Next enabled period boundary; the same PendingIntent slot replaces the previous alarm. */
    private fun scheduleNextPeriodTransition(periods: List<TimePeriod>) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            SchedulingEngine.nextBoundaryMillis(periods, System.currentTimeMillis()),
            broadcastPendingIntent(
                NotificationActions.ACTION_PERIOD_TRANSITION,
                NotificationActions.REQ_PERIOD_TRANSITION
            )
        )
    }

    /** Arms one alarm for the earliest timed, unfinished task later today. */
    private fun scheduleNextTaskReminder(tasks: List<Task>, settings: AppSettings) {
        val pending = taskReminderPendingIntent(0)
        if (!settings.taskRemindersEnabled) {
            alarmManager.cancel(pending)
            return
        }
        val now = LocalTime.now()
        val nowMinute = now.hour * 60 + now.minute
        val endOfToday = DateUtils.endOfToday()
        val nextMinute = tasks
            .filter { TaskFilters.isActiveToday(it, endOfToday) }
            .mapNotNull { it.timeMinutes }
            .filter { it > nowMinute }
            .minOrNull()
        if (nextMinute == null) {
            alarmManager.cancel(pending)
            return
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            DateUtils.startOfToday() + nextMinute * 60_000L,
            taskReminderPendingIntent(nextMinute)
        )
    }

    /**
     * Arms (or cancels) the periodic note ping. The armed configuration is remembered in
     * SharedPreferences so unrelated refreshes never reset the countdown; [rearm] forces a new
     * countdown (used after boot, when all alarms are gone).
     */
    private fun scheduleNotePing(settings: AppSettings, rearm: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val config = if (settings.notePingEnabled) "on:${settings.notePingMinutes}" else "off"
        if (!rearm && prefs.getString(KEY_NOTE_PING_CONFIG, null) == config) return
        prefs.edit().putString(KEY_NOTE_PING_CONFIG, config).apply()

        val pending = broadcastPendingIntent(
            NotificationActions.ACTION_NOTE_PING,
            NotificationActions.REQ_NOTE_PING
        )
        if (!settings.notePingEnabled) {
            alarmManager.cancel(pending)
            notificationManager.cancel(NotificationActions.NOTE_PING_NOTIFICATION_ID)
            return
        }
        armNotePingAlarm(settings.notePingMinutes)
    }

    private fun armNotePingAlarm(intervalMinutes: Int) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMinutes.coerceAtLeast(1) * 60_000L,
            broadcastPendingIntent(
                NotificationActions.ACTION_NOTE_PING,
                NotificationActions.REQ_NOTE_PING
            )
        )
    }

    /** A second past midnight, so "today" has definitely changed when it fires. */
    private fun scheduleDayRollover() {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            DateUtils.startOfTomorrow() + 1_000L,
            broadcastPendingIntent(
                NotificationActions.ACTION_DAY_ROLLOVER,
                NotificationActions.REQ_DAY_ROLLOVER
            )
        )
    }

    /** Low-frequency safety net: refreshes the board (and rollover) even if nothing else fires. */
    private fun scheduleKeepAlive() {
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + NotificationActions.KEEPALIVE_INTERVAL_MS,
            NotificationActions.KEEPALIVE_INTERVAL_MS,
            broadcastPendingIntent(
                NotificationActions.ACTION_REVIVE,
                NotificationActions.REQ_KEEPALIVE
            )
        )
    }

    // ---- Event handlers (called from NotificationActionReceiver) ------------------------------

    suspend fun onPeriodTransition() {
        transitionUntil = System.currentTimeMillis() + NotificationActions.TRANSITION_DURATION_MS
        refresh()
        scope.launch {
            delay(NotificationActions.TRANSITION_DURATION_MS)
            refresh()
        }
    }

    /** A task's time arrived: pop up a reminder for every task between the alarm's target
     * minute and now (covers alarms the OS delivered late), then re-arm. */
    suspend fun onTaskReminder(scheduledMinute: Int) {
        val settings = settingsRepository.getSettings()
        if (settings.taskRemindersEnabled) {
            val now = LocalTime.now()
            val nowMinute = now.hour * 60 + now.minute
            val from = if (scheduledMinute in 0..nowMinute) scheduledMinute else nowMinute
            val endOfToday = DateUtils.endOfToday()
            val due = taskRepository.getAllTasksOnce()
                .filter { TaskFilters.isActiveToday(it, endOfToday) }
                .filter { it.timeMinutes != null && it.timeMinutes in from..nowMinute }
                .sortedBy { it.timeMinutes }
            if (due.isNotEmpty()) postTaskReminder(due, settings)
        }
        refresh(forcePost = true)
    }

    suspend fun onNotePing() {
        val settings = settingsRepository.getSettings()
        if (!settings.notePingEnabled) return

        // Pinned notes ride along inside the ping so the important ones are right there.
        val pinned = noteRepository.getPinnedNotesOnce()
        val pinnedText = pinned.take(MAX_PINNED_IN_PING).joinToString("\n") { note ->
            "📌 ${note.content.replace('\n', ' ').take(120)}"
        }
        val contentText = pinnedText.ifBlank { "Tap to open the app" }

        val notification = NotificationCompat.Builder(context, NotificationActions.NOTE_PING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tasks)
            .setContentTitle(settings.notePingMessage.ifBlank { AppSettings.DEFAULT_NOTE_PING_MESSAGE })
            .setContentText(contentText.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(
                activityPendingIntent(NotificationActions.ACTION_OPEN_APP, NotificationActions.REQ_OPEN_NOTES)
            )
            // Pick which notes are pinned straight from the notification.
            .addAction(0, "Pin note", pinNotePendingIntent())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            // The ping is a nudge, not a task: quietly disappear after a minute.
            .setTimeoutAfter(60_000L)
            .build()
        notificationManager.notify(NotificationActions.NOTE_PING_NOTIFICATION_ID, notification)
        armNotePingAlarm(settings.notePingMinutes)
    }

    suspend fun onDayRollover() {
        refresh() // refresh() always runs the rollover first
    }

    fun stopAlert() = alertPlayer.stop()

    /** Skip button: file the reminded tasks as skipped-for-today and clear the reminder. */
    suspend fun onSkipTasks(taskIds: LongArray?) {
        if (taskIds == null || taskIds.isEmpty()) return
        taskIds.forEach { taskRepository.skipTaskForToday(it) }
        alertPlayer.stop()
        notificationManager.cancel(NotificationActions.TASK_REMINDER_NOTIFICATION_ID)
        refresh()
    }

    /** Snooze button: clear the reminder and re-arm it after the user's snooze duration. */
    suspend fun onSnoozeTasks(taskIds: LongArray?) {
        if (taskIds == null || taskIds.isEmpty()) return
        alertPlayer.stop()
        notificationManager.cancel(NotificationActions.TASK_REMINDER_NOTIFICATION_ID)
        val minutes = settingsRepository.getSettings().snoozeMinutes.coerceAtLeast(1)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + minutes * 60_000L,
            taskIdsPendingIntent(
                NotificationActions.ACTION_SNOOZED_REMINDER,
                NotificationActions.REQ_SNOOZED_REMINDER,
                taskIds
            )
        )
    }

    /** A snoozed reminder came due: re-remind for whichever of the tasks are still open. */
    suspend fun onSnoozedReminder(taskIds: LongArray?) {
        if (taskIds == null || taskIds.isEmpty()) return
        val settings = settingsRepository.getSettings()
        if (!settings.taskRemindersEnabled) return
        val stillDue = taskRepository.getAllTasksOnce()
            .filter { it.id in taskIds.toSet() && !it.isCompleted && !it.archived }
            .sortedBy { it.timeMinutes }
        if (stillDue.isNotEmpty()) {
            postTaskReminder(stillDue, settings)
            refresh(forcePost = true)
        }
    }

    // ---- Task reminder pop-up ------------------------------------------------------------

    private fun postTaskReminder(due: List<Task>, settings: AppSettings) {
        val first = due.first()
        val time = first.timeMinutes?.let { DateUtils.formatMinutesOfDay(it) }.orEmpty()
        val title = if (due.size == 1) "$time — ${first.title}" else "$time — ${due.size} tasks due"
        val text = if (due.size == 1) {
            first.description.ifBlank { "It's time for this task." }
        } else {
            due.joinToString(", ") { it.title }
        }
        val timeoutMs = settings.taskReminderSeconds.coerceAtLeast(10) * 1_000L
        val ids = due.map { it.id }.toLongArray()
        val notification = NotificationCompat.Builder(context, NotificationActions.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tasks)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(
                activityPendingIntent(NotificationActions.ACTION_OPEN_APP, NotificationActions.REQ_OPEN_APP)
            )
            // Dismissing (or timing out) the reminder also silences its alert.
            .setDeleteIntent(
                broadcastPendingIntent(NotificationActions.ACTION_STOP_ALERT, NotificationActions.REQ_STOP_ALERT)
            )
            .addAction(0, "Skip", taskIdsPendingIntent(NotificationActions.ACTION_SKIP_TASKS, NotificationActions.REQ_SKIP_TASKS, ids))
            .addAction(
                0,
                "Snooze ${settings.snoozeMinutes.coerceAtLeast(1)}m",
                taskIdsPendingIntent(NotificationActions.ACTION_SNOOZE_TASKS, NotificationActions.REQ_SNOOZE_TASKS, ids)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            // Auto-dismiss after the user-configured duration.
            .setTimeoutAfter(timeoutMs)
            .build()
        notificationManager.notify(NotificationActions.TASK_REMINDER_NOTIFICATION_ID, notification)

        // Sound/vibration per the task's alert profile; with several tasks due at once, the
        // strongest profile wins. A looping alarm is capped at the reminder's own lifetime.
        val loudest = due.maxBy { it.alertType.severity }
        alertPlayer.play(loudest.alertType, loudest.alertSoundUri, alarmCapMs = timeoutMs)
    }

    // ---- Task-list notification building -------------------------------------------------

    private fun buildNotification(content: PeriodContent, persistent: Boolean): Notification {
        val builder = NotificationCompat.Builder(context, NotificationActions.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tasks)
            .setContentIntent(
                activityPendingIntent(NotificationActions.ACTION_OPEN_APP, NotificationActions.REQ_OPEN_APP)
            )
            .setCustomContentView(buildCollapsedView(content))
            .setCustomBigContentView(buildExpandedView(content))
            .setOngoing(persistent)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
        if (persistent) {
            // If the user manages to dismiss it, this fires and we bring it straight back.
            builder.setDeleteIntent(
                broadcastPendingIntent(NotificationActions.ACTION_REVIVE, NotificationActions.REQ_REVIVE)
            )
        }
        return builder.build()
    }

    private fun headerText(content: PeriodContent): String =
        content.label?.let { "${content.title} · $it" } ?: content.title

    private fun buildCollapsedView(content: PeriodContent): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notification_collapsed)
        rv.setTextViewText(R.id.notif_period, headerText(content))
        rv.setTextViewText(R.id.notif_progress, content.progressText)
        rv.setTextViewText(R.id.notif_preview, content.previewText)
        return rv
    }

    private fun buildExpandedView(content: PeriodContent): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.notification_expanded)
        rv.setTextViewText(R.id.notif_period, headerText(content))
        rv.setTextViewText(R.id.notif_progress, content.progressText)

        rv.setOnClickPendingIntent(R.id.notif_add_task, quickAddPendingIntent(QuickAddActivity.MODE_TASK))
        rv.setOnClickPendingIntent(R.id.notif_add_note, quickAddPendingIntent(QuickAddActivity.MODE_NOTE))

        if (System.currentTimeMillis() < transitionUntil) {
            // Brief period-change summary instead of the list.
            rv.setViewVisibility(R.id.notif_summary, View.VISIBLE)
            rv.setViewVisibility(R.id.notif_task_container, View.GONE)
            rv.setViewVisibility(R.id.notif_empty, View.GONE)
            rv.setViewVisibility(R.id.notif_actions, View.GONE)
            val toGo = content.totalCount - content.completedCount
            rv.setTextViewText(
                R.id.notif_summary,
                "Switching to ${content.title} — ${content.completedCount} done, $toGo to go"
            )
            return rv
        }

        rv.setViewVisibility(R.id.notif_summary, View.GONE)
        rv.setViewVisibility(R.id.notif_actions, View.VISIBLE)

        // Build the task rows directly (addView) for reliable rendering across OS/OEM versions.
        rv.removeAllViews(R.id.notif_task_container)
        if (content.periodTasks.isEmpty()) {
            rv.setViewVisibility(R.id.notif_task_container, View.GONE)
            rv.setViewVisibility(R.id.notif_empty, View.VISIBLE)
            rv.setTextViewText(R.id.notif_empty, "No tasks for ${content.title}")
        } else {
            rv.setViewVisibility(R.id.notif_empty, View.GONE)
            rv.setViewVisibility(R.id.notif_task_container, View.VISIBLE)
            content.periodTasks.take(MAX_VISIBLE_TASKS).forEach { task ->
                rv.addView(R.id.notif_task_container, buildTaskRow(task))
            }
        }
        return rv
    }

    private fun buildTaskRow(task: Task): RemoteViews {
        // Compact single-line rows. Completed tasks (which sit at the bottom) show a filled check
        // and a strikethrough title; tapping a row toggles its completion.
        val row = RemoteViews(context.packageName, R.layout.notification_task_row)
        val prefix = task.timeMinutes?.let { "${DateUtils.formatMinutesOfDay(it)}   " } ?: ""
        val label = "$prefix${task.title}"
        val title: CharSequence = if (task.isCompleted) {
            SpannableString(label).apply {
                setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            label
        }
        row.setTextViewText(R.id.task_row_title, title)
        row.setImageViewResource(
            R.id.task_row_check,
            if (task.isCompleted) R.drawable.ic_task_check else R.drawable.ic_task_uncheck
        )
        row.setOnClickPendingIntent(R.id.task_row_root, rowTogglePendingIntent(task))
        return row
    }

    // ---- PendingIntents ------------------------------------------------------------------

    /** Broadcast PendingIntent to [NotificationActionReceiver]; same requestCode+action replaces
     * the previous one, which is how each alarm slot cancels/reschedules itself. */
    private fun broadcastPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun taskReminderPendingIntent(minuteOfDay: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(NotificationActions.ACTION_TASK_REMINDER)
            .putExtra(NotificationActions.EXTRA_REMINDER_MINUTE, minuteOfDay)
        return PendingIntent.getBroadcast(
            context,
            NotificationActions.REQ_TASK_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Broadcast PI carrying the reminded task ids (Skip / Snooze / snoozed-alarm targets). */
    private fun taskIdsPendingIntent(action: String, requestCode: Int, taskIds: LongArray): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActions.EXTRA_TASK_IDS, taskIds)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun activityPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Per-row toggle: a distinct broadcast PendingIntent per task (unique data avoids reuse). */
    private fun rowTogglePendingIntent(task: Task): PendingIntent {
        val action = if (task.isCompleted) {
            NotificationActions.ACTION_UNCOMPLETE_TASK
        } else {
            NotificationActions.ACTION_COMPLETE_TASK
        }
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("taskmanager://task/${task.id}")
            putExtra(NotificationActions.EXTRA_TASK_ID, task.id)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Launches the translucent quick-add popup (task or note) without opening the main app. */
    private fun quickAddPendingIntent(mode: String): PendingIntent {
        val requestCode = if (mode == QuickAddActivity.MODE_NOTE) {
            NotificationActions.REQ_ADD_NOTE
        } else {
            NotificationActions.REQ_ADD_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            QuickAddActivity.intent(context, mode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Opens the translucent pin-notes picker (same popup family as quick-add). */
    private fun pinNotePendingIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        NotificationActions.REQ_PIN_NOTE,
        QuickAddActivity.intent(context, QuickAddActivity.MODE_PIN),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // ---- Channels --------------------------------------------------------------------------

    private fun createChannels() {
        val persistentChannel = NotificationChannel(
            NotificationActions.CHANNEL_ID,
            NotificationActions.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = NotificationActions.CHANNEL_DESCRIPTION
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        // Heads-up importance but no channel sound/vibration: each task's own alert profile is
        // played by AlertPlayer instead (channels can't vary their sound per notification).
        val reminders = NotificationChannel(
            NotificationActions.REMINDER_CHANNEL_ID,
            NotificationActions.REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = NotificationActions.REMINDER_CHANNEL_DESCRIPTION
            setSound(null, null)
            enableVibration(false)
        }
        val notePings = NotificationChannel(
            NotificationActions.NOTE_PING_CHANNEL_ID,
            NotificationActions.NOTE_PING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = NotificationActions.NOTE_PING_CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(persistentChannel)
        notificationManager.createNotificationChannel(reminders)
        notificationManager.createNotificationChannel(notePings)
        // The old reminder channel had a fixed sound; superseded by per-task alert profiles.
        notificationManager.deleteNotificationChannel(NotificationActions.LEGACY_REMINDER_CHANNEL_ID)
    }

    private companion object {
        /** Cap on rows rendered in the expanded notification. Compact single-line rows let many
         * fit; the system still clips at its max height, and completing a task reveals the next. */
        const val MAX_VISIBLE_TASKS = 15

        /** Cap on pinned notes shown inside the note-ping notification. */
        const val MAX_PINNED_IN_PING = 5

        const val PREFS = "task_board"
        const val KEY_NOTE_PING_CONFIG = "note_ping_config"
    }
}
