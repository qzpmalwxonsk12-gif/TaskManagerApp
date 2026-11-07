package com.skai.taskmanager.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.NoteRepository
import com.skai.taskmanager.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<NotesUiState> = repository.observeAllNotes()
        .map { NotesUiState(notes = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState()
        )

    fun addNote(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch { repository.addNote(content) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    /** Pin/unpin: pinned notes sort first and appear inside the notes-reminder notification. */
    fun togglePinned(note: Note) {
        viewModelScope.launch { repository.setPinned(note.id, !note.pinned) }
    }
}
