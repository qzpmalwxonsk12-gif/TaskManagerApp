package com.skai.taskmanager.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.ProfileRepository
import com.skai.taskmanager.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ProfileRepository
) : ViewModel() {

    val profile: StateFlow<Profile> = repository.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Profile.DEFAULT)

    fun saveDetails(name: String, bio: String) {
        viewModelScope.launch {
            repository.save(profile.value.copy(name = name.trim(), bio = bio.trim()))
        }
    }

    /** Copies the picked image into app storage and stores its path (so it survives permission loss). */
    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val newPath = withContext(Dispatchers.IO) { copyToInternal(uri) } ?: return@launch
            val current = profile.value
            current.photoPath?.let { old -> runCatching { File(old).delete() } }
            repository.save(current.copy(photoPath = newPath))
        }
    }

    private fun copyToInternal(uri: Uri): String? = runCatching {
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrNull()
}
