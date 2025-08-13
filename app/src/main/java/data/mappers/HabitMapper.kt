package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.HabitEntity
import com.elena.autoplanner.domain.models.*
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalDateTime

class HabitMapper : EntityMapper<HabitEntity, Habit> {
    private val gson = Gson()
    private val reminderMapper = ReminderMapper()

    override fun mapToDomain(entity: HabitEntity): Habit {
        return Habit(
            id = entity.id,
            name = entity.name,
            createdDateTime = LocalDateTime.parse(entity.createdDateTime),
            type = PlannerItemType.HABIT,
            targetFrequency = HabitFrequency.valueOf(entity.targetFrequency),
            estimatedDurationMinutes = entity.estimatedDurationMinutes,
            category = HabitCategory.valueOf(entity.category),
            currentStreak = entity.currentStreak,
            longestStreak = entity.longestStreak,
            totalCompletions = entity.totalCompletions,
            isActive = entity.isActive,
            startDate = LocalDate.parse(entity.startDate),
            reminderPlan = entity.reminderPlan?.let { parseReminderPlan(it) },
            listId = entity.listId,
            sectionId = entity.sectionId,
            displayOrder = entity.displayOrder
        )
    }

    override fun mapToEntity(domain: Habit): HabitEntity {
        return HabitEntity(
            id = domain.id,
            name = domain.name,
            createdDateTime = domain.createdDateTime.toString(),
            targetFrequency = domain.targetFrequency.name,
            estimatedDurationMinutes = domain.estimatedDurationMinutes,
            category = domain.category.name,
            currentStreak = domain.currentStreak,
            longestStreak = domain.longestStreak,
            totalCompletions = domain.totalCompletions,
            isActive = domain.isActive,
            startDate = domain.startDate.toString(),
            reminderPlan = domain.reminderPlan?.let { serializeReminderPlan(it) },
            listId = domain.listId,
            sectionId = domain.sectionId,
            displayOrder = domain.displayOrder
        )
    }

    private fun parseReminderPlan(json: String): ReminderPlan? {
        return try {
            gson.fromJson(json, ReminderPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeReminderPlan(reminderPlan: ReminderPlan): String {
        return try {
            gson.toJson(reminderPlan)
        } catch (e: Exception) {
            "{}"
        }
    }
}
