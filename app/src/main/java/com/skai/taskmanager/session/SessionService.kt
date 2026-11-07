package com.skai.taskmanager.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.skai.taskmanager.R
import com.skai.taskmanager.data.repository.SettingsRepository
import com.skai.taskmanager.data.repository.StatsRepository
import com.skai.taskmanager.data.repository.TaskRepository
import com.skai.taskmanager.domain.model.AppSettings
import com.skai.taskmanager.domain.model.SessionState
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.notification.AlertPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dedicated foreground service for focus sessions — independent of the task notification (they
 * coordinate only through Room). It owns the countdown, the session notification, DND, and the
 * 5-minute / completion alerts. Only one session runs at a time.
 */
@AndroidEntryPoint
class SessionService : Service() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var statsRepository: StatsRepository
    @Inject lateinit var alertPlayer: AlertPlayer

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private val handler = Handler(Looper.getMainLooper())
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val dnd by lazy { DndController(this) }

    @Volatile private var focus: Task? = null
    @Volatile private var settings: AppSettings = AppSettings.DEFAULT

    private var ticking = false
    private var warningFired = false
    private var endFired = false

    /** User-configured "almost done" lead time (defaults to 5 minutes). */
    private fun warningMinutes(): Int = settings.sessionWarningMinutes.coerceAtLeast(1)

    private fun warningSeconds(): Int = warningMinutes() * 60

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForegroundCompat(placeholderNotification())
        observe()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getLongExtra(SessionActions.EXTRA_TASK_ID, -1L) ?: -1L
        when (intent?.action) {
            SessionActions.ACTION_START -> onStart(taskId)
            SessionActions.ACTION_PAUSE -> onPause()
            SessionActions.ACTION_RESUME -> onResume()
            SessionActions.ACTION_COMPLETE -> onComplete()
            SessionActions.ACTION_RESTART -> onRestart()
            SessionActions.ACTION_STOP -> onStop()
            SessionActions.ACTION_EXTEND ->
                onExtend(intent.getIntExtra(SessionActions.EXTRA_MINUTES, 5))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    // ---- Observation ---------------------------------------------------------------------

    private fun observe() {
        scope.launch {
            combine(taskRepository.observeAllTasks(), settingsRepository.observeSettings()) { t, s ->
                t to s
            }.collectLatest { (tasks, s) ->
                settings = s
                val active = tasks.firstOrNull {
                    it.isSessionTask && !it.isCompleted &&
                        it.sessionState in ACTIVE_STATES
                }
                val ready = tasks.firstOrNull {
                    it.isSessionTask && !it.isCompleted && it.sessionState == SessionState.IDLE
                }
                val newFocus = active ?: ready
                focus = newFocus

                if (newFocus == null) {
                    stopEverything()
                } else {
                    if (newFocus.sessionState == SessionState.RUNNING) startTicking() else stopTicking()
                    updateNotification()
                }
            }
        }
    }

    // ---- Actions -------------------------------------------------------------------------

    private fun onStart(taskId: Long) = scope.launch {
        val task = taskRepository.getTask(taskId)?.takeIf { it.isSessionTask } ?: return@launch
        val current = focus
        if (current != null && current.id != taskId && current.sessionState in ACTIVE_STATES) {
            // Another session is active — ask before replacing it.
            launchPopup(SessionActions.POPUP_COMPLETE, current.id) // surfaces conflict via popup
            return@launch
        }
        val duration = task.sessionDurationMinutes ?: return@launch
        val now = System.currentTimeMillis()
        warningFired = duration <= warningMinutes()
        endFired = false
        taskRepository.updateSession(
            taskId, SessionState.RUNNING, now, now + duration * 60_000L, duration * 60
        )
        taskRepository.setSessionScheduled(taskId, duration * 60)
        if (settings.dndDuringSession) enableDnd()
    }

    private fun onPause() = scope.launch {
        val t = focus ?: return@launch
        if (t.sessionState != SessionState.RUNNING) return@launch
        taskRepository.updateSession(
            t.id, SessionState.PAUSED, t.sessionStartTime, null, remainingSeconds(t)
        )
    }

    private fun onResume() = scope.launch {
        val t = focus ?: return@launch
        if (t.sessionState != SessionState.PAUSED) return@launch
        val rem = t.sessionRemainingSeconds ?: 0
        warningFired = rem <= warningSeconds()
        taskRepository.updateSession(
            t.id, SessionState.RUNNING, t.sessionStartTime,
            System.currentTimeMillis() + rem * 1000L, rem
        )
    }

    private fun onExtend(minutes: Int) = scope.launch {
        val t = focus ?: return@launch
        val add = minutes.coerceAtLeast(1)
        val base = t.sessionScheduledSeconds ?: ((t.sessionDurationMinutes ?: 0) * 60)
        when (t.sessionState) {
            SessionState.RUNNING -> {
                val end = (t.sessionEndTime ?: System.currentTimeMillis()) + add * 60_000L
                warningFired = (end - System.currentTimeMillis()) / 1000 <= warningSeconds()
                endFired = false
                taskRepository.updateSession(t.id, SessionState.RUNNING, t.sessionStartTime, end, null)
                taskRepository.setSessionScheduled(t.id, base + add * 60)
            }
            SessionState.PAUSED -> {
                val rem = (t.sessionRemainingSeconds ?: 0) + add * 60
                taskRepository.updateSession(t.id, SessionState.PAUSED, t.sessionStartTime, null, rem)
                taskRepository.setSessionScheduled(t.id, base + add * 60)
            }
            SessionState.FINISHED -> {
                val now = System.currentTimeMillis()
                warningFired = add <= warningMinutes()
                endFired = false
                taskRepository.updateSession(t.id, SessionState.RUNNING, t.sessionStartTime, now + add * 60_000L, add * 60)
                taskRepository.setSessionScheduled(t.id, base + add * 60)
            }
            else -> Unit
        }
        alertPlayer.stop()
        notificationManager.cancel(SessionActions.ALERT_NOTIFICATION_ID)
    }

    private fun onComplete() = scope.launch {
        val t = focus ?: return@launch
        // Record the focused duration (scheduled total minus whatever was left) before completing.
        val scheduled = t.sessionScheduledSeconds ?: ((t.sessionDurationMinutes ?: 0) * 60)
        val focused = (scheduled - remainingSeconds(t)).coerceAtLeast(0)
        statsRepository.recordCompletion(t.title, focused, System.currentTimeMillis())
        taskRepository.getTask(t.id)?.let { taskRepository.setTaskCompleted(it, true) }
        taskRepository.updateSession(t.id, SessionState.FINISHED, t.sessionStartTime, null, 0)
        alertPlayer.stop()
        notificationManager.cancel(SessionActions.ALERT_NOTIFICATION_ID)
        // Completing the task removes it from the active set, so `observe` will tear the service down.
    }

    private fun onRestart() = scope.launch {
        val t = focus ?: return@launch
        val duration = t.sessionDurationMinutes ?: return@launch
        val now = System.currentTimeMillis()
        warningFired = duration <= warningMinutes()
        endFired = false
        taskRepository.updateSession(t.id, SessionState.RUNNING, now, now + duration * 60_000L, duration * 60)
        taskRepository.setSessionScheduled(t.id, duration * 60)
        alertPlayer.stop()
        notificationManager.cancel(SessionActions.ALERT_NOTIFICATION_ID)
    }

    private fun onStop() = scope.launch {
        val t = focus ?: return@launch
        taskRepository.updateSession(t.id, SessionState.IDLE, null, null, null)
        restoreDnd()
        alertPlayer.stop()
        notificationManager.cancel(SessionActions.ALERT_NOTIFICATION_ID)
    }

    // ---- Countdown -----------------------------------------------------------------------

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!ticking) return
            onTick()
            handler.postDelayed(this, 1000)
        }
    }

    private fun startTicking() {
        if (ticking) return
        ticking = true
        handler.post(tickRunnable)
    }

    private fun stopTicking() {
        ticking = false
        handler.removeCallbacks(tickRunnable)
    }

    private fun onTick() {
        val t = focus ?: return
        if (t.sessionState != SessionState.RUNNING) {
            stopTicking()
            return
        }
        val rem = remainingSeconds(t)
        if (rem in 1..warningSeconds() && !warningFired) {
            warningFired = true
            fireWarning()
        }
        if (rem <= 0 && !endFired) {
            endFired = true
            fireEnd()
            return
        }
        updateNotification()
    }

    private fun fireWarning() {
        focus?.let { alertPlayer.play(it.sessionWarningAlert, it.alertSoundUri, alarmCapMs = 30_000L) }
        launchPopup(SessionActions.POPUP_WARNING, focus?.id ?: -1L)
    }

    private fun fireEnd() {
        val t = focus ?: return
        // The task's own end-of-session alert: rings whether or not the user is watching; an
        // ALARM profile keeps going until they act on the popup (or the cap).
        alertPlayer.play(t.sessionEndAlert, t.alertSoundUri, alarmCapMs = 120_000L)
        scope.launch {
            taskRepository.updateSession(t.id, SessionState.FINISHED, t.sessionStartTime, null, 0)
        }
        launchPopup(SessionActions.POPUP_COMPLETE, t.id)
        // Fallback auto-complete after 30s if the user ignores everything.
        handler.postDelayed({
            val current = focus
            if (current != null && current.id == t.id &&
                current.sessionState == SessionState.FINISHED && settings.autoCompleteOnTimerEnd
            ) {
                onComplete()
            }
        }, 30_000)
    }

    // ---- Notification --------------------------------------------------------------------

    private fun updateNotification() {
        val t = focus ?: return
        notificationManager.notify(SessionActions.SESSION_NOTIFICATION_ID, buildSessionNotification(t))
    }

    private fun buildSessionNotification(task: Task): Notification {
        val rv = RemoteViews(packageName, R.layout.notification_session)
        rv.setTextViewText(R.id.session_title, task.title)

        val durationSec = (task.sessionDurationMinutes ?: 0) * 60
        val rem = remainingSeconds(task)
        val e = settings

        when (task.sessionState) {
            SessionState.IDLE -> {
                rv.setTextViewText(R.id.session_time, "⏱ ${task.sessionDurationMinutes} min • ready")
                rv.setProgressBar(R.id.session_progress, 100, 0, false)
                applyButtons(rv, listOf("Start" to servicePi(SessionActions.ACTION_START, task.id, 1)))
            }
            SessionState.RUNNING -> {
                rv.setTextViewText(R.id.session_time, "${formatTime(rem)} remaining")
                rv.setProgressBar(R.id.session_progress, durationSec.coerceAtLeast(1), durationSec - rem, false)
                applyButtons(rv, listOf(
                    "Pause" to servicePi(SessionActions.ACTION_PAUSE, task.id, 2),
                    "+${e.ext1}" to extendPi(task.id, e.ext1),
                    "+${e.ext2}" to extendPi(task.id, e.ext2),
                    "+${e.ext3}" to extendPi(task.id, e.ext3),
                    "Done" to servicePi(SessionActions.ACTION_COMPLETE, task.id, 3)
                ))
            }
            SessionState.PAUSED -> {
                rv.setTextViewText(R.id.session_time, "Paused • ${formatTime(rem)} left")
                rv.setProgressBar(R.id.session_progress, durationSec.coerceAtLeast(1), durationSec - rem, false)
                applyButtons(rv, listOf(
                    "Resume" to servicePi(SessionActions.ACTION_RESUME, task.id, 4),
                    "+${e.ext1}" to extendPi(task.id, e.ext1),
                    "+${e.ext2}" to extendPi(task.id, e.ext2),
                    "+${e.ext3}" to extendPi(task.id, e.ext3),
                    "Done" to servicePi(SessionActions.ACTION_COMPLETE, task.id, 3)
                ))
            }
            SessionState.FINISHED -> {
                rv.setTextViewText(R.id.session_time, "Time's up")
                rv.setProgressBar(R.id.session_progress, 100, 100, false)
                applyButtons(rv, listOf(
                    "Done" to servicePi(SessionActions.ACTION_COMPLETE, task.id, 3),
                    "+${e.ext1}" to extendPi(task.id, e.ext1),
                    "+${e.ext2}" to extendPi(task.id, e.ext2),
                    "+${e.ext3}" to extendPi(task.id, e.ext3),
                    "Restart" to servicePi(SessionActions.ACTION_RESTART, task.id, 5)
                ))
            }
        }

        return NotificationCompat.Builder(this, SessionActions.SESSION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tasks)
            .setCustomContentView(rv)
            .setCustomBigContentView(rv)
            .setOngoing(task.sessionState != SessionState.IDLE)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private val buttonIds = intArrayOf(
        R.id.session_btn1, R.id.session_btn2, R.id.session_btn3, R.id.session_btn4, R.id.session_btn5
    )

    private fun applyButtons(rv: RemoteViews, buttons: List<Pair<String, PendingIntent>>) {
        buttonIds.forEachIndexed { index, id ->
            val button = buttons.getOrNull(index)
            if (button == null) {
                rv.setViewVisibility(id, android.view.View.GONE)
            } else {
                rv.setViewVisibility(id, android.view.View.VISIBLE)
                rv.setTextViewText(id, button.first)
                rv.setOnClickPendingIntent(id, button.second)
            }
        }
    }

    private fun servicePi(action: String, taskId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(this, SessionService::class.java)
            .setAction(action)
            .putExtra(SessionActions.EXTRA_TASK_ID, taskId)
        return PendingIntent.getForegroundService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun extendPi(taskId: Long, minutes: Int): PendingIntent {
        val intent = Intent(this, SessionService::class.java)
            .setAction(SessionActions.ACTION_EXTEND)
            .setData(Uri.parse("session://extend/$minutes"))
            .putExtra(SessionActions.EXTRA_TASK_ID, taskId)
            .putExtra(SessionActions.EXTRA_MINUTES, minutes)
        return PendingIntent.getForegroundService(
            this, 8000 + minutes, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ---- Alerts (popups) -----------------------------------------------------------------

    private fun launchPopup(type: String, taskId: Long) {
        val intent = SessionPopupActivity.intent(this, type, taskId)
        val pi = PendingIntent.getActivity(
            this, if (type == SessionActions.POPUP_WARNING) 91 else 92, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (type == SessionActions.POPUP_WARNING) "Session almost finished" else "Session complete"
        val text = if (type == SessionActions.POPUP_WARNING) {
            "${warningMinutes()} minutes remaining"
        } else {
            "Tap to extend or complete"
        }
        val notif = NotificationCompat.Builder(this, SessionActions.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tasks)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        notificationManager.notify(SessionActions.ALERT_NOTIFICATION_ID, notif)
        // Best-effort: also try to surface the popup directly over the current screen.
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    // ---- DND -----------------------------------------------------------------------------

    private suspend fun enableDnd() {
        val previous = dnd.enable() ?: return
        settingsRepository.update(settings.copy(savedDndFilter = previous))
    }

    private suspend fun restoreDnd() {
        val saved = settingsRepository.getSettings().savedDndFilter ?: return
        dnd.restore(saved)
        settingsRepository.update(settingsRepository.getSettings().copy(savedDndFilter = null))
    }

    // ---- Helpers -------------------------------------------------------------------------

    private fun remainingSeconds(task: Task): Int = when (task.sessionState) {
        SessionState.RUNNING -> {
            val end = task.sessionEndTime
            if (end == null) 0 else (((end - System.currentTimeMillis()) / 1000).toInt()).coerceAtLeast(0)
        }
        SessionState.PAUSED -> task.sessionRemainingSeconds ?: 0
        SessionState.FINISHED -> 0
        SessionState.IDLE -> (task.sessionDurationMinutes ?: 0) * 60
    }

    private fun formatTime(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%d:%02d", m, sec)
    }

    private fun stopEverything() {
        stopTicking()
        scope.launch { restoreDnd() }
        alertPlayer.stop()
        notificationManager.cancel(SessionActions.ALERT_NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun placeholderNotification(): Notification =
        NotificationCompat.Builder(this, SessionActions.SESSION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tasks)
            .setContentTitle("Focus")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SessionActions.SESSION_NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(SessionActions.SESSION_NOTIFICATION_ID, notification)
        }
    }

    private fun createChannels() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SessionActions.SESSION_CHANNEL_ID,
                SessionActions.SESSION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SessionActions.ALERT_CHANNEL_ID,
                SessionActions.ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        private val ACTIVE_STATES = setOf(
            SessionState.RUNNING, SessionState.PAUSED, SessionState.FINISHED
        )

        fun start(context: Context, action: String? = null, taskId: Long = -1L) {
            val intent = Intent(context, SessionService::class.java)
            if (action != null) {
                intent.action = action
                intent.putExtra(SessionActions.EXTRA_TASK_ID, taskId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun extend(context: Context, taskId: Long, minutes: Int) {
            val intent = Intent(context, SessionService::class.java)
                .setAction(SessionActions.ACTION_EXTEND)
                .putExtra(SessionActions.EXTRA_TASK_ID, taskId)
                .putExtra(SessionActions.EXTRA_MINUTES, minutes)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
