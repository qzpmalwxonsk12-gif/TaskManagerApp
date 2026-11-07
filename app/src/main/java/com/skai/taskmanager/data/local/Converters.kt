package com.skai.taskmanager.data.local

import androidx.room.TypeConverter
import com.skai.taskmanager.domain.model.Category
import com.skai.taskmanager.domain.model.Priority
import com.skai.taskmanager.domain.model.RecurrenceType

/**
 * Room type converters for the enums we persist. Enums are stored by [Enum.name] so the
 * stored value stays stable even if the ordinal ordering changes later.
 */
class Converters {

    @TypeConverter
    fun categoryToString(category: Category): String = category.name

    @TypeConverter
    fun stringToCategory(value: String): Category =
        runCatching { Category.valueOf(value) }.getOrDefault(Category.DEFAULT)

    @TypeConverter
    fun priorityToString(priority: Priority): String = priority.name

    @TypeConverter
    fun stringToPriority(value: String): Priority =
        runCatching { Priority.valueOf(value) }.getOrDefault(Priority.DEFAULT)

    @TypeConverter
    fun recurrenceTypeToString(type: RecurrenceType?): String? = type?.name

    @TypeConverter
    fun stringToRecurrenceType(value: String?): RecurrenceType? =
        value?.let { runCatching { RecurrenceType.valueOf(it) }.getOrNull() }
}
