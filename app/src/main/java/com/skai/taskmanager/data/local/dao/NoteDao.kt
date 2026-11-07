package com.skai.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skai.taskmanager.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY pinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    /** Pinned notes, newest first — shown inside the note-ping notification. */
    @Query("SELECT * FROM notes WHERE pinned = 1 ORDER BY createdAt DESC")
    suspend fun getPinnedOnce(): List<NoteEntity>

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :noteId")
    suspend fun setPinned(noteId: Long, pinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)
}
