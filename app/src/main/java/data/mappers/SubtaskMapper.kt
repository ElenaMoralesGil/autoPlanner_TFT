package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.domain.models.Subtask

class SubtaskMapper : EntityMapper<SubtaskEntity, Subtask> {
    override fun mapToDomain(entity: SubtaskEntity): Subtask {
        return Subtask(
            id = entity.id,
            name = entity.name,
            isCompleted = entity.isCompleted,
            estimatedDurationInMinutes = entity.estimatedDurationInMinutes
        )
    }

    override fun mapToEntity(domain: Subtask): SubtaskEntity {
        return SubtaskEntity(
            id = domain.id,
            parentTaskId = 0,
            name = domain.name,
            isCompleted = domain.isCompleted,
            estimatedDurationInMinutes = domain.estimatedDurationInMinutes
        )
    }

    fun mapToEntityWithTaskId(domain: Subtask, taskId: Int): SubtaskEntity {
        return SubtaskEntity(
            id = domain.id,
            parentTaskId = taskId,
            name = domain.name,
            isCompleted = domain.isCompleted,
            estimatedDurationInMinutes = domain.estimatedDurationInMinutes
        )
    }
}