package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult

class SaveTaskUseCase(
    private val taskRepository: TaskRepository,
    private val repeatableTaskGenerator: RepeatableTaskGenerator,
) {

    suspend operator fun invoke(task: Task): TaskResult<Int> {
        // Validar la tarea antes de guardar
        try {
            task.validate()
        } catch (e: Exception) {
            return TaskResult.Error("Task validation failed: ${e.message}")
        }

        val isNewTask = task.id == 0
        val hadRepeatPlan = if (!isNewTask) {
            // Para tareas existentes, verificar si tenía plan de repetición antes
            when (val existingResult = taskRepository.getTask(task.id)) {
                is TaskResult.Success -> existingResult.data.repeatPlan?.isEnabled == true
                is TaskResult.Error -> false
            }
        } else false

        // Guardar la tarea principal
        val saveResult = taskRepository.saveTask(task)

        return when (saveResult) {
            is TaskResult.Success -> {
                val savedTaskId = saveResult.data
                val taskWithId = Task.Builder()
                    .id(savedTaskId)
                    .name(task.name)
                    .isCompleted(task.isCompleted)
                    .priority(task.priority)
                    .startDateConf(task.startDateConf)
                    .endDateConf(task.endDateConf)
                    .durationConf(task.durationConf)
                    .reminderPlan(task.reminderPlan)
                    .repeatPlan(task.repeatPlan)
                    .subtasks(task.subtasks)
                    .scheduledStartDateTime(task.scheduledStartDateTime)
                    .scheduledEndDateTime(task.scheduledEndDateTime)
                    .completionDateTime(task.completionDateTime)
                    .listId(task.listId)
                    .sectionId(task.sectionId)
                    .displayOrder(task.displayOrder)
                    .allowSplitting(task.allowSplitting)
                    .isRepeatedInstance(task.isRepeatedInstance)
                    .parentTaskId(task.parentTaskId)
                    .instanceIdentifier(task.instanceIdentifier)
                    .build()

                // Manejar la generación de instancias para tareas repetibles
                if (taskWithId.repeatPlan?.isEnabled == true) {
                    if (isNewTask) {
                        // Nueva tarea repetible: generar todas las instancias
                        when (val generateResult =
                            repeatableTaskGenerator.generateInstancesForNewTask(taskWithId)) {
                            is TaskResult.Success -> {
                                TaskResult.Success(savedTaskId)
                            }

                            is TaskResult.Error -> {
                                taskRepository.deleteTask(savedTaskId)
                                TaskResult.Error("Error generating task instances: ${generateResult.message}")
                            }
                        }
                    } else {
                        // Tarea existente modificada: regenerar instancias con la nueva configuración
                        if (hadRepeatPlan) {
                            // Ya tenía repetición, regenerar instancias futuras
                            repeatableTaskGenerator.regenerateInstancesForUpdatedTask(taskWithId)
                        } else {
                            // No tenía repetición antes, generar instancias nuevas
                            repeatableTaskGenerator.generateInstancesForNewTask(taskWithId)
                        }
                        TaskResult.Success(savedTaskId)
                    }
                } else {
                    // Tarea no repetible o repetición deshabilitada
                    if (!isNewTask && hadRepeatPlan) {
                        // Se deshabilitó la repetición, eliminar solo instancias futuras no modificadas
                        repeatableTaskGenerator.cleanupInstancesWhenDisabled(savedTaskId)
                    }
                    TaskResult.Success(savedTaskId)
                }
            }

            is TaskResult.Error -> saveResult
        }
    }
}