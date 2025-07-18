package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.ListEntity
import com.elena.autoplanner.domain.models.TaskList

fun ListEntity.toDomain(): TaskList {
    return TaskList(
        id = this.id,
        name = this.name,
        colorHex = this.colorHex
    )
}

fun TaskList.toEntity(
    userId: String? = null,
    firestoreId: String? = null,
    lastUpdated: Long? = null
): ListEntity {
    return ListEntity(
        id = this.id, 
        firestoreId = firestoreId,
        userId = userId,
        name = this.name,
        colorHex = this.colorHex,
        lastUpdated = lastUpdated ?: System.currentTimeMillis()
    )
}