package com.elena.autoplanner.domain.usecases.tasks

import android.util.Log
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.RepeatFrequency
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class GetExpandedTasksUseCase(
    private val taskRepository: TaskRepository,
) {

    /**
     * Obtiene todas las tareas expandidas, incluyendo instancias de tareas repetibles
     * generadas dinámicamente para un rango de fechas, con paginación
     */
    operator fun invoke(
        startDate: LocalDate = LocalDate.now().minusMonths(1),
        endDate: LocalDate = LocalDate.now().plusYears(1),
        limit: Int = 50,
        offset: Int = 0,
    ): Flow<TaskResult<List<Task>>> {
        return taskRepository.getTasks().map { result ->
            when (result) {
                is TaskResult.Success -> {
                    val expandedTasks =
                        filterAndOrganizeTasks(result.data, startDate, endDate, limit, offset)

                    // Guardar instancias generadas en la base de datos solo si no existen
                    val tasksToInsert = expandedTasks.filter { task ->
                        task.isRepeatedInstance && task.parentTaskId != null &&
                                run {
                                    val instanceIdentifier = task.instanceIdentifier
                                        ?: "${task.parentTaskId}_${task.startDateConf?.dateTime?.toLocalDate()}"
                                    taskRepository.getTaskByInstanceIdentifier(instanceIdentifier) == null
                                }
                    }
                    if (tasksToInsert.isNotEmpty()) {
                        kotlinx.coroutines.runBlocking {
                            withContext(Dispatchers.IO) {
                                tasksToInsert.forEach { task ->
                                    val instanceIdentifier = task.instanceIdentifier
                                        ?: "${task.parentTaskId}_${task.startDateConf?.dateTime?.toLocalDate()}"
                                    val scheduledDateTime =
                                        task.startDateConf?.dateTime ?: LocalDateTime.now()
                                    val repeatableInstance =
                                        com.elena.autoplanner.domain.models.RepeatableTaskInstance(
                                            parentTaskId = task.parentTaskId!!,
                                            instanceIdentifier = instanceIdentifier,
                                            scheduledDateTime = scheduledDateTime,
                                            isCompleted = task.isCompleted,
                                            isDeleted = false
                                        )
                                    taskRepository.insertRepeatableInstance(repeatableInstance)
                                }
                            }
                        }
                    }
                    TaskResult.Success(expandedTasks)
                }
                is TaskResult.Error -> {
                    Log.e("GetExpandedTasksUseCase", "Error fetching tasks: ${result.message}")
                    result
                }
            }
        }.catch { error ->
            Log.e("GetExpandedTasksUseCase", "Exception in expanded tasks flow", error)
            emit(TaskResult.Error("Error getting expanded tasks: ${error.message}"))
        }
    }

    /**
     * Filtra y organiza las tareas para evitar duplicados y manejar correctamente
     * las tareas repetibles con generación dinámica
     */
    private fun filterAndOrganizeTasks(
        allTasks: List<Task>,
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int = 50,
        offset: Int = 0,
    ): List<Task> {
        Log.d(
            "GetExpandedTasksUseCase",
            "filterAndOrganizeTasks - Total tasks: ${allTasks.size}, range: $startDate to $endDate, limit: $limit, offset: $offset"
        )

        val parentTasks = mutableListOf<Task>()
        val instanceTasks = mutableListOf<Task>()

        // Separar tareas padre de instancias
        allTasks.forEach { task ->
            if (task.isRepeatedInstance) {
                instanceTasks.add(task)
                Log.d("GetExpandedTasksUseCase", "Found instance task: ${task.name}")
            } else {
                parentTasks.add(task)
                Log.d(
                    "GetExpandedTasksUseCase",
                    "Found parent task: ${task.name}, hasRepeat: ${task.repeatPlan != null}, "
                )
            }
        }

        val resultTasks = mutableListOf<Task>()

        // Procesar tareas padre
        parentTasks.forEach { parentTask ->
            // CORRECCIÓN COMPLETA: Verificar TODOS los posibles indicadores de repetición
            val hasRepeatConfig = parentTask.repeatPlan != null && (
                    // Sistema NUEVO
                            parentTask.repeatPlan.frequency != RepeatFrequency.DAILY ||
                            parentTask.repeatPlan.intervalNew > 1 ||
                            parentTask.repeatPlan.endDate != null ||
                            parentTask.repeatPlan.maxOccurrences != null ||

                            // Sistema ANTIGUO - ESTOS SON LOS QUE FALTABAN
                            parentTask.repeatPlan.selectedDays.isNotEmpty() ||  // Días específicos (lunes, sábados, etc.)
                            parentTask.repeatPlan.frequencyType != FrequencyType.NONE ||  // Tipo de frecuencia
                            (parentTask.repeatPlan.interval != null && parentTask.repeatPlan.interval > 0) ||  // Intervalo personalizado
                            parentTask.repeatPlan.daysOfMonth.isNotEmpty() ||  // Días específicos del mes
                            parentTask.repeatPlan.monthsOfYear.isNotEmpty() ||  // Meses específicos
                            parentTask.repeatPlan.ordinalsOfWeekdays.isNotEmpty() ||  // Ordinales de días de semana
                            parentTask.repeatPlan.repeatEndDate != null ||  // Fecha de fin
                            (parentTask.repeatPlan.repeatOccurrences != null && parentTask.repeatPlan.repeatOccurrences > 0)  // Número de ocurrencias
                    )

            if (hasRepeatConfig) {
                Log.d("GetExpandedTasksUseCase", "Processing repeatable task: ${parentTask.name}")
                Log.d(
                    "GetExpandedTasksUseCase",
                    ", selectedDays: ${parentTask.repeatPlan.selectedDays}, frequencyType: ${parentTask.repeatPlan.frequencyType}"
                )

                // Para tareas repetibles, generar instancias dinámicamente
                val generatedInstances = generateDynamicInstances(parentTask, startDate, endDate)
                // PAGINACIÓN: aplicar offset y limit
                val pagedGeneratedInstances = generatedInstances.drop(offset).take(limit)

                Log.d(
                    "GetExpandedTasksUseCase",
                    "Generated ${generatedInstances.size} instances for ${parentTask.name}"
                )

                // Filtrar instancias existentes que coincidan con las generadas para evitar duplicados
                val existingInstancesForParent =
                    instanceTasks.filter { it.parentTaskId == parentTask.id }
                Log.d(
                    "GetExpandedTasksUseCase",
                    "Found ${existingInstancesForParent.size} existing instances for ${parentTask.name}"
                )

                // Combinar instancias existentes con las generadas, evitando duplicados por fecha
                val combinedInstances = mutableListOf<Task>()
                combinedInstances.addAll(existingInstancesForParent.filter { !it.isCompleted })

                // Agregar instancias generadas que no tengan conflicto con las existentes
                val maxToSave =
                    50 // Limita la cantidad de instancias guardadas por ciclo para evitar ANR
                var savedCount = 0
                for (generatedInstance in pagedGeneratedInstances) {
                    if (savedCount >= maxToSave) break
                    var generated = generatedInstance
                    val generatedDate = generated.startDateConf?.dateTime?.toLocalDate()
                    val hasExistingForDate = existingInstancesForParent.any { existing ->
                        existing.startDateConf?.dateTime?.toLocalDate() == generatedDate
                    }
                    if (!hasExistingForDate) {
                        combinedInstances.add(generated)
                        savedCount++
                        Log.d(
                            "GetExpandedTasksUseCase",
                            "Added generated instance for date: $generatedDate"
                        )
                    } else {
                        Log.d(
                            "GetExpandedTasksUseCase",
                            "Skipped duplicate instance for date: $generatedDate"
                        )
                    }
                }

                resultTasks.addAll(combinedInstances)
                Log.d(
                    "GetExpandedTasksUseCase",
                    "Added ${combinedInstances.size} total instances for ${parentTask.name}"
                )

                // IMPORTANTE: Si no hay instancias generadas, incluir la tarea padre para que sea visible
                if (combinedInstances.isEmpty()) {
                    resultTasks.add(parentTask)
                    Log.d(
                        "GetExpandedTasksUseCase",
                        "No instances generated, added parent task: ${parentTask.name}"
                    )
                }
            } else {
                // Para tareas no repetibles, incluir la tarea padre
                // CAMBIO: Ser más inclusivo - incluir tareas sin fecha o dentro del rango extendido
                val taskDate = parentTask.startDateConf?.dateTime?.toLocalDate()
                if (taskDate == null || !taskDate.isBefore(startDate.minusYears(1)) && !taskDate.isAfter(
                        endDate
                    )
                ) {
                    resultTasks.add(parentTask)
                    Log.d(
                        "GetExpandedTasksUseCase",
                        "Added non-repeatable task: ${parentTask.name}"
                    )
                }
            }
        }

        Log.d("GetExpandedTasksUseCase", "Total result tasks: ${resultTasks.size}")

        // Combinar instancias existentes con las tareas principales para la planificación
        val allPlannedTasks = resultTasks + instanceTasks.filter { instance ->
            resultTasks.none { it.id == instance.id }
        }

        return allPlannedTasks.sortedBy {
            it.startDateConf?.dateTime ?: it.createdDateTime
        }
    }

    /**
     * Genera instancias dinámicas de una tarea repetitiva para el rango de fechas especificado
     */
    private fun generateDynamicInstances(
        parentTask: Task,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Task> {
        val repeatPlan = parentTask.repeatPlan ?: return emptyList()

        Log.d("GetExpandedTasksUseCase", "Generando instancias para tarea: ${parentTask.name}")
        Log.d(
            "GetExpandedTasksUseCase",
            ", selectedDays: ${repeatPlan.selectedDays}, frequencyType: ${repeatPlan.frequencyType}"
        )

        // Determinar fecha de inicio de la tarea
        val taskStartDate = parentTask.startDateConf?.dateTime?.toLocalDate()
            ?: parentTask.createdDateTime.toLocalDate()

        // Determinar fecha de fin efectiva
        val effectiveEndDate = calculateEffectiveEndDate(repeatPlan, endDate, taskStartDate)

        // Calcular límite inteligente usando estrategia específica
        val generator = RepeatInstanceGenerator.create(repeatPlan)
        val maxInstances = generator.calculateMaxInstances(startDate, effectiveEndDate)

        Log.d(
            "GetExpandedTasksUseCase",
            "Generando hasta $maxInstances instancias hasta $effectiveEndDate"
        )

        // Generar instancias usando la estrategia específica
        val instances = generator.generateInstances(
            parentTask = parentTask,
            startDate = startDate,
            endDate = effectiveEndDate,
            taskStartDate = taskStartDate,
            maxInstances = maxInstances
        )

        Log.d(
            "GetExpandedTasksUseCase",
            "Total de instancias generadas para ${parentTask.name}: ${instances.size}"
        )
        return instances
    }

    private fun calculateEffectiveEndDate(
        repeatPlan: RepeatPlan,
        endDate: LocalDate,
        taskStartDate: LocalDate,
    ): LocalDate {
        return when {
            repeatPlan.endDate != null -> minOf(endDate, repeatPlan.endDate.toLocalDate())
            repeatPlan.repeatEndDate != null -> minOf(endDate, repeatPlan.repeatEndDate)
            else -> minOf(endDate, taskStartDate.plusYears(1))
        }
    }

    /**
     * Estrategia abstracta para generar instancias de repetición
     */
    private abstract class RepeatInstanceGenerator {

        abstract fun calculateMaxInstances(startDate: LocalDate, endDate: LocalDate): Int

        abstract fun generateInstances(
            parentTask: Task,
            startDate: LocalDate,
            endDate: LocalDate,
            taskStartDate: LocalDate,
            maxInstances: Int,
        ): List<Task>

        companion object {
            fun create(repeatPlan: RepeatPlan): RepeatInstanceGenerator {
                return when {
                    repeatPlan.selectedDays.isNotEmpty() -> WeeklyDaysGenerator(repeatPlan)
                    repeatPlan.frequencyType == FrequencyType.DAILY ||
                            (repeatPlan.frequency == RepeatFrequency.DAILY && repeatPlan.intervalNew == 1) ->
                        DailyGenerator(repeatPlan)

                    repeatPlan.frequencyType != FrequencyType.NONE -> FrequencyBasedGenerator(
                        repeatPlan
                    )

                    repeatPlan.daysOfMonth.isNotEmpty() -> MonthlyDaysGenerator(repeatPlan)
                    else -> DefaultGenerator()
                }
            }
        }

        fun createTaskInstance(
            parentTask: Task,
            date: LocalDate,
            instanceNumber: Int,
        ): Task {
            val instanceIdentifier = "${parentTask.id}_${date}_$instanceNumber"
            val timeOfDay =
                parentTask.startDateConf?.dateTime?.toLocalTime() ?: java.time.LocalTime.of(9, 0)
            val instanceDateTime = date.atTime(timeOfDay)

            return Task.Builder()
                .id(-instanceNumber)
                .name(parentTask.name)
                .isCompleted(false)
                .priority(parentTask.priority)
                .isRepeatedInstance(true)
                .parentTaskId(parentTask.id)
                .instanceIdentifier(instanceIdentifier)
                .startDateConf(
                    TimePlanning(
                        dateTime = instanceDateTime,
                        dayPeriod = parentTask.startDateConf?.dayPeriod ?: DayPeriod.NONE
                    )
                )
                .endDateConf(parentTask.endDateConf?.let { endConf ->
                    val originalStartDate =
                        parentTask.startDateConf?.dateTime ?: parentTask.createdDateTime
                    val duration = ChronoUnit.MINUTES.between(originalStartDate, endConf.dateTime!!)
                    TimePlanning(
                        dateTime = instanceDateTime.plusMinutes(duration),
                        dayPeriod = endConf.dayPeriod
                    )
                })
                .durationConf(parentTask.durationConf)
                .listId(parentTask.listId)
                .sectionId(parentTask.sectionId)
                .reminderPlan(parentTask.reminderPlan)
                .repeatPlan(null)
                .completionDateTime(null)
                .scheduledStartDateTime(null)
                .scheduledEndDateTime(null)
                .createdDateTime(parentTask.createdDateTime)
                .build()
        }
    }

    /**
     * Generador para tareas diarias
     */
    private class DailyGenerator(private val repeatPlan: RepeatPlan) : RepeatInstanceGenerator() {

        override fun calculateMaxInstances(startDate: LocalDate, endDate: LocalDate): Int {
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt()
            return daysBetween.coerceAtMost(7) // Limitar a un máximo de 7 días (una semana)
        }

        override fun generateInstances(
            parentTask: Task,
            startDate: LocalDate,
            endDate: LocalDate,
            taskStartDate: LocalDate,
            maxInstances: Int,
        ): List<Task> {
            val instances = mutableListOf<Task>()
            var currentDate = maxOf(taskStartDate, startDate)
            var instanceNumber = 1
            val interval = repeatPlan.interval ?: 1

            while (currentDate <= endDate && instanceNumber <= maxInstances) {
                // Generar instancias para cada día dentro del rango de planificación
                instances.add(createTaskInstance(parentTask, currentDate, instanceNumber))
                instanceNumber++

                currentDate = when (repeatPlan.frequencyType) {
                    FrequencyType.DAILY -> currentDate.plusDays(interval.toLong())
                    FrequencyType.WEEKLY -> currentDate.plusWeeks(interval.toLong())
                    FrequencyType.MONTHLY -> currentDate.plusMonths(interval.toLong())
                    FrequencyType.YEARLY -> currentDate.plusYears(interval.toLong())
                    else -> currentDate.plusDays(1)
                }
            }

            return instances
        }
    }

    /**
     * Generador para días específicos de la semana
     */
    private class WeeklyDaysGenerator(private val repeatPlan: RepeatPlan) :
        RepeatInstanceGenerator() {

        override fun calculateMaxInstances(startDate: LocalDate, endDate: LocalDate): Int {
            val weeks = ChronoUnit.WEEKS.between(startDate, endDate).toInt() + 1
            val daysPerWeek = repeatPlan.selectedDays.size
            return (weeks * daysPerWeek).coerceAtMost(400)
        }

        override fun generateInstances(
            parentTask: Task,
            startDate: LocalDate,
            endDate: LocalDate,
            taskStartDate: LocalDate,
            maxInstances: Int,
        ): List<Task> {
            val instances = mutableListOf<Task>()
            var currentDate = maxOf(taskStartDate, startDate)
            var instanceNumber = 1

            val convertedSelectedDays = repeatPlan.selectedDays.map { javaDayOfWeek ->
                when (javaDayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> DayOfWeek.MON
                    java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUE
                    java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WED
                    java.time.DayOfWeek.THURSDAY -> DayOfWeek.THU
                    java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRI
                    java.time.DayOfWeek.SATURDAY -> DayOfWeek.SAT
                    java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUN
                }
            }.toSet()

            while (currentDate <= endDate && instanceNumber <= maxInstances) {
                val customDayOfWeek = when (currentDate.dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> DayOfWeek.MON
                    java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUE
                    java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WED
                    java.time.DayOfWeek.THURSDAY -> DayOfWeek.THU
                    java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRI
                    java.time.DayOfWeek.SATURDAY -> DayOfWeek.SAT
                    java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUN
                }

                if (convertedSelectedDays.contains(customDayOfWeek)) {
                    instances.add(createTaskInstance(parentTask, currentDate, instanceNumber))
                    instanceNumber++
                }
                currentDate = currentDate.plusDays(1)
            }

            return instances
        }
    }

    /**
     * Generador genérico para otros tipos de frecuencia
     */
    private class FrequencyBasedGenerator(private val repeatPlan: RepeatPlan) :
        RepeatInstanceGenerator() {

        override fun calculateMaxInstances(startDate: LocalDate, endDate: LocalDate): Int {
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt()
            return when (repeatPlan.frequencyType) {
                FrequencyType.WEEKLY -> (daysBetween / 7).coerceAtMost(200)
                FrequencyType.MONTHLY -> (daysBetween / 30).coerceAtMost(50)
                FrequencyType.YEARLY -> (daysBetween / 365).coerceAtMost(10)
                else -> 100
            }
        }

        override fun generateInstances(
            parentTask: Task,
            startDate: LocalDate,
            endDate: LocalDate,
            taskStartDate: LocalDate,
            maxInstances: Int,
        ): List<Task> {
            val instances = mutableListOf<Task>()
            var currentDate = maxOf(taskStartDate, startDate)
            var instanceNumber = 1
            val interval = repeatPlan.interval ?: 1

            while (currentDate <= endDate && instanceNumber <= maxInstances) {
                // Solo agregar instancias que caen dentro del rango de planificación
                if (currentDate in startDate..endDate) {
                    instances.add(createTaskInstance(parentTask, currentDate, instanceNumber))
                }
                instanceNumber++

                currentDate = when (repeatPlan.frequencyType) {
                    FrequencyType.DAILY -> currentDate.plusDays(interval.toLong())
                    FrequencyType.WEEKLY -> currentDate.plusWeeks(interval.toLong())
                    FrequencyType.MONTHLY -> currentDate.plusMonths(interval.toLong())
                    FrequencyType.YEARLY -> currentDate.plusYears(interval.toLong())
                    else -> currentDate.plusDays(1)
                }
            }

            return instances
        }
    }

    /**
     * Generador para días específicos del mes
     */
    private class MonthlyDaysGenerator(private val repeatPlan: RepeatPlan) :
        RepeatInstanceGenerator() {

        override fun calculateMaxInstances(startDate: LocalDate, endDate: LocalDate): Int {
            val months = ChronoUnit.MONTHS.between(startDate, endDate).toInt() + 1
            return (months * repeatPlan.daysOfMonth.size).coerceAtMost(200)
        }

        override fun generateInstances(
            parentTask: Task,
            startDate: LocalDate,
            endDate: LocalDate,
            taskStartDate: LocalDate,
            maxInstances: Int,
        ): List<Task> {
            val instances = mutableListOf<Task>()
            var currentMonth = maxOf(taskStartDate.withDayOfMonth(1), startDate.withDayOfMonth(1))
            var instanceNumber = 1

            while (currentMonth <= endDate && instanceNumber <= maxInstances) {
                repeatPlan.daysOfMonth.forEach { dayOfMonth ->
                    try {
                        val instanceDate = currentMonth.withDayOfMonth(dayOfMonth)
                        if (instanceDate >= startDate && instanceDate <= endDate && instanceDate >= taskStartDate) {
                            instances.add(
                                createTaskInstance(
                                    parentTask,
                                    instanceDate,
                                    instanceNumber
                                )
                            )
                            instanceNumber++
                        }
                        // Día inválido para este mes
                    } catch (e: Exception) {
                        Log.w(
                            "GetExpandedTasksUseCase",
                            "Invalid day of month $dayOfMonth for month ${currentMonth.month}: ${e.message}"
                        )
                    }
                }
                currentMonth = currentMonth.plusMonths(1)
            }

            return instances
        }
    }

    /**
     * Generador por defecto
     */
    private class DefaultGenerator() : RepeatInstanceGenerator() {

        override fun calculateMaxInstances(startDate: LocalDate, endDate: LocalDate): Int = 1

        override fun generateInstances(
            parentTask: Task,
            startDate: LocalDate,
            endDate: LocalDate,
            taskStartDate: LocalDate,
            maxInstances: Int,
        ): List<Task> {
            return if (taskStartDate >= startDate && taskStartDate <= endDate) {
                listOf(createTaskInstance(parentTask, taskStartDate, 1))
            } else {
                emptyList()
            }
        }
    }
}