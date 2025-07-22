package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult

enum class RepeatTaskDeleteOption {
    THIS_INSTANCE_ONLY,
    THIS_AND_FUTURE,
    ALL_INSTANCES
}

data class DeleteRepeatTaskResult(
    val option: RepeatTaskDeleteOption,
    val confirmed: Boolean,
)

class DeleteRepeatableTaskUseCase(
    private val repository: TaskRepository,
) {

    fun needsDeleteOptions(task: Task): Boolean {
        return task.repeatPlan != null || task.isRepeatedInstance
    }

    suspend fun execute(task: Task, option: RepeatTaskDeleteOption): TaskResult<Unit> {
        return try {
            when (option) {
                RepeatTaskDeleteOption.THIS_INSTANCE_ONLY -> {
                    if (task.isRepeatedInstance) {

                        TaskResult.Success(Unit)
                    } else {

                        stopRepetitionFromDate(task)
                    }
                }

                RepeatTaskDeleteOption.THIS_AND_FUTURE -> {
                    if (task.isRepeatedInstance && task.parentTaskId != null) {

                        stopFutureInstances(task)
                    } else {

                        stopRepetitionFromDate(task)
                    }
                }

                RepeatTaskDeleteOption.ALL_INSTANCES -> {

                    val parentId = task.parentTaskId ?: task.id
                    repository.deleteTask(parentId)
                }
            }
        } catch (e: Exception) {
            TaskResult.Error("Error deleting repeatable task: ${e.message}")
        }
    }

    private suspend fun stopRepetitionFromDate(task: Task): TaskResult<Unit> {
        val taskToUpdate = if (task.isRepeatedInstance && task.parentTaskId != null) {

            when (val parentResult = repository.getTask(task.parentTaskId)) {
                is TaskResult.Success -> parentResult.data
                is TaskResult.Error -> return TaskResult.Error("Could not find parent task")
            }
        } else {
            task
        }

        val currentDate = task.startDateConf.dateTime?.toLocalDate()
        if (currentDate != null) {
            val updatedRepeatPlan = taskToUpdate.repeatPlan?.copy(
                repeatEndDate = currentDate.minusDays(1)
            )

            val updatedTask = Task.from(taskToUpdate)
                .repeatPlan(updatedRepeatPlan)
                .build()

            return when (val saveResult = repository.saveTask(updatedTask)) {
                is TaskResult.Success -> TaskResult.Success(Unit)
                is TaskResult.Error -> TaskResult.Error("Failed to update repeat end date")
            }
        }

        return TaskResult.Error("Could not determine task date")
    }

    private suspend fun stopFutureInstances(task: Task): TaskResult<Unit> {
        if (task.parentTaskId == null) {
            return TaskResult.Error("No parent task found")
        }

        return stopRepetitionFromDate(task)
    }
}