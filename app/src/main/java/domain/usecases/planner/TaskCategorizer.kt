package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.CategorizationResult
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.PlanningTask
import com.elena.autoplanner.domain.models.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.collections.forEach

class TaskCategorizer {
    fun categorizeTasks(
        planningTasks: Collection<PlanningTask>,
        scopeStart: LocalDate,
        scopeEnd: LocalDate,
        context: PlanningContext,
    ): CategorizationResult {
        val fixed = mutableListOf<Pair<PlanningTask, LocalDateTime>>()
        val periodPending =
            mutableMapOf<LocalDate, MutableMap<DayPeriod, MutableList<PlanningTask>>>()
        val dateFlex = mutableMapOf<LocalDate, MutableList<PlanningTask>>()
        val deadlineFlex = mutableListOf<PlanningTask>()
        val fullFlex = mutableListOf<PlanningTask>()
        val today = LocalDate.now()

        Log.d("TaskCategorizer", "=== INICIO CATEGORIZACIÓN ===")
        Log.d("TaskCategorizer", "Scope: $scopeStart a $scopeEnd, Hoy: $today")
        Log.d("TaskCategorizer", "Tareas a categorizar: ${planningTasks.size}")

        planningTasks.forEach taskLoop@{ planningTask ->
            // Skip tasks already placed or marked as hard conflicts
            if (context.placedTaskIds.contains(planningTask.id) || planningTask.flags.isHardConflict) {
                Log.d(
                    "TaskCategorizer",
                    "Task ${planningTask.id} SALTADA - ya colocada o conflicto duro"
                )
                return@taskLoop
            }

            val task = planningTask.task
            val startDate = task.startDateConf?.dateTime
            val startPeriod = task.startDateConf?.dayPeriod ?: DayPeriod.NONE
            val endDate = task.endDateConf?.dateTime
            val hasSpecificStartTime =
                startDate != null && startDate.toLocalTime() != LocalTime.MIDNIGHT
            val hasEndDate = endDate != null
            val hasPeriod = startPeriod != DayPeriod.NONE && startPeriod != DayPeriod.ALLDAY
            val hasDuration =
                task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes > 0

            // ✅ DEBUG ESPECÍFICO PARA TUS TAREAS
            Log.d("DEBUG_SPECIFIC", "")
            Log.d("DEBUG_SPECIFIC", "=== PROCESANDO: '${task.name}' (ID: ${task.id}) ===")
            Log.d("DEBUG_SPECIFIC", "startDateConf?.dateTime: ${task.startDateConf?.dateTime}")
            Log.d("DEBUG_SPECIFIC", "startDateConf?.dayPeriod: ${task.startDateConf?.dayPeriod}")
            Log.d("DEBUG_SPECIFIC", "endDateConf?.dateTime: ${task.endDateConf?.dateTime}")
            Log.d(
                "DEBUG_SPECIFIC",
                "durationConf?.totalMinutes: ${task.durationConf?.totalMinutes}"
            )
            Log.d("DEBUG_SPECIFIC", "priority: ${task.priority}")
            Log.d("DEBUG_SPECIFIC", "isExpired(): ${task.isExpired()}")
            Log.d("DEBUG_SPECIFIC", "isOverdue flag: ${planningTask.flags.isOverdue}")
            Log.d("DEBUG_SPECIFIC", "constraintDate: ${planningTask.flags.constraintDate}")
            Log.d("DEBUG_SPECIFIC", "hasSpecificStartTime: $hasSpecificStartTime")
            Log.d("DEBUG_SPECIFIC", "hasEndDate: $hasEndDate")
            Log.d("DEBUG_SPECIFIC", "hasPeriod: $hasPeriod")
            Log.d("DEBUG_SPECIFIC", "hasDuration: $hasDuration")

            // FASE 1: MANEJO DE TAREAS VENCIDAS PRIMERO
            if (planningTask.flags.isOverdue) {
                when {
                    // Tareas vencidas con fecha constraint específica para mañana
                    planningTask.flags.constraintDate == today.plusDays(1) -> {
                        Log.d(
                            "DEBUG_SPECIFIC",
                            "RESULTADO: Tarea '${task.name}' → DateFlex (Overdue Tomorrow)"
                        )
                        dateFlex.computeIfAbsent(today.plusDays(1)) { mutableListOf() }
                            .add(planningTask)
                        return@taskLoop
                    }

                    // Tareas vencidas con fecha constraint para hoy
                    planningTask.flags.constraintDate == today -> {
                        Log.d(
                            "DEBUG_SPECIFIC",
                            "RESULTADO: Tarea '${task.name}' → DateFlex (Overdue Today)"
                        )
                        dateFlex.computeIfAbsent(today) { mutableListOf() }.add(planningTask)
                        return@taskLoop
                    }

                    // Tareas vencidas con fecha constraint específica dentro del scope
                    planningTask.flags.constraintDate != null &&
                            planningTask.flags.constraintDate!! in scopeStart..scopeEnd -> {
                        val constraintDate = planningTask.flags.constraintDate!!
                        Log.d(
                            "DEBUG_SPECIFIC",
                            "RESULTADO: Tarea '${task.name}' → DateFlex (Overdue on $constraintDate)"
                        )
                        dateFlex.computeIfAbsent(constraintDate) { mutableListOf() }
                            .add(planningTask)
                        return@taskLoop
                    }

                    // Tareas vencidas sin constraint o constraint fuera del scope
                    else -> {
                        // CORRECCIÓN CRÍTICA: Determinar categoría según si solo tiene fecha fin o es completamente flexible
                        if (startDate == null && hasEndDate) {
                            Log.d(
                                "DEBUG_SPECIFIC",
                                "RESULTADO: Tarea '${task.name}' → DeadlineFlex (Overdue with only end date)"
                            )
                            deadlineFlex.add(planningTask)
                        } else {
                            Log.d(
                                "DEBUG_SPECIFIC",
                                "RESULTADO: Tarea '${task.name}' → FullFlex (Overdue Flexible)"
                            )
                            fullFlex.add(planningTask)
                        }
                        return@taskLoop
                    }
                }
            }

            // FASE 2: VERIFICAR SI LA TAREA ENCAJA EN EL SCOPE
            if (task.repeatPlan?.frequencyType != FrequencyType.NONE) {
                if (!taskFitsScope(task, scopeStart, scopeEnd)) {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → SALTADA (Tarea repetible fuera de scope)"
                    )
                    return@taskLoop
                }
            }

            if (!taskFitsScope(task, scopeStart, scopeEnd) && !planningTask.flags.isOverdue) {
                Log.d(
                    "DEBUG_SPECIFIC",
                    "RESULTADO: Tarea '${task.name}' → SALTADA (Fuera de scope y no vencida)"
                )
                return@taskLoop
            }

            // FASE 3: CATEGORIZACIÓN POR TIPO DE RESTRICCIÓN TEMPORAL
            when {
                // 1. TAREAS FIJAS: ✅ CORRECCIÓN CRÍTICA - Simplificar detección
                // Una tarea es fija si tiene fecha inicio Y fin específicas en el mismo día con hora específica
                hasSpecificStartTime && hasEndDate &&
                        startDate?.toLocalDate() == endDate?.toLocalDate() -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → Fixed (Same day start-end with specific time)"
                    )
                    if (startDate.toLocalDate() in scopeStart..scopeEnd) {
                        fixed.add(planningTask to startDate)
                        Log.d("TaskCategorizer", "     ✓ AÑADIDA A FIXED con hora: $startDate")
                    } else {
                        Log.d("TaskCategorizer", "     ✗ Fixed appointment fuera de scope")
                    }
                }

