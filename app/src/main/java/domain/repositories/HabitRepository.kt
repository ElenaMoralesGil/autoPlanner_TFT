package com.elena.autoplanner.domain.repositories

import com.elena.autoplanner.domain.models.Habit
import com.elena.autoplanner.domain.models.HabitCompletion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    // Operaciones de hábitos
    suspend fun insertHabit(habit: Habit): Result<Unit>
    suspend fun updateHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: Int): Result<Unit>
    suspend fun getHabits(): Flow<List<Habit>>
    suspend fun getHabitById(habitId: Int): Habit?
    suspend fun getActiveHabits(): Flow<List<Habit>>

    // Operaciones de completaciones (dentro del mismo repositorio)
    suspend fun markHabitComplete(
        habitId: Int,
        date: LocalDate,
        notes: String? = null,
    ): Result<Unit>

    suspend fun markHabitIncomplete(habitId: Int, date: LocalDate): Result<Unit>
    suspend fun getHabitCompletions(
        habitId: Int,
        dateRange: ClosedRange<LocalDate>,
    ): List<HabitCompletion>

    suspend fun getCompletionsForDate(date: LocalDate): Flow<List<HabitCompletion>>
    suspend fun updateHabitStreaks(habitId: Int): Result<Unit>
}
