package com.skai.taskmanager.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.skai.taskmanager.session.SessionService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * After a reboot: re-posts the task-list notification, re-arms every alarm (alarms don't
 * survive reboots — the note ping in particular needs a fresh countdown), and resumes any
 * focus session. The session start is wrapped because background foreground-service starts
 * can be restricted on some OEM builds; the session simply resumes when the app next opens.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            val board = EntryPointAccessors.fromApplication(
                context.applicationContext,
                NotificationRepositoryEntryPoint::class.java
            ).taskBoard()
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    runCatching { board.refresh(rearmNotePing = true) }
                } finally {
                    pending.finish()
                }
            }
            runCatching { SessionService.start(context) }
        }
    }
}
