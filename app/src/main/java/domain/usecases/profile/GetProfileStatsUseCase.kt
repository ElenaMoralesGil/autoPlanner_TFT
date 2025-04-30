package com.elena.autoplanner.domain.usecases.profile

import com.elena.autoplanner.domain.models.ProfileStats
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class GetProfileStatsUseCase(private val taskRepository: TaskRepository) {

    suspend operator fun invoke(): TaskResult<ProfileStats> {
        return when (val taskListResult = taskRepository.getTasks().first()) { // Get current list
            is TaskResult.Success -> {
                val tasks = taskListResult.data
                val today = LocalDate.now()

                // Weekly Stats
                val startOfWeek =
                    today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val endOfWeek = startOfWeek.plusDays(6)
                val weeklyTasks = tasks.filter { isInDateRange(it, startOfWeek, endOfWeek) }
                val completedWeekly = weeklyTasks.count { it.isCompleted }
                val successWeekly = calculateSuccessRate(weeklyTasks)

                // Monthly Stats
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                val monthlyTasks = tasks.filter { isInDateRange(it, startOfMonth, endOfMonth) }
                val completedMonthly = monthlyTasks.count { it.isCompleted }
                val successMonthly = calculateSuccessRate(monthlyTasks)

                // Yearly Stats
                val startOfYear = today.withDayOfYear(1)
                val endOfYear = today.with(TemporalAdjusters.lastDayOfYear())
                val yearlyTasks = tasks.filter { isInDateRange(it, startOfYear, endOfYear) }
                val completedYearly = yearlyTasks.count { it.isCompleted }
                val successYearly = calculateSuccessRate(yearlyTasks)

                TaskResult.Success(
                    ProfileStats(
                        completedTasksWeekly = completedWeekly,
                        completedTasksMonthly = completedMonthly,
                        completedTasksYearly = completedYearly,
                        successRateWeekly = successWeekly,
                        successRateMonthly = successMonthly,
                        successRateYearly = successYearly
                    )
                )
            }

            is TaskResult.Error -> TaskResult.Error(
                taskListResult.message,
                taskListResult.exception
            )
        }
    }

    private fun isInDateRange(task: Task, start: LocalDate, end: LocalDate): Boolean {
        // Consider tasks due or completed within the range
        val dueDate =
            task.endDateConf?.dateTime?.toLocalDate() ?: task.startDateConf.dateTime?.toLocalDate()
        // Add completion date check if you track it, otherwise use due date
        return dueDate != null && !dueDate.isBefore(start) && !dueDate.isAfter(end)
    }

    private fun calculateSuccessRate(tasks: List<Task>): Float {
        if (tasks.isEmpty()) return 0f
        val completedOnTime = tasks.count {
            true // TODO(we need to check if the task was completed on time somehow)
        }
        val totalDue =
            tasks.count { it.endDateConf?.dateTime != null || it.startDateConf.dateTime != null }
        return if (totalDue == 0) 0f else (tasks.count { it.isCompleted }
            .toFloat() / totalDue.toFloat()) * 100f

    }
}