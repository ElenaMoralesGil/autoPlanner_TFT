package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.Priority
import java.time.LocalDate

class OverdueTaskHandler {
    fun handleOverdueTasks(
        context: PlanningContext,
        strategy: OverdueTaskHandling,
        today: LocalDate,
        scopeStartDate: LocalDate,
        scopeEndDate: LocalDate,
    ) {
        val overdueTaskIds = context.planningTaskMap.values
            .filter { it.task.isExpired() && !context.placedTaskIds.contains(it.id) }
            .map { it.id }

        if (overdueTaskIds.isEmpty()) return
        Log.d(
            "OverdueTaskHandler",
            "Handling ${overdueTaskIds.size} overdue tasks with strategy: $strategy"
        )

        overdueTaskIds.forEach { taskId ->
            val planningTask = context.planningTaskMap[taskId] ?: return@forEach
            if (context.placedTaskIds.contains(taskId)) return@forEach

            when (strategy) {
                OverdueTaskHandling.POSTPONE_TO_TOMORROW -> {
                    context.addPostponedTask(planningTask.task)
                }

                OverdueTaskHandling.USER_REVIEW_REQUIRED -> {
                    context.addExpiredForManualResolution(planningTask.task)
                }

                OverdueTaskHandling.NEXT_AVAILABLE -> {
                    handleAddToFreeTime(
                        context,
                        overdueTaskIds,
                        today,
                        scopeStartDate,
                        scopeEndDate
                    )
                }
            }
        }
    }

    private fun handleAddToFreeTime(
        context: PlanningContext,
        overdueTaskIds: List<Int>,
        today: LocalDate,
        scopeStartDate: LocalDate,
        scopeEndDate: LocalDate,
    ) {
        val availableDays = mutableListOf<LocalDate>()
        var currentDate = maxOf(today, scopeStartDate)
        while (!currentDate.isAfter(scopeEndDate)) {
            availableDays.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        if (availableDays.isEmpty()) {
            overdueTaskIds.forEach { taskId ->
                context.planningTaskMap[taskId]?.let { planningTask ->
                    context.addExpiredForManualResolution(planningTask.task)
                }
            }
            return
        }

        val sortedOverdueTasks = overdueTaskIds
            .mapNotNull { context.planningTaskMap[it] }
            .sortedByDescending {
                when (it.task.priority) {
                    Priority.HIGH -> 3
                    Priority.MEDIUM -> 2
                    Priority.LOW -> 1
                    Priority.NONE -> 0
                }
            }

        var dayIndex = 0
        sortedOverdueTasks.forEach { planningTask ->
            val taskId = planningTask.id
            val targetDate = availableDays[dayIndex % availableDays.size]

            planningTask.flags.isOverdue = true
            planningTask.flags.constraintDate = targetDate

            Log.d("OverdueTaskHandler", "Task $taskId marked for placement on $targetDate")

            // Move to next day for distribution
            dayIndex++
        }
    }
}