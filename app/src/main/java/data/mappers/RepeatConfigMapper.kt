package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.RepeatConfigEntity
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.RepeatPlan

class RepeatConfigMapper : EntityMapper<RepeatConfigEntity, RepeatPlan> {
    override fun mapToDomain(entity: RepeatConfigEntity): RepeatPlan {
        val frequencyType = try {
            FrequencyType.valueOf(entity.frequencyType)
        } catch (e: Exception) {
            FrequencyType.NONE
        }

        return RepeatPlan(
            frequencyType = frequencyType,
            interval = entity.interval,
            intervalUnit = entity.intervalUnit,
            selectedDays = entity.selectedDays
        )
    }

    override fun mapToEntity(domain: RepeatPlan): RepeatConfigEntity {
        return RepeatConfigEntity(
            id = 0,
            taskId = 0,
            frequencyType = domain.frequencyType.name,
            interval = domain.interval,
            intervalUnit = domain.intervalUnit,
            selectedDays = domain.selectedDays
        )
    }

    fun mapToEntityWithTaskId(domain: RepeatPlan, taskId: Int): RepeatConfigEntity {
        return RepeatConfigEntity(
            id = 0,
            taskId = taskId,
            frequencyType = domain.frequencyType.name,
            interval = domain.interval,
            intervalUnit = domain.intervalUnit,
            selectedDays = domain.selectedDays
        )
    }
}