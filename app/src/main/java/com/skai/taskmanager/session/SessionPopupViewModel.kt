package com.skai.taskmanager.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionPopupViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val extensions: StateFlow<List<Int>> = settingsRepository.observeSettings()
        .map { it.extensions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(5, 10, 15))
}
