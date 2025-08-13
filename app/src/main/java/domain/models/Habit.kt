package com.elena.autoplanner.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

data class Habit(
    override val id: Int = 0,
    override val name: String,
    override val createdDateTime: LocalDateTime = LocalDateTime.now(),
    override val type: PlannerItemType = PlannerItemType.HABIT,
    val targetFrequency: HabitFrequency,
    val estimatedDurationMinutes: Int? = null,
    val category: HabitCategory = HabitCategory.PERSONAL,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val isActive: Boolean = true,
    val startDate: LocalDate = LocalDate.now(),
    val reminderPlan: ReminderPlan? = null,
    val listId: Long? = null,
    val sectionId: Long? = null,
    val displayOrder: Int = 0,
    val firestoreId: String? = null, // For Firebase sync
    val userId: String? = null, // For Firebase sync
) : PlannerItem

enum class HabitFrequency { DAILY, WEEKLY, CUSTOM }
enum class HabitCategory { HEALTH, PRODUCTIVITY, PERSONAL, LEARNING, SOCIAL, FITNESS, MINDFULNESS }