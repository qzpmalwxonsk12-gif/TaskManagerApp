package com.skai.taskmanager.domain.model

/**
 * A free-form note. Stored independently of tasks (Stage: Notes) in its own table.
 */
data class Note(
    val id: Long = 0L,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Pinned notes sort to the top and are shown inside the periodic note-ping notification. */
    val pinned: Boolean = false
)
