package com.elena.autoplanner.domain.usecases.planner

import com.elena.autoplanner.domain.models.PlanningTask
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import java.time.Duration
import java.time.LocalDateTime

class TaskPrioritizer {
    fun calculateRobustScore(
        planningTask: PlanningTask,
        strategy: PrioritizationStrategy,
    ): Double {
        val task = planningTask.task

        return when (strategy) {
            PrioritizationStrategy.BY_URGENCY -> calculateUrgencyScore(task)
            PrioritizationStrategy.BY_IMPORTANCE -> calculateImportanceScore(task)
            PrioritizationStrategy.BY_DURATION -> calculateDurationScore(task)
        }
    }

    private fun calculateUrgencyScore(task: Task): Double {
        var score = 0.0

        task.endDateConf?.dateTime?.let { deadline ->
            val hours = Duration.between(LocalDateTime.now(), deadline).toHours()
            score += when {
                hours < 0 -> 100000.0
                hours <= 8 -> 50000.0
                hours <= 24 -> 25000.0
                hours <= 72 -> 10000.0
                else -> 1000.0
            }
        }

        val priorityMultiplier = when (task.priority) {
            Priority.HIGH -> 2.0
            Priority.MEDIUM -> 1.5
            Priority.LOW -> 1.0
            Priority.NONE -> 0.5
        }

        return score * priorityMultiplier
    }

    private fun calculateImportanceScore(task: Task): Double {

        val priorityScore = when (task.priority) {
            Priority.HIGH -> 100000.0
            Priority.MEDIUM -> 50000.0
            Priority.LOW -> 10000.0
            Priority.NONE -> 1000.0
        }

        val deadlineBoost = task.endDateConf?.dateTime?.let { deadline ->
            val hours = Duration.between(LocalDateTime.now(), deadline).toHours()
            when {
                hours < 0 -> 500.0
                hours <= 24 -> 200.0
                hours <= 72 -> 100.0
                else -> 10.0
            }
        } ?: 0.0

        return priorityScore + deadlineBoost
    }

    private fun calculateDurationScore(task: Task): Double {
        val minutes = task.effectiveDurationMinutes
        val durationScore = 100000.0 / (minutes + 1.0)

        val priorityBoost = when (task.priority) {
            Priority.HIGH -> 500.0
            Priority.MEDIUM -> 200.0
            Priority.LOW -> 100.0
            Priority.NONE -> 0.0
        }

        return durationScore + priorityBoost
    }
}