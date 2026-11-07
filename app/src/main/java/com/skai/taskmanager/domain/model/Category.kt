package com.skai.taskmanager.domain.model

/**
 * The category a task belongs to. The ordinal/name is what gets persisted by Room
 * via [com.skai.taskmanager.data.local.Converters].
 */
enum class Category(val displayName: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    SHOPPING("Shopping"),
    OTHER("Other");

    companion object {
        val DEFAULT = OTHER
    }
}
