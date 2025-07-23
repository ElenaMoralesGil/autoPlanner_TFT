package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult

/**
 * Use case para guardar tareas repetibles, manejando la generación de instancias
 */
class SaveRepeatableTaskUseCase(
    private val taskRepository: TaskRepository,
    private val repeatableTaskGenerator: RepeatableTaskGenerator,
) {

    /**
     * Guarda una tarea repetible, creando instancias si es necesario
     */
    suspend fun execute(task: Task, isNewTask: Boolean = false): TaskResult<Task> {
        return try {
            // Primero guardar la tarea padre
            when (val saveResult = taskRepository.saveTask(task)) {
                is TaskResult.Success -> {
                    val savedTaskId = saveResult.data
                    val savedTask = task.copy(id = savedTaskId)

                    // Si es una nueva tarea con repetición, generar instancias
                    if (isNewTask) {
                        when (val instancesResult =
                            repeatableTaskGenerator.generateInstancesForNewTask(savedTask)) {
                            is TaskResult.Success -> {
                                TaskResult.Success(savedTask)
                            }

                            is TaskResult.Error -> {
                                // Si falló la generación de instancias, mantener la tarea pero sin instancias
                                TaskResult.Success(savedTask)
                            }
                        }
                    } else if (!isNewTask) {
                        // Si es una actualización y tiene repetición, regenerar instancias
                        repeatableTaskGenerator.regenerateInstancesForUpdatedTask(savedTask)
                        TaskResult.Success(savedTask)
                    } else if (!isNewTask && (task.repeatPlan == null)) {
                        // Si se deshabilitó la repetición, limpiar instancias
                        repeatableTaskGenerator.cleanupInstancesWhenDisabled(savedTaskId)
                        TaskResult.Success(savedTask)
                    } else {
                        TaskResult.Success(savedTask)
                    }
                }

                is TaskResult.Error -> saveResult
            }
        } catch (e: Exception) {
            TaskResult.Error("Error saving repeatable task: ${e.message}")
        }
    }

    /**
     * Actualiza una tarea existente, manejando cambios en la repetición
     */
    suspend fun updateTask(updatedTask: Task): TaskResult<Task> {
        return execute(updatedTask, isNewTask = false)
    }

    /**
     * Crea una nueva tarea repetible
     */
    suspend fun createTask(newTask: Task): TaskResult<Task> {
        return execute(newTask, isNewTask = true)
    }
}
