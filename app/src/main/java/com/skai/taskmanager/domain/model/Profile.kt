package com.skai.taskmanager.domain.model

/**
 * The user's profile. A single-row record (always [id] = 1).
 */
data class Profile(
    val id: Int = SINGLETON_ID,
    val name: String = "",
    val bio: String = "",
    val photoPath: String? = null
) {
    companion object {
        const val SINGLETON_ID = 1
        val DEFAULT = Profile()
    }
}
