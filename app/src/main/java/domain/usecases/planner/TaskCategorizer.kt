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
        defaultTime: LocalTime,
        recurrenceExpander: RecurrenceExpander,
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
                "Task ${task.id}: isOverdue=${planningTask.flags.isOverdue}, constraintDate=${planningTask.flags.constraintDate}, startDate=${task.startDateConf.dateTime}"
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
                val occurrences = recurrenceExpander.expandRecurringTask(
                    planningTask,
                    scopeStart,
                    scopeEnd,
                    context
                )
                if (occurrences.isNotEmpty()) {
                    occurrences.forEach { time -> fixed.add(planningTask to time) }
                    context.placedTaskIds.add(task.id)
                } else if (planningTask.flags.isHardConflict) {

                } else {
                    if (!taskFitsScope(task, scopeStart, scopeEnd)) return@taskLoop

                }
                if (occurrences.isNotEmpty() || planningTask.flags.isHardConflict) return@taskLoop
            }

            if (!taskFitsScope(task, scopeStart, scopeEnd) && !planningTask.flags.isOverdue) {

                return@taskLoop
            }

            val startDate = task.startDateConf.dateTime
            val startPeriod = task.startDateConf.dayPeriod
            val endDate = task.endDateConf?.dateTime
            val hasSpecificStartTime =
                startDate != null && startDate.toLocalTime() != LocalTime.MIDNIGHT
            val hasEndDate = endDate != null
            val hasPeriod = startPeriod != DayPeriod.NONE && startPeriod != DayPeriod.ALLDAY

            when {

                planningTask.flags.isOverdue && planningTask.flags.constraintDate == today -> {
                    Log.v("Categorizer", "Task ${task.id} - Type: Overdue Today -> DateFlex")
                    dateFlex.computeIfAbsent(today) { mutableListOf() }.add(planningTask)
                }

                startDate != null && hasSpecificStartTime && !hasEndDate && !hasPeriod -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Exact Start Time (No End) -> Fixed"
                    )
                    if (startDate.toLocalDate() in scopeStart..scopeEnd) {
                        fixed.add(planningTask to startDate)
                    } else if (taskFitsScope(task, scopeStart, scopeEnd)) {
                        Log.v(
                            "Categorizer",
                            "Task ${task.id} - Fixed time outside scope -> FullFlex"
                        )
                        fullFlex.add(planningTask)
                    }
                }

                startDate != null && hasSpecificStartTime && hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Exact Start Time + End Date -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                startDate != null && hasPeriod && !hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Start Date + Period (No End) -> PeriodPending"
                    )
                    val targetDate = startDate.toLocalDate()
                    if (targetDate in scopeStart..scopeEnd) {
                        periodPending.computeIfAbsent(targetDate) { mutableMapOf() }
                            .computeIfAbsent(startPeriod) { mutableListOf() }
                            .add(planningTask)
                    } else if (taskFitsScope(task, scopeStart, scopeEnd)) {
                        Log.v(
                            "Categorizer",
                            "Task ${task.id} - Period date outside scope -> FullFlex"
                        )
                        fullFlex.add(planningTask)
                    }
                }

                startDate != null && hasPeriod && hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Start Date + Period + End Date -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                startDate != null && !hasSpecificStartTime && !hasPeriod && !hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Start Date Only (No End) -> DateFlex"
                    )
                    val targetDate = startDate.toLocalDate()
                    if (targetDate in scopeStart..scopeEnd) {
                        dateFlex.computeIfAbsent(targetDate) { mutableListOf() }.add(planningTask)
                    } else if (taskFitsScope(task, scopeStart, scopeEnd)) {
                        Log.v(
                            "Categorizer",
                            "Task ${task.id} - DateFlex date outside scope -> FullFlex"
                        )
                        fullFlex.add(planningTask)
                    }
                }

                startDate != null && !hasSpecificStartTime && !hasPeriod && hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Start Date Only + End Date -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                hasEndDate -> {
                    Log.v("Categorizer", "Task ${task.id} - Type: End Date Only -> DeadlineFlex")
                    deadlineFlex.add(planningTask)
                }

                else -> {
                    Log.v("Categorizer", "Task ${task.id} - Type: Fully Flexible -> FullFlex")
                    fullFlex.add(planningTask)
                }
            }
        }
        return CategorizationResult(fixed, periodPending, dateFlex, deadlineFlex, fullFlex)
    }

    private fun taskFitsScope(task: Task, scopeStart: LocalDate, scopeEnd: LocalDate): Boolean {
        val taskStart = task.startDateConf.dateTime?.toLocalDate()
        val taskEnd = task.endDateConf?.dateTime?.toLocalDate()
        val estimatedTaskEndFromStart = taskStart?.plusDays(
            (task.effectiveDurationMinutes / (24 * 60)).coerceAtLeast(0).toLong()
        )
        return when {
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