package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PlanningTask
import com.elena.autoplanner.domain.models.Priority
import java.time.LocalDate

class OverdueTaskHandler {
    fun handleOverdueTasks(
        context: PlanningContext,
        strategy: OverdueTaskHandling,
        today: LocalDate,
        scopeStartDate: LocalDate,
        scopeEndDate: LocalDate,
    ) {
        Log.d("OverdueTaskHandler", "=== INICIO MANEJO TAREAS VENCIDAS ===")
        Log.d("OverdueTaskHandler", "Hoy: $today, Scope: $scopeStartDate a $scopeEndDate")
        Log.d("OverdueTaskHandler", "Estrategia: $strategy")

        // FASE 1: IDENTIFICAR TAREAS VENCIDAS
        val overdueTaskIds = context.planningTaskMap.values
            .filter { planningTask ->
                val task = planningTask.task
                val isExpired = task.isExpired() && !context.placedTaskIds.contains(planningTask.id)

                // ✅ CORRECCIÓN CRÍTICA: Mejorar detección de citas fijas
                // Solo excluir tareas que son REALMENTE citas fijas (mismo día con hora específica Y duración explícita)
                val isFixedAppointment = task.startDateConf?.dateTime != null &&
                        task.endDateConf?.dateTime != null &&
                        task.startDateConf.dateTime.toLocalDate() == task.endDateConf?.dateTime?.toLocalDate() &&
                        // Verificar que tenga hora específica, no medianoche
                        task.startDateConf.dateTime.toLocalTime() != java.time.LocalTime.MIDNIGHT &&
                        // ✅ NUEVO: Considerar también tareas con hora específica aunque no tengan duración explícita
                        (task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes > 0 ||
                                task.startDateConf.dateTime.toLocalTime() != task.endDateConf?.dateTime?.toLocalTime())

                Log.d("OverdueTaskHandler", "Evaluando tarea ${task.id} ('${task.name}'):")
                Log.d("OverdueTaskHandler", "  - isExpired: $isExpired")
                Log.d("OverdueTaskHandler", "  - startDate: ${task.startDateConf?.dateTime}")
                Log.d("OverdueTaskHandler", "  - endDate: ${task.endDateConf?.dateTime}")
                Log.d("OverdueTaskHandler", "  - isFixedAppointment: $isFixedAppointment")
                Log.d(
                    "OverdueTaskHandler",
                    "  - ya colocada: ${context.placedTaskIds.contains(planningTask.id)}"
                )

                val shouldBeHandled = isExpired && !isFixedAppointment
                Log.d("OverdueTaskHandler", "  - ¿Manejar como vencida?: $shouldBeHandled")

                shouldBeHandled
            }
            .map { it.id }

        if (overdueTaskIds.isEmpty()) {
            Log.d("OverdueTaskHandler", "No se encontraron tareas vencidas")
        } else {
            Log.d(
                "OverdueTaskHandler",
                "Encontradas ${overdueTaskIds.size} tareas vencidas: $overdueTaskIds"
            )
        }

        // FASE 2: APLICAR ESTRATEGIA A CADA TAREA VENCIDA
        overdueTaskIds.forEach { taskId ->
            val planningTask = context.planningTaskMap[taskId] ?: return@forEach
            if (context.placedTaskIds.contains(taskId)) return@forEach

            Log.d(
                "OverdueTaskHandler",
                "Aplicando estrategia $strategy a tarea $taskId ('${planningTask.task.name}')"
            )

            when (strategy) {
                OverdueTaskHandling.POSTPONE_TO_TOMORROW -> {
                    // ✅ CORRECCIÓN CRÍTICA: POSTPONE_TO_TOMORROW debe programar la tarea para mañana
                    val hasSpecificPeriod = planningTask.task.startDateConf?.dayPeriod != null &&
                            planningTask.task.startDateConf.dayPeriod != com.elena.autoplanner.domain.models.DayPeriod.NONE &&
                            planningTask.task.startDateConf.dayPeriod != com.elena.autoplanner.domain.models.DayPeriod.ALLDAY

                    if (hasSpecificPeriod) {
                        // Para tareas con período: NO marcar como overdue, solo constraint date
                        planningTask.flags.constraintDate = today.plusDays(1)
                        planningTask.flags.needsManualResolution = false
                        // NO marcar isPostponed para que se procese normalmente
                        context.addPostponedTask(planningTask.task) // Solo para tracking/reporte
                        Log.d(
                            "OverdueTaskHandler",
                            "  ✓ Tarea con período $taskId pospuesta para mañana y programada automáticamente"
                        )
                    } else {
                        // Para tareas sin período: marcar como overdue con constraint
                        planningTask.flags.isOverdue = true
                        planningTask.flags.constraintDate = today.plusDays(1)
                        planningTask.flags.needsManualResolution = false
                        // NO marcar isPostponed para que se procese normalmente
                        context.addPostponedTask(planningTask.task) // Solo para tracking/reporte
                        Log.d(
                            "OverdueTaskHandler",
                            "  ✓ Tarea $taskId pospuesta para mañana y programada automáticamente"
                        )
                    }
                }

                OverdueTaskHandling.USER_REVIEW_REQUIRED -> {
                    context.addExpiredForManualResolution(planningTask.task)
                    Log.d("OverdueTaskHandler", "  ✓ Tarea $taskId marcada para revisión manual")
                }

                OverdueTaskHandling.NEXT_AVAILABLE -> {
                    handleSingleTaskNextAvailable(
                        context,
                        planningTask,
                        today,
                        scopeStartDate,
                        scopeEndDate
                    )
                }
            }
        }

        // FASE 3: MANEJO ESPECIAL PARA SCOPE "MAÑANA"
        if (scopeStartDate == today.plusDays(1) && scopeEndDate == today.plusDays(1)) {
            Log.d("OverdueTaskHandler", "")
            Log.d("OverdueTaskHandler", "=== MANEJO ESPECIAL PARA SCOPE MAÑANA ===")

            val tasksToMakeAvailableTomorrow =
                context.planningTaskMap.values.filter { planningTask ->
                val task = planningTask.task
                val startDate = task.startDateConf?.dateTime?.toLocalDate()
                    val endDate = task.endDateConf?.dateTime?.toLocalDate()
                    val startPeriod = task.startDateConf?.dayPeriod

                    // ✅ CORRECCIÓN CRÍTICA: Mejorar detección de citas fijas para este contexto también
                val isFixedAppointment = task.startDateConf?.dateTime != null &&
                        task.endDateConf?.dateTime != null &&
                        startDate == endDate &&
                        task.startDateConf.dateTime.toLocalTime() != java.time.LocalTime.MIDNIGHT &&
                        (task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes > 0 ||
                                task.startDateConf.dateTime.toLocalTime() != task.endDateConf?.dateTime?.toLocalTime())

                    val hasDuration =
                        task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes > 0

                    Log.d(
                        "OverdueTaskHandler",
                        "Evaluando para mañana - Tarea ${task.id} ('${task.name}'):"
                    )
                    Log.d("OverdueTaskHandler", "  - startDate: $startDate, endDate: $endDate")
                    Log.d("OverdueTaskHandler", "  - startPeriod: $startPeriod")
                    Log.d("OverdueTaskHandler", "  - isFixedAppointment: $isFixedAppointment")
                    Log.d("OverdueTaskHandler", "  - hasDuration: $hasDuration")
                    Log.d("OverdueTaskHandler", "  - isCompleted: ${planningTask.task.isCompleted}")
                    Log.d(
                        "OverdueTaskHandler",
                        "  - ya colocada: ${context.placedTaskIds.contains(planningTask.id)}"
                    )

                    // ✅ CORRECCIÓN CRÍTICA: Mejorar lógica para incluir tareas con período específico
                    val shouldBeMadeAvailable = when {
                        // ✅ CASO ESPECÍFICO: Tareas con fecha de hoy/anterior + período específico
                        startDate != null && startDate <= today &&
                                startPeriod != null && startPeriod != com.elena.autoplanner.domain.models.DayPeriod.NONE &&
                                startPeriod != com.elena.autoplanner.domain.models.DayPeriod.ALLDAY -> {
                            Log.d(
                                "OverdueTaskHandler",
                                "  - ✅ Tiene fecha anterior + período $startPeriod - DEBE estar disponible mañana"
                            )
                            true
                        }
                        // Tareas con fecha inicio hoy o anterior pero sin fecha fin - disponibles mañana
                        startDate != null && startDate <= today && endDate == null -> {
                            Log.d(
                                "OverdueTaskHandler",
                                "  - Fecha inicio pasada sin fecha fin - disponible mañana"
                            )
                            true
                        }
                        // Tareas con solo fecha fin hoy o anterior - vencidas, disponibles mañana
                        startDate == null && endDate != null && endDate <= today -> {
                            Log.d(
                                "OverdueTaskHandler",
                                "  - Solo fecha fin vencida - disponible mañana"
                            )
                            true
                        }
                        // ✅ NUEVO: Tareas completamente flexibles SIN duración específica pero con período
                        startDate == null && endDate == null && startPeriod != null &&
                                startPeriod != com.elena.autoplanner.domain.models.DayPeriod.NONE &&
                                startPeriod != com.elena.autoplanner.domain.models.DayPeriod.ALLDAY -> {
                            Log.d(
                                "OverdueTaskHandler",
                                "  - Completamente flexible con período específico - disponible mañana"
                            )
                            true
                        }
                        // Tareas completamente flexibles con duración
                        startDate == null && endDate == null && hasDuration -> {
                            Log.d(
                                "OverdueTaskHandler",
                                "  - Completamente flexible con duración - disponible mañana"
                            )
                            true
                        }

                        else -> {
                            Log.d("OverdueTaskHandler", "  - No necesita estar disponible mañana")
                            false
                        }
                    }

                    val finalDecision = !planningTask.task.isCompleted &&
                            shouldBeMadeAvailable &&
                            !isFixedAppointment &&
                            !context.placedTaskIds.contains(planningTask.id)

                    Log.d("OverdueTaskHandler", "  - ¿Hacer disponible mañana?: $finalDecision")

                    finalDecision
                }

            Log.d(
                "OverdueTaskHandler",
                "Tareas a hacer disponibles mañana: ${tasksToMakeAvailableTomorrow.size}"
            )

            tasksToMakeAvailableTomorrow.forEach { planningTask ->
                // ✅ CORRECCIÓN CRÍTICA: Preservar mejor la información de período
                val hasSpecificPeriod = planningTask.task.startDateConf?.dayPeriod != null &&
                        planningTask.task.startDateConf.dayPeriod != com.elena.autoplanner.domain.models.DayPeriod.NONE &&
                        planningTask.task.startDateConf.dayPeriod != com.elena.autoplanner.domain.models.DayPeriod.ALLDAY

                // ✅ CRÍTICO: Para tareas con período específico, NO marcar como overdue
                // Esto permite que mantengan su categoría Period en TaskCategorizer
                if (hasSpecificPeriod) {
                    // Solo establecer constraint date, mantener toda la demás información intacta
                    planningTask.flags.constraintDate = today.plusDays(1)
                    planningTask.flags.needsManualResolution = false
                    // ✅ NO establecer isOverdue = true para preservar la lógica de período
                    Log.d(
                        "OverdueTaskHandler",
                        "  ✅ Tarea con período ${planningTask.id} ('${planningTask.task.name}') disponible para mañana SIN marcar como overdue"
                    )
                } else {
                    // Para tareas sin período específico: usar la lógica anterior
                    planningTask.flags.isOverdue = true
                    planningTask.flags.constraintDate = today.plusDays(1)
                    planningTask.flags.needsManualResolution = false
                    Log.d(
                        "OverdueTaskHandler",
                        "  ✅ Tarea ${planningTask.id} ('${planningTask.task.name}') disponible para mañana"
                    )
                }
            }
        }

        Log.d("OverdueTaskHandler", "=== FIN MANEJO TAREAS VENCIDAS ===")
    }

