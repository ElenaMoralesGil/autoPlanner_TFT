package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import java.time.LocalDateTime

class DeleteRepeatableTaskUseCase(
    private val taskRepository: TaskRepository,
    private val repeatableTaskGenerator: RepeatableTaskGenerator,
) {
    /**
     * Checks if a task needs special delete options (for repeatable tasks)
     */
    fun needsDeleteOptions(task: Task): Boolean {
        return (task.repeatPlan != null && task.repeatPlan.isEnabled) || task.isRepeatedInstance
    }

    /**
     * Executes the deletion based on the selected option
     */
    suspend fun execute(task: Task, option: RepeatTaskDeleteOption): TaskResult<Unit> {
        return when {
            task.isRepeatedInstance -> handleInstanceDeletion(task, option)
            task.repeatPlan != null && task.repeatPlan.isEnabled -> handleParentTaskDeletion(
                task,
                option
            )

            else -> deleteThisOccurrence(task.id) // Fallback para tareas normales
        }
    }

    private suspend fun handleInstanceDeletion(
        instance: Task,
        option: RepeatTaskDeleteOption,
    ): TaskResult<Unit> {
        return when (option) {
            RepeatTaskDeleteOption.THIS_INSTANCE_ONLY -> {
                deleteThisOccurrence(instance.id)
            }

            RepeatTaskDeleteOption.THIS_AND_FUTURE -> {
                // Eliminar esta instancia y todas las futuras del mismo padre
                deleteThisOccurrence(instance.id)
                deleteFutureInstancesFromDate(
                    instance.parentTaskId ?: return TaskResult.Error("Instance has no parent"),
                    instance.startDateConf?.dateTime
                        ?: return TaskResult.Error("Instance has no date")
                )
            }

            RepeatTaskDeleteOption.ALL_INSTANCES -> {
                // Eliminar todas las instancias y deshabilitar la repetición en el padre
                deleteAllInstancesAndDisableRepeat(
                    instance.parentTaskId ?: return TaskResult.Error(
                        "Instance has no parent"
                    )
                )
            }
        }
    }

    private suspend fun handleParentTaskDeletion(
        parentTask: Task,
        option: RepeatTaskDeleteOption,
    ): TaskResult<Unit> {
        return when (option) {
            RepeatTaskDeleteOption.THIS_INSTANCE_ONLY -> {
                // Para tarea padre, solo eliminar esta ocurrencia (deshabilitar repetición)
                disableRepeatAndKeepInstances(parentTask)
            }

            RepeatTaskDeleteOption.THIS_AND_FUTURE -> {
                // Eliminar tarea padre y todas las instancias futuras
                deleteThisOccurrence(parentTask.id)
                deleteFutureInstancesFromDate(
                    parentTask.id,
                    parentTask.startDateConf?.dateTime
                        ?: return TaskResult.Error("Parent has no date")
                )
            }

            RepeatTaskDeleteOption.ALL_INSTANCES -> {
                // Eliminar tarea padre y todas sus instancias
                deleteAllInstancesAndParent(parentTask.id)
            }
        }
    }

    /**
     * Deletes this occurrence only of a repeatable task
     */
    suspend fun deleteThisOccurrence(taskId: Int): TaskResult<Unit> {
        return taskRepository.deleteTask(taskId)
    }

    /**
     * Deletes this and future occurrences of a repeatable task
     */
    suspend fun deleteThisAndFutureOccurrences(taskId: Int): TaskResult<Unit> {
        // Obtener la tarea para saber su fecha
        return when (val taskResult = taskRepository.getTask(taskId)) {
            is TaskResult.Success -> {
                val task = taskResult.data
                // Eliminar esta tarea
                deleteThisOccurrence(taskId)

                // Si es una tarea padre, eliminar instancias futuras
                if (task.repeatPlan != null) {
                    deleteFutureInstancesFromDate(
                        taskId,
                        task.startDateConf?.dateTime ?: return TaskResult.Error("Task has no date")
                    )
                } else if (task.isRepeatedInstance) {
                    // Si es instancia, eliminar futuras del padre
                    deleteFutureInstancesFromDate(
                        task.parentTaskId ?: return TaskResult.Error("Instance has no parent"),
                        task.startDateConf?.dateTime
                            ?: return TaskResult.Error("Instance has no date")
                    )
                } else {
                    TaskResult.Success(Unit)
                }
            }

            is TaskResult.Error -> taskResult
        }
    }

    /**
     * Deletes all occurrences of a repeatable task (disables the repeat pattern)
     */
    suspend fun deleteAllOccurrences(taskId: Int): TaskResult<Unit> {
        return taskRepository.deleteRepeatableTaskCompletely(taskId)
    }

    /**
     * Elimina instancias futuras a partir de una fecha específica
     */
    private suspend fun deleteFutureInstancesFromDate(
        parentTaskId: Int,
        fromDate: LocalDateTime,
    ): TaskResult<Unit> {
        return when (val instancesResult =
            taskRepository.getTaskInstancesByParentId(parentTaskId)) {
            is TaskResult.Success -> {
                val futureInstances = instancesResult.data.filter { instance ->
                    val instanceDate = instance.startDateConf?.dateTime
                    instanceDate != null && instanceDate.isAfter(fromDate)
                }

                futureInstances.forEach { instance ->
                    taskRepository.deleteTask(instance.id)
                }

                TaskResult.Success(Unit)
            }

            is TaskResult.Error -> instancesResult
        }
    }

    /**
     * Deshabilita la repetición de una tarea padre pero mantiene las instancias existentes
     */
    private suspend fun disableRepeatAndKeepInstances(parentTask: Task): TaskResult<Unit> {
        val updatedTask = Task.Builder()
            .id(parentTask.id)
            .name(parentTask.name)
            .isCompleted(parentTask.isCompleted)
            .priority(parentTask.priority)
            .startDateConf(parentTask.startDateConf)
            .endDateConf(parentTask.endDateConf)
            .durationConf(parentTask.durationConf)
            .reminderPlan(parentTask.reminderPlan)
            .repeatPlan(parentTask.repeatPlan?.copy(isEnabled = false)) // Deshabilitar repetición
            .subtasks(parentTask.subtasks)
            .scheduledStartDateTime(parentTask.scheduledStartDateTime)
            .scheduledEndDateTime(parentTask.scheduledEndDateTime)
            .completionDateTime(parentTask.completionDateTime)
            .listId(parentTask.listId)
            .sectionId(parentTask.sectionId)
            .displayOrder(parentTask.displayOrder)
            .allowSplitting(parentTask.allowSplitting)
            .isRepeatedInstance(parentTask.isRepeatedInstance)
            .parentTaskId(parentTask.parentTaskId)
            .instanceIdentifier(parentTask.instanceIdentifier)
            .build()

        return when (val result = taskRepository.saveTask(updatedTask)) {
            is TaskResult.Success -> {
                // Limpiar instancias futuras no modificadas
                repeatableTaskGenerator.cleanupInstancesWhenDisabled(parentTask.id)
                TaskResult.Success(Unit)
            }

            is TaskResult.Error -> TaskResult.Error("Failed to disable repeat: ${result.message}")
        }
    }

    /**
     * Elimina todas las instancias y deshabilita la repetición en el padre
     */
    private suspend fun deleteAllInstancesAndDisableRepeat(parentTaskId: Int): TaskResult<Unit> {
        // Primero obtener y eliminar todas las instancias
        when (val instancesResult = taskRepository.getTaskInstancesByParentId(parentTaskId)) {
            is TaskResult.Success -> {
                instancesResult.data.forEach { instance ->
                    taskRepository.deleteTask(instance.id)
                }
            }

            is TaskResult.Error -> return instancesResult
        }

        // Luego deshabilitar la repetición en el padre
        return when (val parentResult = taskRepository.getTask(parentTaskId)) {
            is TaskResult.Success -> {
                disableRepeatAndKeepInstances(parentResult.data)
            }

            is TaskResult.Error -> parentResult
        }
    }

    /**
     * Elimina la tarea padre y todas sus instancias
     */
    private suspend fun deleteAllInstancesAndParent(parentTaskId: Int): TaskResult<Unit> {
        // Primero eliminar todas las instancias
        when (val instancesResult = taskRepository.getTaskInstancesByParentId(parentTaskId)) {
            is TaskResult.Success -> {
                instancesResult.data.forEach { instance ->
                    taskRepository.deleteTask(instance.id)
                }
            }

            is TaskResult.Error -> return instancesResult
        }

        // Luego eliminar la tarea padre
        return deleteThisOccurrence(parentTaskId)
    }

    /**
     * Elimina una instancia repetida usando el instanceIdentifier
     */
    suspend fun deleteInstance(instanceIdentifier: String): TaskResult<Unit> {
        repeatableTaskGenerator.instanceManager.deleteInstanceByIdentifier(instanceIdentifier)
        return TaskResult.Success(Unit)
    }

    suspend fun deleteFutureInstances(instanceIdentifier: String): TaskResult<Unit> {
        val instance = taskRepository.getTaskByInstanceIdentifier(instanceIdentifier)
            ?: return TaskResult.Error("Instance not found")
        val parentId = instance.parentTaskId ?: return TaskResult.Error("Instance has no parent")
        val startDate =
            instance.startDateConf?.dateTime ?: return TaskResult.Error("Instance has no date")
        // Elimina todas las instancias futuras con el mismo parentId y fecha >= startDate
        // Usar el manager para eliminar instancias futuras
        repeatableTaskGenerator.instanceManager.updateFutureInstances(parentId, startDate)
        return TaskResult.Success(Unit)
    }

    suspend fun deleteAllInstances(instanceIdentifier: String): TaskResult<Unit> {
        val instance = taskRepository.getTaskByInstanceIdentifier(instanceIdentifier)
            ?: return TaskResult.Error("Instance not found")
        val parentId = instance.parentTaskId ?: return TaskResult.Error("Instance has no parent")
        return deleteAllInstancesAndDisableRepeat(parentId)
    }
}
