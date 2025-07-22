package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.first

class GetTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int): TaskResult<Task> {
        if (taskId == 0) {
            return TaskResult.Error("Cannot fetch generated task instances by ID. Use task instance identifier instead.")
        }

        return repository.getTask(taskId)
    }

    suspend fun getTaskByInstanceIdentifier(instanceIdentifier: String): TaskResult<Task> {
        return try {
            // Buscar en todas las tareas expandidas por el identificador de instancia
            when (val tasksResult = repository.getTasks().first()) {
                is TaskResult.Success -> {
                    val foundTask = tasksResult.data.find { task ->
                        task.instanceIdentifier == instanceIdentifier
                    }

                    if (foundTask != null) {
                        TaskResult.Success(foundTask)
                    } else {
                        TaskResult.Error("Task with instance identifier $instanceIdentifier not found")
                    }
                }

                is TaskResult.Error -> tasksResult
            }
        } catch (e: Exception) {
            TaskResult.Error("Error finding task by instance identifier: ${e.message}")
        }
    }
}