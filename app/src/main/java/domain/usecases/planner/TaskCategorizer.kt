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

        planningTasks.forEach taskLoop@{ planningTask ->

            if (context.placedTaskIds.contains(planningTask.id) || planningTask.flags.isHardConflict) {

                return@taskLoop
            }
            val task = planningTask.task
            Log.d(
                "CategoryDebug",
                "Task ${task.id}: isOverdue=${planningTask.flags.isOverdue}, constraintDate=${planningTask.flags.constraintDate}, startDate=${task.startDateConf?.dateTime}"
            )

            if (planningTask.flags.isOverdue) {
                when {

                    planningTask.flags.constraintDate == today -> {
                        Log.d("Categorizer", "Task ${task.id} - Type: Overdue Today -> DateFlex")
                        dateFlex.computeIfAbsent(today) { mutableListOf() }.add(planningTask)
                        return@taskLoop
                    }

                    planningTask.flags.constraintDate == null -> {
                        Log.d("Categorizer", "Task ${task.id} - Type: Overdue Flexible -> FullFlex")
                        fullFlex.add(planningTask)
                        return@taskLoop
                    }

                    else -> {
                        val constraintDate = planningTask.flags.constraintDate!!
                        if (constraintDate in scopeStart..scopeEnd) {
                            Log.d(
                                "Categorizer",
                                "Task ${task.id} - Type: Overdue on $constraintDate -> DateFlex"
                            )
                            dateFlex.computeIfAbsent(constraintDate) { mutableListOf() }
                                .add(planningTask)
                        } else {
                            Log.d(
                                "Categorizer",
                                "Task ${task.id} - Type: Overdue outside scope -> FullFlex"
                            )
                            fullFlex.add(planningTask)
                        }
                        return@taskLoop
                    }
                }
            }

            if (task.repeatPlan?.frequencyType != FrequencyType.NONE) {
                // Las tareas repetibles ya están generadas y no necesitan expansión adicional.
                if (!taskFitsScope(task, scopeStart, scopeEnd)) return@taskLoop
            }

            if (!taskFitsScope(task, scopeStart, scopeEnd) && !planningTask.flags.isOverdue) {

                return@taskLoop
            }

            val startDate = task.startDateConf?.dateTime
            val startPeriod = task.startDateConf?.dayPeriod ?: DayPeriod.NONE
            val endDate = task.endDateConf?.dateTime
            val hasSpecificStartTime =
                startDate != null && startDate.toLocalTime() != LocalTime.MIDNIGHT
            val hasEndDate = endDate != null
            val hasPeriod = startPeriod != DayPeriod.NONE && startPeriod != DayPeriod.ALLDAY
            val hasDuration =
                task.durationConf?.totalMinutes != null && task.durationConf.totalMinutes > 0

            when {

                planningTask.flags.isOverdue && planningTask.flags.constraintDate == today -> {
                    Log.v("Categorizer", "Task ${task.id} - Type: Overdue Today -> DateFlex")
                    dateFlex.computeIfAbsent(today) { mutableListOf() }.add(planningTask)
                }

                // NUEVA LÓGICA CORREGIDA:

                // 1. TAREAS FIJAS: Tienen fecha inicio específica + hora específica SIN rango de flexibilidad
                // Solo si la fecha de inicio y fin son la misma (cita puntual)
                startDate != null && hasEndDate && hasDuration && hasSpecificStartTime &&
                        startDate.toLocalDate() == endDate.toLocalDate() -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Fixed Appointment (Same day start-end) -> Fixed"
                    )
                    if (startDate.toLocalDate() in scopeStart..scopeEnd) {
                        fixed.add(planningTask to startDate)
                    }
                }

                // 2. TAREAS CON RANGO DE FECHAS + DURACIÓN: Flexibles dentro del rango
                startDate != null && hasEndDate && hasDuration &&
                        startDate.toLocalDate() != endDate?.toLocalDate() -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Date Range + Duration (Flexible within range) -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 3. TAREAS CON PERÍODO: Tienen fecha + período preferido (flexibles en horario)
                startDate != null && hasPeriod -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Period Flexible (Date + Period) -> PeriodPending"
                    )
                    val targetDate = startDate.toLocalDate()
                    if (targetDate in scopeStart..scopeEnd) {
                        periodPending.computeIfAbsent(targetDate) { mutableMapOf() }
                            .computeIfAbsent(startPeriod) { mutableListOf() }
                            .add(planningTask)
                    }
                }

                // 4. TAREAS CON DEADLINE: Solo tienen fecha límite (SIN fecha de inicio)
                startDate == null && hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Deadline Only (End Date Only) -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 5. TAREAS CON HORARIO SUGERIDO: Fecha + hora específica pero flexible
                startDate != null && hasSpecificStartTime && !hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Suggested Time (Start Only) -> DateFlex"
                    )
                    val targetDate = startDate.toLocalDate()
                    if (targetDate in scopeStart..scopeEnd) {
                        dateFlex.computeIfAbsent(targetDate) { mutableListOf() }.add(planningTask)
                    }
                }

                // 6. TAREAS CON FECHA PERO SIN HORA: Solo fecha (flexible en horario)
                startDate != null && !hasSpecificStartTime && !hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Date Only -> DateFlex"
                    )
                    val targetDate = startDate.toLocalDate()
                    if (targetDate in scopeStart..scopeEnd) {
                        dateFlex.computeIfAbsent(targetDate) { mutableListOf() }.add(planningTask)
                    }
                }

                // 7. CASOS MIXTOS: Start + End pero sin duración específica (deadline con rango)
                startDate != null && hasEndDate && !hasDuration -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Date Range Flexible (Start + End, No Duration) -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 8. TAREAS COMPLETAMENTE LIBRES: Sin fechas específicas
                else -> {
                    Log.v("Categorizer", "Task ${task.id} - Type: Completely Flexible -> FullFlex")
                    fullFlex.add(planningTask)
                }
            }
        }
        return CategorizationResult(fixed, periodPending, dateFlex, deadlineFlex, fullFlex)
    }

    private fun taskFitsScope(task: Task, scopeStart: LocalDate, scopeEnd: LocalDate): Boolean {
        val taskStart = task.startDateConf?.dateTime?.toLocalDate()
        val taskEnd = task.endDateConf?.dateTime?.toLocalDate()
        val estimatedTaskEndFromStart = taskStart?.plusDays(
            (task.effectiveDurationMinutes / (24 * 60)).coerceAtLeast(0).toLong()
        )

        // Para tareas expiradas, permitir que se procesen independientemente del scope
        val today = LocalDate.now()
        val isExpired = (taskStart != null && taskStart.isBefore(today)) ||
                (taskEnd != null && taskEnd.isBefore(today))

        return when {
            // Tareas expiradas siempre "fit" para poder ser procesadas
            isExpired -> true
            taskStart == null && taskEnd == null -> true
            taskStart != null && taskStart.isAfter(scopeEnd) -> false
            taskEnd != null && taskEnd.isBefore(scopeStart) -> false
            taskStart != null && estimatedTaskEndFromStart != null && taskStart.isBefore(scopeStart) && estimatedTaskEndFromStart.isBefore(
                scopeStart
            ) -> false

            else -> true
        }
    }
}