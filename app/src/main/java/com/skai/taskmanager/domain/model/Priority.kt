package com.skai.taskmanager.domain.model

/**
 * Task priority. [sortWeight] is used to order tasks (High first).
 */
enum class Priority(val displayName: String, val sortWeight: Int) {
    HIGH("High", 0),
    MEDIUM("Medium", 1),
    LOW("Low", 2);

    companion object {
        val DEFAULT = MEDIUM
    }
}
