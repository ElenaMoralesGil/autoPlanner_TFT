package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ConflictType
import com.elena.autoplanner.domain.models.PlannerInput
import com.elena.autoplanner.domain.models.PlannerOutput
import com.elena.autoplanner.domain.models.ScheduleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class GeneratePlanUseCase(
    private val taskCategorizer: TaskCategorizer,
    private val recurrenceExpander: RecurrenceExpander,
    private val timelineManager: TimelineManager,
    private val taskPlacer: TaskPlacer,
    private val overdueTaskHandler: OverdueTaskHandler,
    private val taskPrioritizer: TaskPrioritizer,
) {

    suspend operator fun invoke(input: PlannerInput): PlannerOutput =
        withContext(Dispatchers.Default) {
            Log.i("GeneratePlanUseCase", "--- Starting Refined Plan Generation ---")
            val today = LocalDate.now()
            val (scheduleStartDate, scheduleEndDate) = determineSchedulingWindow(
                input.scheduleScope,
                today
            )

            val context = PlanningContext(input.tasks.filter { !it.isCompleted })
            Log.d(
                "GeneratePlanUseCase",
                "Phase 0: Preparation - Scope: $scheduleStartDate to $scheduleEndDate, Tasks: ${context.planningTaskMap.size}"
            )

            overdueTaskHandler.handleOverdueTasks(
                context = context,
                strategy = input.overdueTaskHandling,
                today = today,
                scopeStartDate = scheduleStartDate,
                scopeEndDate = scheduleEndDate
            )
            Log.d(
                "GeneratePlanUseCase",
                "After Overdue Handling: Tasks to Plan=${context.getTasksToPlan().size}, Postponed=${context.postponedTasks.size}, Manual=${context.expiredForResolution.size}"
            )

            val categorizationResult = taskCategorizer.categorizeTasks(
                planningTasks = context.getTasksToPlan(), 
                scopeStart = scheduleStartDate,
                scopeEnd = scheduleEndDate,
                defaultTime = input.workStartTime,
                recurrenceExpander = recurrenceExpander,
                context = context
            )
            Log.d(
                "GeneratePlanUseCase",
                "Categorization Result: Fixed=${categorizationResult.fixedOccurrences.size}, Period=${categorizationResult.periodTasksPending.values.sumOf { it.values.sumOf { list -> list.size } }}, DateFlex=${categorizationResult.dateFlexPending.values.sumOf { it.size }}, DeadlineFlex=${categorizationResult.deadlineFlexibleTasks.size}, FullFlex=${categorizationResult.fullyFlexibleTasks.size}"
            )

            timelineManager.initialize(
                start = scheduleStartDate,
                end = scheduleEndDate,
                workStart = input.workStartTime,
                workEnd = input.workEndTime,
                initialPeriodTasks = emptyMap(),
                dayOrganization = input.dayOrganization

            )
            Log.d(
                "GeneratePlanUseCase",
                "Timeline initialized for ${timelineManager.getDates().size} days."
            )

            Log.i(
                "GeneratePlanUseCase",
                "--- Phase 1: Place Fixed Tasks (${categorizationResult.fixedOccurrences.size}) ---"
            )
            taskPlacer.placeFixedTasks(
                timelineManager,
                categorizationResult.fixedOccurrences,
                context
            )

            Log.i("GeneratePlanUseCase", "--- Phase 2: Place Prioritized Remaining Tasks ---")

            val tasksToPrioritize = (
                    categorizationResult.periodTasksPending.values.flatMap { it.values.flatten() } +
                            categorizationResult.dateFlexPending.values.flatten() +
                            categorizationResult.deadlineFlexibleTasks +
                            categorizationResult.fullyFlexibleTasks
                    )
                .mapNotNull { context.planningTaskMap[it.id] }
                .filterNot { context.placedTaskIds.contains(it.id) }
                .distinctBy { it.id }

            val sortedTasksToPlace = tasksToPrioritize
                .sortedByDescending {
                    taskPrioritizer.calculateRobustScore(
                        it,
                        input.prioritizationStrategy,
                    )
                }

            Log.d(
                "GeneratePlanUseCase",
                "Prioritized ${sortedTasksToPlace.size} tasks for placement."
            )

            sortedTasksToPlace.forEach { planningTask ->
                if (!context.placedTaskIds.contains(planningTask.id)) { 
                    taskPlacer.placePrioritizedTask(
                        planningTask = planningTask,
                        timelineManager = timelineManager,
                        context = context,
                        input = input,
                        today = today,
                        scopeStartDate = scheduleStartDate,
                        scopeEndDate = scheduleEndDate
                    )
                }
            }

            Log.i("GeneratePlanUseCase", "--- Consolidating Results ---")
            consolidateResults(context, timelineManager) 

            Log.i(
                "GeneratePlanUseCase",
                "Final Results: Scheduled=${context.scheduledItemsMap.values.sumOf { it.size }}, Conflicts=${context.conflicts.size}, Expired=${context.expiredForResolution.size}, Postponed=${context.postponedTasks.size}, Info=${context.infoItems.size}"
            )

            return@withContext PlannerOutput(
                scheduledTasks = context.scheduledItemsMap,
                unresolvedExpired = context.expiredForResolution,
                unresolvedConflicts = context.conflicts,
                postponedTasks = context.postponedTasks,
                infoItems = context.infoItems
            )
        }

    private fun determineSchedulingWindow(
        scope: ScheduleScope,
        today: LocalDate,
    ): Pair<LocalDate, LocalDate> {
        return when (scope) {
            ScheduleScope.TODAY -> today to today
            ScheduleScope.TOMORROW -> today.plusDays(1) to today.plusDays(1)
            ScheduleScope.THIS_WEEK -> today to today.plusDays(6)
        }
    }

    private fun consolidateResults(context: PlanningContext, timelineManager: TimelineManager) {

        timelineManager.getPendingPeriodTasks().forEach { (date, periodMap) ->
            periodMap.forEach { (period, tasks) ->
                tasks.forEach { taskRef ->
                    if (!context.placedTaskIds.contains(taskRef.id)) {
                        val planningTask = context.planningTaskMap[taskRef.id]
                        if (planningTask != null && !planningTask.flags.isHardConflict && !planningTask.flags.needsManualResolution && !planningTask.flags.isPostponed) {
                            context.addConflict(
                                ConflictItem(
                                    listOf(taskRef),
                                    "Cannot fit Period constraint ($period) on $date",
                                    taskRef.startDateConf.dateTime ?: date.atStartOfDay(),
                                    ConflictType.CANNOT_FIT_PERIOD
                                ),
                                taskRef.id 
                            )
                            Log.w(
                                "Consolidate",
                                "Task ${taskRef.id} remains in pending period $period on $date, adding conflict."
                            )
                        }
                    }
                }
            }
        }

        val hardConflictTaskIds = context.conflicts
            .filter { it.conflictType == ConflictType.FIXED_VS_FIXED || it.conflictType == ConflictType.RECURRENCE_ERROR }
            .flatMap { it.conflictingTasks.map { t -> t.id } }
            .toSet()

        hardConflictTaskIds.forEach { taskId ->
            if (!context.placedTaskIds.contains(taskId)) {
                context.planningTaskMap[taskId]?.task?.let { task ->

                    if (context.conflicts.none { c -> c.conflictingTasks.any { it.id == taskId } }) {
                        context.addConflict(
                            ConflictItem(
                                listOf(task),
                                "Remains unplaced due to prior hard conflict",
                                task.startDateConf.dateTime,
                                ConflictType.PLACEMENT_ERROR
                            ),
                            taskId 
                        )
                        Log.w(
                            "Consolidate",
                            "Task $taskId remains unplaced due to prior hard conflict, adding conflict."
                        )
                    } else {
                        context.placedTaskIds.add(taskId) 
                    }
                }
            }
        }

        context.scheduledItemsMap.values.forEach { it.sortBy { item -> item.scheduledStartTime } }
    }
}