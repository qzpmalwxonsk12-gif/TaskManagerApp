package com.skai.taskmanager.data.repository

import com.skai.taskmanager.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeAllNotes(): Flow<List<Note>>
    suspend fun addNote(content: String): Long
    suspend fun deleteNote(note: Note)

    /** Pins/unpins a note. Pinned notes sort first and ride along in the note-ping notification. */
    suspend fun setPinned(noteId: Long, pinned: Boolean)

    /** One-shot snapshot of the pinned notes (for the notification builder). */
    suspend fun getPinnedNotesOnce(): List<Note>
}
