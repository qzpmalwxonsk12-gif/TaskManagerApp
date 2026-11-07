package com.skai.taskmanager.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import com.skai.taskmanager.domain.model.TaskAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays a [TaskAlert] profile: the four built-in sound/vibration combinations or a user-picked
 * custom sound. One shared instance (Hilt singleton) so a new alert always replaces the previous
 * one, and any part of the app (reminder pop-ups, session warnings/ends) can [stop] it — e.g.
 * when the user dismisses the notification or acts on a session popup.
 *
 * Sounds are played here rather than through notification channels because per-task sounds can't
 * be expressed with a channel (channels fix their sound at creation on Android 8+).
 */
@Singleton
class AlertPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val vibrator: Vibrator?
        get() = context.getSystemService(Vibrator::class.java)

    private val stopRunnable = Runnable { stop() }

    /**
     * Plays [alert]. [customSoundUri] backs [TaskAlert.CUSTOM]. [alarmCapMs] bounds the looping
     * ALARM profile (and any custom file); the softer profiles stop on their own sooner.
     */
    fun play(alert: TaskAlert, customSoundUri: String? = null, alarmCapMs: Long = DEFAULT_ALARM_CAP_MS) {
        stop()
        when (alert) {
            TaskAlert.NONE -> Unit

            // 1: a small vibration.
            TaskAlert.VIBRATE -> vibrate(longArrayOf(0, 250))

            // 2: vibration plus a small chime (the system notification sound).
            TaskAlert.CHIME -> {
                vibrate(longArrayOf(0, 250))
                playSound(defaultUri(RingtoneManager.TYPE_NOTIFICATION), loop = false, stopAfterMs = CHIME_MS)
            }

            // 3: a long vibration and a short part of a song (the device ringtone melody).
            TaskAlert.MELODY -> {
                vibrate(longArrayOf(0, 800))
                playSound(defaultUri(RingtoneManager.TYPE_RINGTONE), loop = false, stopAfterMs = MELODY_MS)
            }

            // 4: full-on alarm with constant vibration until stopped (or the cap).
            TaskAlert.ALARM -> {
                vibrate(longArrayOf(0, 600, 400), repeat = 0)
                playSound(defaultUri(RingtoneManager.TYPE_ALARM), loop = true, stopAfterMs = alarmCapMs, asAlarm = true)
            }

            // Custom: the user's own sound file, with a gentle vibration.
            TaskAlert.CUSTOM -> {
                vibrate(longArrayOf(0, 250))
                val uri = customSoundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    ?: defaultUri(RingtoneManager.TYPE_NOTIFICATION)
                playSound(uri, loop = false, stopAfterMs = minOf(CUSTOM_MAX_MS, alarmCapMs))
            }
        }
    }

    /** Stops any playing sound and vibration immediately. Safe to call at any time. */
    fun stop() {
        handler.removeCallbacks(stopRunnable)
        runCatching { vibrator?.cancel() }
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        player = null
    }

    private fun vibrate(pattern: LongArray, repeat: Int = -1) {
        val v = vibrator ?: return
        runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, repeat)) }
    }

    private fun playSound(uri: Uri?, loop: Boolean, stopAfterMs: Long, asAlarm: Boolean = false) {
        if (uri == null) {
            scheduleStop(stopAfterMs)
            return
        }
        runCatching {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (asAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(context, uri)
            mp.isLooping = loop
            if (!loop) mp.setOnCompletionListener { stop() }
            mp.prepare()
            mp.start()
            player = mp
        }.onFailure {
            player?.let { p -> runCatching { p.release() } }
            player = null
        }
        scheduleStop(stopAfterMs)
    }

    private fun scheduleStop(afterMs: Long) {
        handler.postDelayed(stopRunnable, afterMs)
    }

    private fun defaultUri(type: Int): Uri? =
        runCatching { RingtoneManager.getDefaultUri(type) }.getOrNull()

    private companion object {
        const val CHIME_MS = 5_000L
        const val MELODY_MS = 10_000L
        const val CUSTOM_MAX_MS = 30_000L
        const val DEFAULT_ALARM_CAP_MS = 60_000L
    }
}
