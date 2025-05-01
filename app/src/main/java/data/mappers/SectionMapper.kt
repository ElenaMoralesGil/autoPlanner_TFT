package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.local.entities.SectionEntity
import com.elena.autoplanner.domain.models.TaskSection

fun SectionEntity.toDomain(): TaskSection {
    return TaskSection(
        id = this.id,
        listId = this.listId,
        name = this.name,
        displayOrder = this.displayOrder
    )
}

fun TaskSection.toEntity(): SectionEntity {
    return SectionEntity(
        id = this.id,
        listId = this.listId,
        name = this.name,
        displayOrder = this.displayOrder
    )
}