    private fun handleSingleTaskNextAvailable(
        context: PlanningContext,
        planningTask: PlanningTask,
        today: LocalDate,
        scopeStartDate: LocalDate,
        scopeEndDate: LocalDate,
    ) {
        Log.d("OverdueTaskHandler", "  Aplicando NEXT_AVAILABLE a tarea ${planningTask.id}")

        val availableDays = mutableListOf<LocalDate>()
        var currentDate = maxOf(today, scopeStartDate)
        while (!currentDate.isAfter(scopeEndDate)) {
            availableDays.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        if (availableDays.isEmpty()) {
            context.addExpiredForManualResolution(planningTask.task)
            Log.d(
                "OverdueTaskHandler",
                "    ✗ No hay días disponibles - marcada para revisión manual"
            )
            return
        }

        // ✅ CORRECCIÓN: Distribución más inteligente basada en prioridad
        val targetDate = when (planningTask.task.priority) {
            Priority.HIGH -> availableDays.first() // Tareas de alta prioridad van al primer día disponible
            Priority.MEDIUM -> availableDays[minOf(
                1,
                availableDays.size - 1
            )] // Tareas medias van al segundo día si está disponible
            else -> availableDays.last() // Tareas de baja prioridad van al último día disponible
        }

        // ✅ CORRECCIÓN CRÍTICA: Preservar información de período para tareas NEXT_AVAILABLE
        val hasSpecificPeriod = planningTask.task.startDateConf?.dayPeriod != null &&
                planningTask.task.startDateConf.dayPeriod != com.elena.autoplanner.domain.models.DayPeriod.NONE &&
                planningTask.task.startDateConf.dayPeriod != com.elena.autoplanner.domain.models.DayPeriod.ALLDAY

        if (hasSpecificPeriod) {
            // Para tareas con período: NO marcar como overdue, solo establecer constraint date
            planningTask.flags.constraintDate = targetDate
            planningTask.flags.needsManualResolution = false
            Log.d(
                "OverdueTaskHandler",
                "    ✓ Tarea con período ${planningTask.id} (Prioridad: ${planningTask.task.priority}) marcada para colocación automática el $targetDate SIN marcar como overdue"
            )
        } else {
            // Para tareas sin período: marcar como overdue
            planningTask.flags.isOverdue = true
            planningTask.flags.constraintDate = targetDate
            planningTask.flags.needsManualResolution = false
            Log.d(
                "OverdueTaskHandler",
                "    ✓ Tarea ${planningTask.id} (Prioridad: ${planningTask.task.priority}) marcada para colocación automática el $targetDate"
            )
        }
    }
}