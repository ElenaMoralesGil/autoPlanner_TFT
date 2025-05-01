package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.local.entities.ListEntity
import com.elena.autoplanner.domain.models.TaskList

fun ListEntity.toDomain(): TaskList {
    return TaskList(
        id = this.id,
        name = this.name,
        colorHex = this.colorHex
    )
}

fun TaskList.toEntity(): ListEntity {
    return ListEntity(
        id = this.id,
        name = this.name,
        colorHex = this.colorHex
    )
}