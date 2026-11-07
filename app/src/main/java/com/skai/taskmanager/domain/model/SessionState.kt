package com.skai.taskmanager.domain.model

/** Lifecycle of a focus session attached to a Session Task. */
enum class SessionState {
    IDLE,       // a session task that hasn't been started
    RUNNING,    // counting down
    PAUSED,     // frozen
    FINISHED;   // timer reached zero (awaiting completion / extension)

    companion object {
        fun fromName(value: String?): SessionState =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: IDLE
    }
}
