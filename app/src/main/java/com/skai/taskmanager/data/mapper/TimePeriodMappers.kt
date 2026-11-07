package com.skai.taskmanager.data.mapper

import com.skai.taskmanager.data.local.entity.TimePeriodEntity
import com.skai.taskmanager.domain.model.TimePeriod

fun TimePeriodEntity.toDomain(): TimePeriod = TimePeriod(
    id = id,
    name = name,
    startHour = startHour,
    startMinute = startMinute,
    endHour = endHour,
    endMinute = endMinute,
    isEnabled = isEnabled,
    sortOrder = sortOrder
)

fun TimePeriod.toEntity(): TimePeriodEntity = TimePeriodEntity(
    id = id,
    name = name,
    startHour = startHour,
    startMinute = startMinute,
    endHour = endHour,
    endMinute = endMinute,
    isEnabled = isEnabled,
    sortOrder = sortOrder
)

@JvmName("timePeriodEntityListToDomain")
fun List<TimePeriodEntity>.toDomain(): List<TimePeriod> = map { it.toDomain() }
