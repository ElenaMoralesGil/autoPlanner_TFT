package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import java.time.LocalDateTime

class CompleteRepeatableTaskUseCase(
    private val taskRepository: TaskRepository,
    private val repeatableTaskGenerator: RepeatableTaskGenerator,
) {

    suspend fun execute(task: Task): TaskResult<Task> {
        return try {
            if (task.isRepeatedInstance) {
                // Para instancias repetidas, solo marcar como completada, NO crear nueva instancia
                handleInstanceCompletion(task)
            } else if (task.repeatPlan != null) {
                // Para tareas padre con repetición, marcar como completada Y generar siguiente instancia
                handleParentTaskCompletion(task)
            } else {
                // Para tareas normales sin repetición
                handleNormalTaskCompletion(task)
            }
        } catch (e: Exception) {
            TaskResult.Error("Error completing repeatable task: ${e.message}")
        }
    }

    private suspend fun handleInstanceCompletion(task: Task): TaskResult<Task> {
        val completedTask = Task.Builder()
            .id(task.id)
            .name(task.name)
            .isCompleted(true)
            .priority(task.priority)
            .startDateConf(task.startDateConf)
            .endDateConf(task.endDateConf)
            .durationConf(task.durationConf)
            .reminderPlan(task.reminderPlan)
            .repeatPlan(task.repeatPlan)
            .subtasks(task.subtasks)
            .scheduledStartDateTime(task.scheduledStartDateTime)
            .scheduledEndDateTime(task.scheduledEndDateTime)
            .completionDateTime(LocalDateTime.now())
            .listId(task.listId)
            .sectionId(task.sectionId)
            .displayOrder(task.displayOrder)
            .allowSplitting(task.allowSplitting)
            .isRepeatedInstance(task.isRepeatedInstance)
            .parentTaskId(task.parentTaskId)
            .instanceIdentifier(task.instanceIdentifier)
            .build()

        return when (val result = taskRepository.saveTask(completedTask)) {
            is TaskResult.Success -> TaskResult.Success(completedTask)
            is TaskResult.Error -> TaskResult.Error("Failed to save completed instance: ${result.message}")
        }
    }

    private suspend fun handleParentTaskCompletion(task: Task): TaskResult<Task> {
        // Marcar la tarea padre como completada
        val completedTask = Task.Builder()
            .id(task.id)
            .name(task.name)
            .isCompleted(true)
            .priority(task.priority)
            .startDateConf(task.startDateConf)
            .endDateConf(task.endDateConf)
            .durationConf(task.durationConf)
            .reminderPlan(task.reminderPlan)
            .repeatPlan(task.repeatPlan)
            .subtasks(task.subtasks)
            .scheduledStartDateTime(task.scheduledStartDateTime)
            .scheduledEndDateTime(task.scheduledEndDateTime)
            .completionDateTime(LocalDateTime.now())
            .listId(task.listId)
            .sectionId(task.sectionId)
            .displayOrder(task.displayOrder)
            .allowSplitting(task.allowSplitting)
            .isRepeatedInstance(task.isRepeatedInstance)
            .parentTaskId(task.parentTaskId)
            .instanceIdentifier(task.instanceIdentifier)
            .build()

        // Guardar la tarea completada
        when (val saveResult = taskRepository.saveTask(completedTask)) {
            is TaskResult.Success -> {
                // Intentar generar la siguiente instancia
                when (val nextInstanceResult =
                    repeatableTaskGenerator.generateNextInstanceAfterCompletion(task)) {
                    is TaskResult.Success -> {
                        // Éxito independientemente de si se generó una nueva instancia o no
                        return TaskResult.Success(completedTask)
                    }

                    is TaskResult.Error -> {
                        // Log el error pero no fallar la operación completa
                        // La tarea se completó exitosamente aunque no se pudo generar la siguiente instancia
                        return TaskResult.Success(completedTask)
                    }
                }
            }

            is TaskResult.Error -> return TaskResult.Error("Failed to save completed parent task: ${saveResult.message}")
        }
    }

    private suspend fun handleNormalTaskCompletion(task: Task): TaskResult<Task> {
        val completedTask = Task.Builder()
            .id(task.id)
            .name(task.name)
            .isCompleted(true)
            .priority(task.priority)
            .startDateConf(task.startDateConf)
            .endDateConf(task.endDateConf)
            .durationConf(task.durationConf)
            .reminderPlan(task.reminderPlan)
            .repeatPlan(task.repeatPlan)
            .subtasks(task.subtasks)
            .scheduledStartDateTime(task.scheduledStartDateTime)
            .scheduledEndDateTime(task.scheduledEndDateTime)
            .completionDateTime(LocalDateTime.now())
            .listId(task.listId)
            .sectionId(task.sectionId)
            .displayOrder(task.displayOrder)
            .allowSplitting(task.allowSplitting)
            .isRepeatedInstance(task.isRepeatedInstance)
            .parentTaskId(task.parentTaskId)
            .instanceIdentifier(task.instanceIdentifier)
            .build()

        return when (val result = taskRepository.saveTask(completedTask)) {
            is TaskResult.Success -> TaskResult.Success(completedTask)
            is TaskResult.Error -> TaskResult.Error("Failed to save completed task: ${result.message}")
        }
    }
}