                // 2. TAREAS CON RANGO DE FECHAS + DURACIÓN: Flexibles dentro del rango (días diferentes)
                startDate != null && hasEndDate && hasDuration &&
                        startDate.toLocalDate() != endDate?.toLocalDate() -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → DeadlineFlex (Date Range + Duration)"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 3. TAREAS CON PERÍODO: CORRECCIÓN CRÍTICA
                hasPeriod -> {
                    val targetDate = when {
                        // ✅ Si tiene constraint date (ej: movida para mañana), usar esa fecha
                        planningTask.flags.constraintDate != null ->
                            planningTask.flags.constraintDate!!
                        // Si tiene fecha específica y está dentro del scope, usarla
                        startDate != null && startDate.toLocalDate() in scopeStart..scopeEnd ->
                            startDate.toLocalDate()
                        // ✅ CRÍTICO: Si la fecha original está fuera del scope, usar el primer día del scope
                        // Esto maneja el caso de "hacer ejercicio" (24 julio) cuando planificamos para mañana (25 julio)
                        startDate != null && !startDate.toLocalDate().isAfter(scopeEnd) ->
                            scopeStart  // Usar el primer día del scope
                        // Si no tiene fecha específica, usar el primer día del scope
                        else -> scopeStart
                    }

                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → PeriodPending (Period $startPeriod para $targetDate)"
                    )
                    Log.d("TaskCategorizer", "     - Fecha original: ${startDate?.toLocalDate()}")
                    Log.d(
                        "TaskCategorizer",
                        "     - Constraint date: ${planningTask.flags.constraintDate}"
                    )
                    Log.d("TaskCategorizer", "     - Fecha objetivo calculada: $targetDate")

