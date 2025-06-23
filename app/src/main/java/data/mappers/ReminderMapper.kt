package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.ReminderEntity
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan

class ReminderMapper : EntityMapper<ReminderEntity, ReminderPlan> {
    override fun mapToDomain(entity: ReminderEntity): ReminderPlan {
        val reminderMode = try {
            ReminderMode.valueOf(entity.mode)
        } catch (e: Exception) {
            ReminderMode.NONE
        }

        return ReminderPlan(
            mode = reminderMode,
            offsetMinutes = entity.offsetMinutes,
            exactDateTime = entity.exactDateTime
        )
    }

    override fun mapToEntity(domain: ReminderPlan): ReminderEntity {
        return ReminderEntity(
            id = 0,
            taskId = 0,
            mode = domain.mode.name,
            offsetMinutes = domain.offsetMinutes,
            exactDateTime = domain.exactDateTime
        )
    }

    fun mapToEntityWithTaskId(domain: ReminderPlan, taskId: Int): ReminderEntity {
        return ReminderEntity(
            id = 0,
            taskId = taskId,
            mode = domain.mode.name,
            offsetMinutes = domain.offsetMinutes,
            exactDateTime = domain.exactDateTime
        )
    }
}