package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.SectionEntity
import com.elena.autoplanner.domain.models.TaskSection

fun SectionEntity.toDomain(): TaskSection {
    return TaskSection(
        id = this.id,
        listId = this.listId,
        name = this.name,
        displayOrder = this.displayOrder
    )
}

fun TaskSection.toEntity(
    userId: String? = null,
    firestoreId: String? = null,
    lastUpdated: Long? = null
): SectionEntity {
    return SectionEntity(
        id = this.id, 
        firestoreId = firestoreId,
        userId = userId,
        listId = this.listId, 
        name = this.name,
        displayOrder = this.displayOrder,
        lastUpdated = lastUpdated ?: System.currentTimeMillis()
    )
}