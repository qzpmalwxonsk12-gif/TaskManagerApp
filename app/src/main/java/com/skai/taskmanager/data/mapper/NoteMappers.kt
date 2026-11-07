package com.skai.taskmanager.data.mapper

import com.skai.taskmanager.data.local.entity.NoteEntity
import com.skai.taskmanager.domain.model.Note

fun NoteEntity.toDomain(): Note =
    Note(id = id, content = content, createdAt = createdAt, pinned = pinned)

fun Note.toEntity(): NoteEntity =
    NoteEntity(id = id, content = content, createdAt = createdAt, pinned = pinned)

@JvmName("noteEntityListToDomain")
fun List<NoteEntity>.toDomain(): List<Note> = map { it.toDomain() }
