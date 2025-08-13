package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.HabitCompletionEntity
import com.elena.autoplanner.domain.models.HabitCompletion
import java.time.LocalDate
import java.time.LocalDateTime

class HabitCompletionMapper : EntityMapper<HabitCompletionEntity, HabitCompletion> {

    override fun mapToDomain(entity: HabitCompletionEntity): HabitCompletion {
        return HabitCompletion(
            habitId = entity.habitId,
            date = LocalDate.parse(entity.date),
            completed = entity.completed,
            completedAt = entity.completedAt?.let { LocalDateTime.parse(it) },
            notes = entity.notes
        )
    }

    override fun mapToEntity(domain: HabitCompletion): HabitCompletionEntity {
        return HabitCompletionEntity(
            habitId = domain.habitId,
            date = domain.date.toString(),
            completed = domain.completed,
            completedAt = domain.completedAt?.toString(),
            notes = domain.notes
        )
    }
}
