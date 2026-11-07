package com.skai.taskmanager.data.mapper

import com.skai.taskmanager.data.local.entity.ProfileEntity
import com.skai.taskmanager.domain.model.Profile

fun ProfileEntity.toDomain(): Profile =
    Profile(id = id, name = name, bio = bio, photoPath = photoPath)

fun Profile.toEntity(): ProfileEntity =
    ProfileEntity(id = id, name = name, bio = bio, photoPath = photoPath)
