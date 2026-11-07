package com.skai.taskmanager.session

import android.app.NotificationManager
import android.content.Context

/**
 * Best-effort Do Not Disturb control for focus sessions. Requires the user to have granted
 * notification-policy access; every call fails gracefully (no crash) when it isn't available.
 */
class DndController(context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)

    fun hasAccess(): Boolean = runCatching { nm.isNotificationPolicyAccessGranted }.getOrDefault(false)

    /** Turns on DND (priority-only) and returns the previous interruption filter to restore later. */
    fun enable(): Int? {
        if (!hasAccess()) return null
        val previous = runCatching { nm.currentInterruptionFilter }.getOrNull() ?: return null
        runCatching {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
        return previous
    }

    /** Restores a previously-saved interruption filter. */
    fun restore(previousFilter: Int?) {
        if (previousFilter == null || !hasAccess()) return
        runCatching { nm.setInterruptionFilter(previousFilter) }
    }
}
