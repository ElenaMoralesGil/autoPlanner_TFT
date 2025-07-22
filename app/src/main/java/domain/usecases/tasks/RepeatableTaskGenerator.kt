package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class RepeatableTaskGenerator(
    private val taskRepository: TaskRepository,
) {

    /**
     * Genera instancias de una tarea repetible cuando se crea inicialmente
     */
    suspend fun generateInstancesForNewTask(parentTask: Task): TaskResult<List<Int>> {
        if (parentTask.repeatPlan == null || !parentTask.repeatPlan.isEnabled) {
            return TaskResult.Success(emptyList())
        }

        val instances = generateInstances(parentTask)
        val savedInstanceIds = mutableListOf<Int>()

        try {
            instances.forEach { instance ->
                when (val result = taskRepository.saveTask(instance)) {
                    is TaskResult.Success -> savedInstanceIds.add(result.data)
                    is TaskResult.Error -> {
                        // Si hay error, eliminar las instancias ya creadas
                        savedInstanceIds.forEach { instanceId ->
                            taskRepository.deleteTask(instanceId)
                        }
                        return TaskResult.Error("Error creating instances: ${result.message}")
                    }
                }
            }
            return TaskResult.Success(savedInstanceIds)
        } catch (e: Exception) {
            // Cleanup en caso de excepción
            savedInstanceIds.forEach { instanceId ->
                try {
                    taskRepository.deleteTask(instanceId)
                } catch (_: Exception) {
                    // Log pero no propagar el error de cleanup
                }
            }
            return TaskResult.Error("Exception generating instances: ${e.message}")
        }
    }

    /**
     * Genera las instancias individuales basándose en el RepeatPlan
     */
    private fun generateInstances(parentTask: Task): List<Task> {
        val repeatPlan = parentTask.repeatPlan ?: return emptyList()
        val startDate = parentTask.startDateConf?.dateTime ?: return emptyList()

        val instances = mutableListOf<Task>()
        val endDate = calculateEndDate(startDate, repeatPlan)
        val maxInstances = 365 // Máximo 1 año de instancias como mencionaste

        var currentDate = startDate
        var instanceCount = 0
        var instanceNumber = 1

        while (currentDate.isBefore(endDate) && instanceCount < maxInstances) {
            // Crear la instancia para esta fecha
            val instanceIdentifier = "${parentTask.id}_${currentDate.toLocalDate()}_$instanceNumber"

            val instance = Task.Builder()
                .id(0) // Se asignará uno nuevo al guardar
                .name(parentTask.name)
                .isCompleted(false)
                .priority(parentTask.priority)
                .isRepeatedInstance(true)
                .parentTaskId(parentTask.id)
                .instanceIdentifier(instanceIdentifier)
                .startDateConf(
                    TimePlanning(
                        dateTime = currentDate,
                        dayPeriod = parentTask.startDateConf?.dayPeriod ?: DayPeriod.NONE
                    )
                )
                .endDateConf(parentTask.endDateConf?.let { endConf ->
                    val duration = ChronoUnit.MINUTES.between(startDate, endConf.dateTime!!)
                    TimePlanning(
                        dateTime = currentDate.plusMinutes(duration),
                        dayPeriod = endConf.dayPeriod
                    )
                })
                .durationConf(parentTask.durationConf)
                .listId(parentTask.listId)
                .sectionId(parentTask.sectionId)
                .reminderPlan(parentTask.reminderPlan)
                .repeatPlan(null) // Las instancias no tienen plan de repetición
                .completionDateTime(null)
                .scheduledStartDateTime(null)
                .scheduledEndDateTime(null)
                .build()

            instances.add(instance)
            instanceCount++
            instanceNumber++

            // Calcular la siguiente fecha según el tipo de repetición
            currentDate = calculateNextDate(currentDate, repeatPlan)
        }

        return instances
    }

    /**
     * Calcula la fecha de finalización para la generación de instancias
     */
    private fun calculateEndDate(startDate: LocalDateTime, repeatPlan: RepeatPlan): LocalDateTime {
        return when {
            repeatPlan.endDate != null -> repeatPlan.endDate
            repeatPlan.maxOccurrences != null -> {
                // Calcular fecha aproximada basada en el número máximo de ocurrencias
                val intervalDays = when (repeatPlan.frequency) {
                    RepeatFrequency.DAILY -> 1L
                    RepeatFrequency.WEEKLY -> 7L
                    RepeatFrequency.MONTHLY -> 30L
                    RepeatFrequency.YEARLY -> 365L
                }
                startDate.plusDays(intervalDays * repeatPlan.maxOccurrences)
            }

            else -> startDate.plusYears(1) // Por defecto, generar para 1 año
        }
    }

    /**
     * Calcula la siguiente fecha de repetición
     */
    fun calculateNextDate(currentDate: LocalDateTime, repeatPlan: RepeatPlan): LocalDateTime {
        val interval = repeatPlan.intervalNew

        return when (repeatPlan.frequency) {
            RepeatFrequency.DAILY -> currentDate.plusDays(interval.toLong())
            RepeatFrequency.WEEKLY -> currentDate.plusWeeks(interval.toLong())
            RepeatFrequency.MONTHLY -> currentDate.plusMonths(interval.toLong())
            RepeatFrequency.YEARLY -> currentDate.plusYears(interval.toLong())
        }
    }

    /**
     * Regenera instancias futuras cuando se modifica una tarea repetible
     */
    suspend fun regenerateInstancesForUpdatedTask(updatedParentTask: Task): TaskResult<Unit> {
        if (updatedParentTask.repeatPlan == null || !updatedParentTask.repeatPlan.isEnabled) {
            // Si se deshabilitó la repetición, eliminar instancias futuras
            return deleteFutureInstances(updatedParentTask.id)
        }

        // Eliminar instancias futuras no completadas
        deleteFutureInstances(updatedParentTask.id)

        // Generar nuevas instancias con la configuración actualizada
        generateInstancesForNewTask(updatedParentTask)

        return TaskResult.Success(Unit)
    }

    /**
     * Elimina instancias futuras no completadas de una tarea repetible
     */
    private suspend fun deleteFutureInstances(parentTaskId: Int): TaskResult<Unit> {
        return taskRepository.deleteFutureInstancesByParentId(parentTaskId)
    }

    /**
     * Verifica si una instancia ha sido modificada individualmente
     */
    private fun hasIndividualModifications(instance: Task, parentTask: Task): Boolean {
        // Verificar si hay modificaciones en campos clave que indicarían personalización
        return instance.name != parentTask.name ||
                instance.durationConf != parentTask.durationConf ||
                instance.reminderPlan != parentTask.reminderPlan ||
                instance.priority != parentTask.priority
    }

    /**
     * Actualiza una instancia con cambios del padre, preservando modificaciones individuales
     */
    private fun updateInstanceWithParentChanges(instance: Task, parentTask: Task): Task {
        return Task.Builder()
            .id(instance.id)
            .name(parentTask.name) // Actualizar nombre desde el padre
            .isCompleted(instance.isCompleted) // Preservar estado de completado
            .priority(parentTask.priority) // Actualizar prioridad desde el padre
            .startDateConf(instance.startDateConf) // Preservar fecha específica de la instancia
            .endDateConf(instance.endDateConf) // Preservar fecha específica de la instancia
            .durationConf(parentTask.durationConf) // Actualizar duración desde el padre
            .reminderPlan(parentTask.reminderPlan) // Actualizar recordatorio desde el padre
            .repeatPlan(null) // Las instancias no tienen plan de repetición
            .subtasks(instance.subtasks) // Preservar subtareas de la instancia
            .scheduledStartDateTime(instance.scheduledStartDateTime) // Preservar programación
            .scheduledEndDateTime(instance.scheduledEndDateTime) // Preservar programación
            .completionDateTime(instance.completionDateTime) // Preservar fecha de completado
            .listId(parentTask.listId) // Actualizar lista desde el padre
            .sectionId(parentTask.sectionId) // Actualizar sección desde el padre
            .displayOrder(instance.displayOrder) // Preservar orden
            .allowSplitting(parentTask.allowSplitting) // Actualizar desde el padre
            .isRepeatedInstance(true) // Mantener como instancia
            .parentTaskId(parentTask.id) // Mantener referencia al padre
            .instanceIdentifier(instance.instanceIdentifier) // Preservar identificador
            .build()
    }

    /**
     * Genera la siguiente instancia de una tarea repetible después de completar una
     */
    suspend fun generateNextInstanceAfterCompletion(completedTask: Task): TaskResult<Task?> {
        // Solo generar si es una tarea padre con repetición habilitada
        if (completedTask.isRepeatedInstance || completedTask.repeatPlan == null || !completedTask.repeatPlan.isEnabled) {
            return TaskResult.Success(null)
        }

        val repeatPlan = completedTask.repeatPlan
        val baseDateTime = completedTask.startDateConf?.dateTime ?: return TaskResult.Success(null)

        // Verificar si hemos alcanzado el límite de ocurrencias
        if (repeatPlan.maxOccurrences != null) {
            val existingInstancesResult =
                taskRepository.getTaskInstancesByParentId(completedTask.id)
            if (existingInstancesResult is TaskResult.Success) {
                val instanceCount =
                    existingInstancesResult.data.size + 1 // +1 por la tarea padre completada
                if (instanceCount >= repeatPlan.maxOccurrences) {
                    return TaskResult.Success(null) // No generar más instancias
                }
            }
        }

        // Verificar si hemos alcanzado la fecha límite
        val nextDateTime = calculateNextDate(baseDateTime, repeatPlan)
        if (repeatPlan.endDate != null && nextDateTime.isAfter(repeatPlan.endDate)) {
            return TaskResult.Success(null) // No generar más instancias
        }

        // Generar la siguiente instancia
        val duration = completedTask.durationConf?.totalMinutes ?: 0
        val nextEndDateTime =
            if (duration > 0) nextDateTime.plusMinutes(duration.toLong()) else nextDateTime

        val newStartConf = TimePlanning(
            dateTime = nextDateTime,
            dayPeriod = completedTask.startDateConf?.dayPeriod ?: DayPeriod.NONE
        )
        val newEndConf = completedTask.endDateConf?.let {
            TimePlanning(
                dateTime = nextEndDateTime,
                dayPeriod = it.dayPeriod
            )
        }

        val nextTask = Task.Builder()
            .id(0) // El repositorio asignará un nuevo ID automáticamente
            .name(completedTask.name)
            .isCompleted(false)
            .priority(completedTask.priority)
            .startDateConf(newStartConf)
            .endDateConf(newEndConf)
            .durationConf(completedTask.durationConf)
            .reminderPlan(completedTask.reminderPlan)
            .repeatPlan(null) // Las instancias no tienen plan de repetición
            .subtasks(completedTask.subtasks)
            .scheduledStartDateTime(null)
            .scheduledEndDateTime(null)
            .completionDateTime(null)
            .listId(completedTask.listId)
            .sectionId(completedTask.sectionId)
            .displayOrder(completedTask.displayOrder)
            .allowSplitting(completedTask.allowSplitting)
            .isRepeatedInstance(true) // Marcar como instancia repetida
            .parentTaskId(completedTask.id) // Referenciar la tarea padre
            .instanceIdentifier("${completedTask.id}_${nextDateTime.toLocalDate()}_next")
            .build()

        return when (val result = taskRepository.saveTask(nextTask)) {
            is TaskResult.Success -> TaskResult.Success(nextTask.copy(id = result.data))
            is TaskResult.Error -> TaskResult.Error("Failed to generate next instance: ${result.message}")
        }
    }

    /**
     * Limpia instancias cuando se deshabilita la repetición, preservando las que han sido modificadas
     */
    suspend fun cleanupInstancesWhenDisabled(parentTaskId: Int): TaskResult<Unit> {
        return when (val instancesResult =
            taskRepository.getTaskInstancesByParentId(parentTaskId)) {
            is TaskResult.Success -> {
                val instances = instancesResult.data
                val currentDate = LocalDateTime.now()

                // Solo eliminar instancias futuras que NO han sido modificadas individualmente
                instances.forEach { instance ->
                    val instanceDate = instance.startDateConf?.dateTime
                    val isFuture = instanceDate != null && instanceDate.isAfter(currentDate)
                    val isNotCompleted = !instance.isCompleted
                    val hasNoIndividualMods = !hasIndividualModifications(
                        instance,
                        instance
                    ) // Usar la instancia como referencia base

                    if (isFuture && isNotCompleted && hasNoIndividualMods) {
                        taskRepository.deleteTask(instance.id)
                    }
                    // Preservar instancias que han sido modificadas individualmente o ya completadas
                }

                TaskResult.Success(Unit)
            }

            is TaskResult.Error -> instancesResult
        }
    }
}