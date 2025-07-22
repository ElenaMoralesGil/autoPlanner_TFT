package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import java.time.LocalDateTime

class CompleteRepeatableTaskUseCase(
    private val repository: TaskRepository,
    private val repeatableTaskGenerator: RepeatableTaskGenerator = RepeatableTaskGenerator(),
) {

    suspend fun execute(task: Task): TaskResult<Task> {
        return try {
            if (task.isRepeatedInstance) {
                val completedTask = Task.from(task)
                    .isCompleted(true)
                    .completionDateTime(LocalDateTime.now())
                    .build()

                TaskResult.Success(completedTask)
            } else if (task.repeatPlan != null) {
                val completedTask = Task.from(task)
                    .isCompleted(true)
                    .completionDateTime(LocalDateTime.now())
                    .build()

                repository.saveTask(completedTask)

                generateNextInstance(task)

                TaskResult.Success(completedTask)
            } else {
                val completedTask = Task.from(task)
                    .isCompleted(true)
                    .completionDateTime(LocalDateTime.now())
                    .build()

                repository.saveTask(completedTask)
                TaskResult.Success(completedTask)
            }
        } catch (e: Exception) {
            TaskResult.Error("Error completing repeatable task: ${e.message}")
        }
    }

    private suspend fun generateNextInstance(baseTask: Task) {
        val repeatPlan = baseTask.repeatPlan ?: return
        val baseDateTime = baseTask.startDateConf.dateTime ?: return

        val nextDateTime = repeatableTaskGenerator.getNextOccurrence(baseDateTime, repeatPlan)
        if (nextDateTime != null) {
            val duration = baseTask.durationConf?.totalMinutes ?: 0
            val nextEndDateTime =
                if (duration > 0) nextDateTime.plusMinutes(duration.toLong()) else nextDateTime

            val newStartConf = baseTask.startDateConf.copy(dateTime = nextDateTime)
            val newEndConf = baseTask.endDateConf?.copy(dateTime = nextEndDateTime)

            val nextTask = Task.from(baseTask)
                .id(0)
                .startDateConf(newStartConf)
                .endDateConf(newEndConf)
                .isCompleted(false)
                .completionDateTime(null)
                .build()

            repository.saveTask(nextTask)
        }
    }
}