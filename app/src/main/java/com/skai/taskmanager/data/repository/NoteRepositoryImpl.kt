package com.skai.taskmanager.data.repository

import com.skai.taskmanager.data.local.dao.NoteDao
import com.skai.taskmanager.data.mapper.toDomain
import com.skai.taskmanager.data.mapper.toEntity
import com.skai.taskmanager.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val dao: NoteDao
) : NoteRepository {

    override fun observeAllNotes(): Flow<List<Note>> =
        dao.observeAll().map { it.toDomain() }

    override suspend fun addNote(content: String): Long =
        dao.insert(Note(content = content.trim()).toEntity())

    override suspend fun deleteNote(note: Note) = dao.delete(note.toEntity())

    override suspend fun setPinned(noteId: Long, pinned: Boolean) = dao.setPinned(noteId, pinned)

    override suspend fun getPinnedNotesOnce(): List<Note> = dao.getPinnedOnce().toDomain()
}
