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

fun TaskList.toEntity(
    userId: String? = null,
    firestoreId: String? = null,
    lastUpdated: Long? = null
): ListEntity {
    return ListEntity(
        id = this.id, // Use domain ID (0 for new)
        firestoreId = firestoreId,
        userId = userId,
        name = this.name,
        colorHex = this.colorHex,
        lastUpdated = lastUpdated ?: System.currentTimeMillis()
    )
}