package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

class GetTaskUseCase(
    private val repository: TaskRepository,
    private val getExpandedTasksUseCase: GetExpandedTasksUseCase = GetExpandedTasksUseCase(
        repository
    ),
) {
    suspend operator fun invoke(taskId: Int): TaskResult<Task> {

        if (taskId == 0) {
            return TaskResult.Error("Cannot fetch generated task instances by ID. Use task instance identifier instead.")
        }

        return repository.getTask(taskId)
    }

    suspend fun getByInstanceIdentifier(instanceIdentifier: String): TaskResult<Task> {
        return try {

            val expandedTasks = getExpandedTasksUseCase(
                startDate = LocalDateTime.now().minusMonths(1),
                endDate = LocalDateTime.now().plusMonths(6)
            ).first()

            val task = expandedTasks.find { task -> task.instanceIdentifier == instanceIdentifier }

            if (task != null) {
                TaskResult.Success(task)
            } else {
                TaskResult.Error("Task instance not found with identifier: $instanceIdentifier")
            }
        } catch (e: Exception) {
            TaskResult.Error("Error fetching task instance: ${e.localizedMessage}")
        }
    }

    suspend fun getTaskSmart(taskId: Int, instanceIdentifier: String?): TaskResult<Task> {
        return if (taskId > 0) {

            invoke(taskId)
        } else if (!instanceIdentifier.isNullOrBlank()) {

            getByInstanceIdentifier(instanceIdentifier)
        } else {
            TaskResult.Error("Invalid task identifier")
        }
    }
}