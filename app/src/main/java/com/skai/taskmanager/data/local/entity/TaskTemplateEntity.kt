package com.skai.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skai.taskmanager.domain.model.Category
import com.skai.taskmanager.domain.model.Priority
import com.skai.taskmanager.domain.model.RecurrenceType

/**
 * The reusable *pattern* behind a recurring task. Instances ([TaskEntity]) reference this via
 * their `templateId`. Editing/skipping a single instance does not touch the template, so the
 * repeating pattern is preserved.
 */
@Entity(tableName = "task_templates")
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val category: Category,
    val priority: Priority,
    val recurrenceType: RecurrenceType,
    val recurrenceIntervalDays: Int,
    val createdAt: Long,
    val isActive: Boolean
)