                    if (targetDate in scopeStart..scopeEnd) {
                        periodPending.computeIfAbsent(targetDate) { mutableMapOf() }
                            .computeIfAbsent(startPeriod) { mutableListOf() }
                            .add(planningTask)
                        Log.d(
                            "TaskCategorizer",
                            "     ✅ AÑADIDA A PERIOD_PENDING[$targetDate][$startPeriod]"
                        )
                    } else {
                        Log.d(
                            "TaskCategorizer",
                            "     ✗ Período fuera de scope, moviendo a FullFlex"
                        )
                        fullFlex.add(planningTask)
                    }
                }

                // 4. TAREAS CON SOLO DEADLINE: Solo tienen fecha límite (SIN fecha de inicio)
                startDate == null && hasEndDate -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → DeadlineFlex (Deadline Only)"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 5. TAREAS CON HORARIO SUGERIDO: Fecha + hora específica pero SIN fecha fin
                startDate != null && hasSpecificStartTime && !hasEndDate -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → DateFlex (Suggested Time)"
                    )
                    val targetDate = startDate.toLocalDate()
                    if (targetDate in scopeStart..scopeEnd) {
                        dateFlex.computeIfAbsent(targetDate) { mutableListOf() }.add(planningTask)
                    } else {
                        Log.d(
                            "TaskCategorizer",
                            "     ✗ Suggested time fuera de scope, moviendo a FullFlex"
                        )
                        fullFlex.add(planningTask)
                    }
                }

                // 6. TAREAS CON FECHA PERO SIN HORA: Solo fecha (flexible en horario)
                startDate != null && !hasSpecificStartTime && !hasEndDate -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → DateFlex (Date Only)"
                    )
                    val targetDate = startDate.toLocalDate()
                    if (targetDate in scopeStart..scopeEnd) {
                        dateFlex.computeIfAbsent(targetDate) { mutableListOf() }.add(planningTask)
                    } else {
                        Log.d(
                            "TaskCategorizer",
                            "     ✗ Date only fuera de scope, moviendo a FullFlex"
                        )
                        fullFlex.add(planningTask)
                    }
                }

                // 7. CASOS MIXTOS: Start + End pero SIN duración específica
                startDate != null && hasEndDate && !hasDuration -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → DeadlineFlex (Date Range No Duration)"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 8. CORRECCIÓN: Tareas con fecha inicio pasada pero sin fecha fin - disponibles para el scope
                startDate != null && startDate.toLocalDate()
                    .isBefore(scopeStart) && !hasEndDate -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → FullFlex (Past start date, no end date)"
                    )
                    fullFlex.add(planningTask)
                }

                // 9. TAREAS COMPLETAMENTE LIBRES: Sin fechas específicas
                else -> {
                    Log.d(
                        "DEBUG_SPECIFIC",
                        "RESULTADO: Tarea '${task.name}' → FullFlex (Completely Flexible)"
                    )
                    fullFlex.add(planningTask)
                }
            }

            Log.d("DEBUG_SPECIFIC", "===============================================")
        }

        Log.d("TaskCategorizer", "")
        Log.d("TaskCategorizer", "=== RESUMEN CATEGORIZACIÓN ===")
        Log.d("TaskCategorizer", "Fixed: ${fixed.size} tareas")
        fixed.forEach { (task, dateTime) ->
            Log.d("TaskCategorizer", "  - Fixed: ${task.task.name} a las $dateTime")
        }
        Log.d(
            "TaskCategorizer",
            "Period: ${periodPending.values.sumOf { it.values.sumOf { list -> list.size } }} tareas"
        )
        periodPending.forEach { (date, periodMap) ->
            periodMap.forEach { (period, tasks) ->
                tasks.forEach { task ->
                    Log.d(
                        "TaskCategorizer",
                        "  - Period: ${task.task.name} el $date en período $period"
                    )
                }
            }
        }
        Log.d("TaskCategorizer", "DateFlex: ${dateFlex.values.sumOf { it.size }} tareas")
        Log.d("TaskCategorizer", "DeadlineFlex: ${deadlineFlex.size} tareas")
        Log.d("TaskCategorizer", "FullFlex: ${fullFlex.size} tareas")

        return CategorizationResult(fixed, periodPending, dateFlex, deadlineFlex, fullFlex)
    }

    private fun taskFitsScope(task: Task, scopeStart: LocalDate, scopeEnd: LocalDate): Boolean {
        val taskStart = task.startDateConf?.dateTime?.toLocalDate()
        val taskEnd = task.endDateConf?.dateTime?.toLocalDate()
        val today = LocalDate.now()

        Log.d(
            "TaskCategorizer",
            "  taskFitsScope - Task ${task.id}: start=$taskStart, end=$taskEnd, scope=$scopeStart..$scopeEnd"
        )

        // CORRECCIÓN: Mejor manejo de tareas con solo fecha fin
        if (taskStart == null && taskEnd != null) {
            // Si solo tiene fecha fin y está vencida, permitir en cualquier scope
            if (taskEnd.isBefore(today)) {
                Log.d("TaskCategorizer", "  ✓ Solo fecha fin vencida - válida para cualquier scope")
                return true
            }
            // Si solo tiene fecha fin no vencida, verificar que la fecha fin permita el scope
            val result = taskEnd >= scopeStart
            Log.d(
                "TaskCategorizer",
                "  ${if (result) "✓" else "✗"} Solo fecha fin no vencida - deadline permite scope: $result"
            )
            return result
        }

        // CORRECCIÓN: Si tiene fecha inicio pasada pero no fecha fin, es completamente flexible
        if (taskStart != null && taskStart.isBefore(scopeStart) && taskEnd == null) {
            Log.d(
                "TaskCategorizer",
                "  ✓ Fecha inicio pasada sin fecha fin - completamente flexible"
            )
            return true
        }

        val estimatedTaskEndFromStart = taskStart?.plusDays(
            (task.effectiveDurationMinutes / (24 * 60)).coerceAtLeast(0).toLong()
        )

        val isExpired = (taskStart != null && taskStart.isBefore(today)) ||
                (taskEnd != null && taskEnd.isBefore(today))

        // Permitir tareas sin fecha fin, o con fecha fin distinta al inicio, aunque el inicio sea anterior al scope
        if (taskStart != null && taskStart.isBefore(scopeStart)) {
            // Si no tiene fecha fin, puede colocarse en cualquier día del scope
            if (taskEnd == null) {
                Log.d("TaskCategorizer", "  ✓ Inicio anterior al scope pero sin fecha fin")
                return true
            }
            // Si tiene fecha fin y no es el mismo día que el inicio, y la fecha fin está dentro del scope o después
            if (taskEnd != taskStart && taskEnd >= scopeStart) {
                Log.d("TaskCategorizer", "  ✓ Inicio anterior pero fecha fin permite scope")
                return true
            }
            // Si la fecha fin es el mismo día que el inicio, no entra en el scope
            if (taskEnd == taskStart) {
                Log.d("TaskCategorizer", "  ✗ Fecha inicio = fecha fin, ambas antes del scope")
                return false
            }
        }

        val result = when {
            isExpired -> {
                Log.d("TaskCategorizer", "  ✓ Tarea vencida - siempre válida")
                true
            }

            taskStart == null && taskEnd == null -> {
                Log.d("TaskCategorizer", "  ✓ Completamente flexible")
                true
            }

            taskStart != null && taskStart.isAfter(scopeEnd) -> {
                Log.d("TaskCategorizer", "  ✗ Empieza después del scope")
                false
            }

            taskEnd != null && taskEnd.isBefore(scopeStart) -> {
                Log.d("TaskCategorizer", "  ✗ Termina antes del scope")
                false
            }

            taskStart != null && estimatedTaskEndFromStart != null &&
                    taskStart.isBefore(scopeStart) && estimatedTaskEndFromStart.isBefore(scopeStart) -> {
                Log.d("TaskCategorizer", "  ✗ Inicio y fin estimado antes del scope")
                false
            }

            else -> {
                Log.d("TaskCategorizer", "  ✓ Encaja en scope por defecto")
                true
            }
        }

        return result
    }
}