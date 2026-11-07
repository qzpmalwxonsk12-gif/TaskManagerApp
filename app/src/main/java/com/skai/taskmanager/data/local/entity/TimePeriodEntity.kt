package com.skai.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence for a configurable day period. Mirrors
 * [com.skai.taskmanager.domain.model.TimePeriod]; mapped via TimePeriodMappers.
 */
@Entity(tableName = "time_periods")
data class TimePeriodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isEnabled: Boolean,
    val sortOrder: Int
)
