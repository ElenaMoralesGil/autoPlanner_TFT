package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.ZoneId
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.time.temporal.TemporalAdjusters
import java.util.TimeZone
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.*
import java.time.Instant
import java.time.format.DateTimeFormatter

class GeneratePlanUseCase(
    private val taskCategorizer: TaskCategorizer,
    private val recurrenceExpander: RecurrenceExpander,
    private val timelineManager: TimelineManager,
    private val taskPlacer: TaskPlacer,
    private val overdueTaskHandler: OverdueTaskHandler,
    private val taskPrioritizer: TaskPrioritizer,
) {
    companion object {
        const val RECURRENCE_ITERATION_LIMIT = 1000
    }

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

            // --- Phase 0.1: Handle Overdue ---
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

            // --- Phase 0.2: Categorize ---
            // Categorization still useful to identify fixed vs flexible initially
            val categorizationResult = taskCategorizer.categorizeTasks(
                planningTasks = context.getTasksToPlan(), // Get tasks not yet handled by overdue
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

            // --- Phase 0.3: Initialize Timeline ---
            timelineManager.initialize(
                start = scheduleStartDate,
                end = scheduleEndDate,
                workStart = input.workStartTime,
                workEnd = input.workEndTime,
                // Don't pass initial period tasks here anymore, they'll be placed in the main loop
                initialPeriodTasks = emptyMap()
            )
            Log.d(
                "GeneratePlanUseCase",
                "Timeline initialized for ${timelineManager.getDates().size} days."
            )

            // --- Phase 1: Place STRICTLY Fixed Tasks ---
            Log.i(
                "GeneratePlanUseCase",
                "--- Phase 1: Place Fixed Tasks (${categorizationResult.fixedOccurrences.size}) ---"
            )
            taskPlacer.placeFixedTasks(
                timelineManager,
                categorizationResult.fixedOccurrences,
                context
            )

            // --- Phase 2: Prioritized Placement of ALL Remaining Tasks ---
            Log.i("GeneratePlanUseCase", "--- Phase 2: Place Prioritized Remaining Tasks ---")

            // Combine all non-fixed tasks that haven't been handled yet
            val tasksToPrioritize = (
                    categorizationResult.periodTasksPending.values.flatMap { it.values.flatten() } +
                            categorizationResult.dateFlexPending.values.flatten() +
                            categorizationResult.deadlineFlexibleTasks +
                            categorizationResult.fullyFlexibleTasks
                    )
                .mapNotNull { context.planningTaskMap[it.id] } // Get the PlanningTask object
                .filterNot { context.placedTaskIds.contains(it.id) } // Filter already placed/conflicted
                .distinctBy { it.id } // Ensure uniqueness

            // Sort the combined list by priority score
            val sortedTasksToPlace = tasksToPrioritize
                .sortedByDescending {
                    taskPrioritizer.calculateRobustScore(
                        it,
                        input.prioritizationStrategy,
                        today
                    )
                }

            Log.d(
                "GeneratePlanUseCase",
                "Prioritized ${sortedTasksToPlace.size} tasks for placement."
            )

            // Loop through sorted tasks and attempt placement
            sortedTasksToPlace.forEach { planningTask ->
                if (!context.placedTaskIds.contains(planningTask.id)) { // Double-check if handled
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

            // --- Phase 3: Consolidation ---
            Log.i("GeneratePlanUseCase", "--- Consolidating Results ---")
            consolidateResults(context, timelineManager) // Keep consolidation logic

            Log.i(
                "GeneratePlanUseCase",
                "Final Results: Scheduled=${context.scheduledItemsMap.values.sumOf { it.size }}, Conflicts=${context.conflicts.size}, Expired=${context.expiredForResolution.size}, Postponed=${context.postponedTasks.size}, Info=${context.infoItems.size}"
            )

            // --- Phase 4: Return Output ---
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
            ScheduleScope.THIS_WEEK -> today to today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
        }
    }

    private fun consolidateResults(context: PlanningContext, timelineManager: TimelineManager) {
        // Add unplaced period tasks from timeline manager's state as conflicts
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
                                taskRef.id // Mark as handled (conflicted)
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

        // Ensure tasks involved in hard conflicts earlier are marked as handled if not placed
        val hardConflictTaskIds = context.conflicts
            .filter { it.conflictType == ConflictType.FIXED_VS_FIXED || it.conflictType == ConflictType.RECURRENCE_ERROR }
            .flatMap { it.conflictingTasks.map { t -> t.id } }
            .toSet()

        hardConflictTaskIds.forEach { taskId ->
            if (!context.placedTaskIds.contains(taskId)) {
                context.planningTaskMap[taskId]?.task?.let { task ->
                    // Avoid adding *another* conflict if one already exists for this task
                    if (context.conflicts.none { c -> c.conflictingTasks.any { it.id == taskId } }) {
                        context.addConflict(
                            ConflictItem(
                                listOf(task),
                                "Remains unplaced due to prior hard conflict",
                                task.startDateConf.dateTime,
                                ConflictType.PLACEMENT_ERROR
                            ),
                            taskId // Mark as handled (conflicted)
                        )
                        Log.w(
                            "Consolidate",
                            "Task ${taskId} remains unplaced due to prior hard conflict, adding conflict."
                        )
                    } else {
                        context.placedTaskIds.add(taskId) // Ensure it's marked handled even if conflict already exists
                    }
                }
            }
        }

        // Sort scheduled items within each day
        context.scheduledItemsMap.values.forEach { it.sortBy { item -> item.scheduledStartTime } }
    }
}

// --- PlanningContext --- (Holds mutable state)
class PlanningContext(initialTasks: List<Task>) {
    val planningTaskMap: MutableMap<Int, PlanningTask> =
        initialTasks.associate { it.id to PlanningTask(it) }.toMutableMap()
    val conflicts: MutableList<ConflictItem> = mutableListOf()
    val expiredForResolution: MutableList<Task> = mutableListOf() // Tasks needing manual review
    val postponedTasks: MutableList<Task> = mutableListOf()      // Tasks flagged for postponement
    val infoItems: MutableList<InfoItem> = mutableListOf()
    val scheduledItemsMap: MutableMap<LocalDate, MutableList<ScheduledTaskItem>> = mutableMapOf()
    val placedTaskIds: MutableSet<Int> =
        mutableSetOf() // Tracks IDs handled (placed, conflicted, postponed, etc.)

    fun getTasksToPlan(): Collection<PlanningTask> = planningTaskMap.values.filterNot {
        placedTaskIds.contains(it.id) || it.flags.isPostponed || it.flags.needsManualResolution
    }

    fun addConflict(conflict: ConflictItem, taskIdToMarkHandled: Int?) {
        if (conflicts.none { it.hashCode() == conflict.hashCode() }) {
            conflicts.add(conflict)
        }
        // Mark all involved tasks as handled
        conflict.conflictingTasks.forEach { task ->
            placedTaskIds.add(task.id)
            // Mark flags for hard conflicts
            if (conflict.conflictType == ConflictType.FIXED_VS_FIXED || conflict.conflictType == ConflictType.RECURRENCE_ERROR) {
                planningTaskMap[task.id]?.flags?.isHardConflict = true
            }
        }
        // Also mark the explicitly passed ID if different (e.g., the task *causing* the conflict)
        taskIdToMarkHandled?.let { placedTaskIds.add(it) }
    }

    fun addScheduledItem(item: ScheduledTaskItem, taskId: Int) {
        scheduledItemsMap.computeIfAbsent(item.date) { mutableListOf() }.add(item)
        placedTaskIds.add(taskId)
    }

    fun addInfoItem(info: InfoItem) {
        if (infoItems.none { it.task.id == info.task.id && it.message == info.message }) {
            infoItems.add(info)
        }
    }

    fun addPostponedTask(task: Task) {
        if (postponedTasks.none { it.id == task.id }) {
            postponedTasks.add(task)
        }
        // Don't remove from planningTaskMap, just mark flags and handled ID
        planningTaskMap[task.id]?.flags?.isPostponed = true
        placedTaskIds.add(task.id)
    }

    fun addExpiredForManualResolution(task: Task) {
        if (expiredForResolution.none { it.id == task.id }) {
            expiredForResolution.add(task)
        }
        // Don't remove from planningTaskMap, just mark flags and handled ID
        planningTaskMap[task.id]?.flags?.needsManualResolution = true
        placedTaskIds.add(task.id)
    }
}

// --- OverdueTaskHandler ---
class OverdueTaskHandler {
    fun handleOverdueTasks(
        context: PlanningContext,
        strategy: OverdueTaskHandling,
        today: LocalDate,
        scopeStartDate: LocalDate,
        scopeEndDate: LocalDate,
    ) {
        val overdueTaskIds = context.planningTaskMap.values
            .filter { it.task.isExpired() && !context.placedTaskIds.contains(it.id) }
            .map { it.id }

        if (overdueTaskIds.isEmpty()) return
        Log.d(
            "OverdueTaskHandler",
            "Handling ${overdueTaskIds.size} overdue tasks with strategy: $strategy"
        )

        overdueTaskIds.forEach { taskId ->
            val planningTask = context.planningTaskMap[taskId] ?: return@forEach
            if (context.placedTaskIds.contains(taskId)) return@forEach // Double check

            when (strategy) {
                OverdueTaskHandling.POSTPONE_TO_TOMORROW -> {
                    context.addPostponedTask(planningTask.task) // Uses context method to set flags/ID
                }

                OverdueTaskHandling.MANAGE_WHEN_FREE -> {
                    context.addExpiredForManualResolution(planningTask.task) // Uses context method
                }

                OverdueTaskHandling.ADD_TODAY_FREE_TIME -> {
                    if (scopeStartDate <= today && scopeEndDate >= today) {
                        planningTask.flags.isOverdue = true
                        planningTask.flags.constraintDate = today
                        Log.d("OverdueTaskHandler", "Task ${taskId} marked for placement today.")
                        // Keep in taskMap for placement attempts, will be filtered by getTasksToPlan
                    } else {
                        Log.w(
                            "OverdueTaskHandler",
                            "Cannot schedule overdue task ${taskId} today (scope outside). Fallback to Manual."
                        )
                        context.addExpiredForManualResolution(planningTask.task) // Uses context method
                    }
                }
            }
        }
    }
}

class TaskPrioritizer {
    fun calculateRobustScore(
        planningTask: PlanningTask,
        strategy: PrioritizationStrategy,
        today: LocalDate,
    ): Double {
        val task = planningTask.task
        var score = 0.0
        score += when (task.priority) {
            Priority.HIGH -> 10000.0
            Priority.MEDIUM -> 5000.0
            Priority.LOW -> 1000.0
            Priority.NONE -> 100.0
        }

        task.endDateConf?.dateTime?.let { deadline ->
            val now = LocalDateTime.now()
            val hours = Duration.between(now, deadline).toHours()
            score += when {
                hours < 0 -> 50000.0
                hours <= 8 -> 20000.0 / (hours + 1).coerceAtLeast(1L)
                hours <= 24 -> 10000.0 / (hours + 1).coerceAtLeast(1L)
                hours <= 72 -> 5000.0 / (hours + 1).coerceAtLeast(1L)
                else -> 1000.0 / (hours + 1).coerceAtLeast(1L)
            }
        } ?: run {
            if (task.priority != Priority.HIGH) score *= 0.7
        }

        val durationHours = task.effectiveDurationMinutes / 60.0
        when (strategy) {
            PrioritizationStrategy.URGENT_FIRST -> if (task.endDateConf != null) score *= 1.1
            PrioritizationStrategy.HIGH_PRIORITY_FIRST -> if (task.priority == Priority.HIGH) score *= 1.2 else if (task.priority == Priority.MEDIUM) score *= 1.1
            PrioritizationStrategy.SHORT_TASKS_FIRST -> score += 1000.0 / (durationHours + 0.1).coerceAtLeast(
                0.1
            )

            PrioritizationStrategy.EARLIER_DEADLINES_FIRST -> if (task.endDateConf != null) score *= 1.05
        }

        task.startDateConf.dateTime?.let { start ->
            val now = LocalDateTime.now()
            val hours = Duration.between(now, start).toHours()
            if (hours > 0 && hours < 48) {
                score += 200.0 / (hours + 1).coerceAtLeast(1L)
            } else if (hours <= 0) {
                score += 100.0
            }
        }

        if (planningTask.flags.isOverdue) {
            score *= 5.0
            if (planningTask.flags.constraintDate == today) {
                score += 100000.0
            }
        }

        return maxOf(0.1, score)
    }
}

// --- RecurrenceExpander --- (No significant changes needed, error handling seems okay)
class RecurrenceExpander {

    companion object {
        const val MAX_GENERATED_OCCURRENCES = 1000
        val RRULE_TIME_ZONE_ID: ZoneId = ZoneId.systemDefault()
        val RRULE_TIME_ZONE: TimeZone = TimeZone.getTimeZone(RRULE_TIME_ZONE_ID)
        val RRULE_UNTIL_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneId.of("UTC"))
    }

    fun expandRecurringTask(
        planningTask: PlanningTask,
        scopeStart: LocalDate,
        scopeEnd: LocalDate,
        context: PlanningContext,
    ): List<LocalDateTime> {
        val task = planningTask.task
        Log.d("RecurrenceExpander", "Expanding task ${task.id} (${task.name})")
        val occurrences = mutableListOf<LocalDateTime>()
        val repeatPlan = task.repeatPlan ?: return emptyList()

        val startDateTime = task.startDateConf.dateTime ?: run {
            if (repeatPlan.frequencyType != FrequencyType.NONE) {
                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        "Recurring task missing start date",
                        null,
                        ConflictType.RECURRENCE_ERROR
                    ),
                    task.id
                )
                planningTask.flags.isHardConflict = true
            }
            return emptyList()
        }

        if (repeatPlan.frequencyType == FrequencyType.NONE) {
            if (!startDateTime.toLocalDate().isBefore(scopeStart) && !startDateTime.toLocalDate()
                    .isAfter(scopeEnd)
            ) {
                return listOf(startDateTime)
            } else {
                return emptyList()
            }
        }

        try {
            val rruleString = buildRRuleString(repeatPlan, startDateTime)
            if (rruleString == null) {
                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        "Invalid frequency/unit combination in recurrence",
                        startDateTime,
                        ConflictType.RECURRENCE_ERROR
                    ),
                    task.id
                )
                planningTask.flags.isHardConflict = true
                return emptyList()
            }
            Log.v("RecurrenceExpander", "Task ${task.id} RRULE String: $rruleString")

            val rule = RecurrenceRule(rruleString)

            val startDateTimeDmfs = startDateTime.toDmfsDateTime()
            val startTimestampMillis = startDateTimeDmfs.timestamp
            val iterator: RecurrenceRuleIterator =
                rule.iterator(startTimestampMillis, RRULE_TIME_ZONE)
            val scopeStartDmfs = scopeStart.atTime(LocalTime.MIN).toDmfsDateTime()
            val scopeEndDmfs = scopeEnd.atTime(LocalTime.MAX).toDmfsDateTime()

            if (scopeStartDmfs.timestamp > startDateTimeDmfs.timestamp) {
                iterator.fastForward(scopeStartDmfs)
            }

            var generatedCount = 0
            while (iterator.hasNext() && generatedCount < MAX_GENERATED_OCCURRENCES) {
                val occurrenceMillis = iterator.nextMillis()
                val occurrenceDateTime = DateTime(RRULE_TIME_ZONE, occurrenceMillis)

                if (occurrenceDateTime.timestamp > scopeEndDmfs.timestamp) {
                    break
                }
                if (occurrenceDateTime.timestamp >= scopeStartDmfs.timestamp) {
                    occurrences.add(occurrenceDateTime.toLocalDateTime())
                    generatedCount++
                }
            }

            if (iterator.hasNext() && generatedCount >= MAX_GENERATED_OCCURRENCES) {
                val nextOccurrenceMillis = iterator.peekMillis()
                val nextOccurrenceDateTime = DateTime(RRULE_TIME_ZONE, nextOccurrenceMillis)
                if (nextOccurrenceDateTime.timestamp <= scopeEndDmfs.timestamp) {
                    Log.w(
                        "RecurrenceExpander",
                        "Task ${task.id}: Hit occurrence limit ($MAX_GENERATED_OCCURRENCES) within scope [$scopeStart, $scopeEnd]. Results might be truncated."
                    )
                    context.addInfoItem(
                        InfoItem(
                            task,
                            "Recurrence expansion limited to $MAX_GENERATED_OCCURRENCES occurrences within the search scope",
                            scopeEnd
                        )
                    )
                }
            }

        } catch (e: InvalidRecurrenceRuleException) {
            Log.e(
                "RecurrenceExpander",
                "Invalid RRULE definition or error during parsing/iteration for task ${task.id}: ${e.message}",
                e
            )
            context.addConflict(
                ConflictItem(
                    listOf(task),
                    "Invalid recurrence rule: ${e.message}",
                    startDateTime,
                    ConflictType.RECURRENCE_ERROR
                ),
                task.id
            )
            planningTask.flags.isHardConflict = true
        } catch (e: Exception) {
            Log.e(
                "RecurrenceExpander",
                "Unexpected error expanding recurrence for task ${task.id}",
                e
            )
            context.addConflict(
                ConflictItem(
                    listOf(task),
                    "Error processing recurrence: ${e.message}",
                    startDateTime,
                    ConflictType.RECURRENCE_ERROR
                ),
                task.id
            )
            planningTask.flags.isHardConflict = true
        }

        Log.d("RecurrenceExpander", "Task ${task.id} generated ${occurrences.size} occurrences.")
        return occurrences.distinct()
    }

    private fun buildRRuleString(repeatPlan: RepeatPlan, startDateTime: LocalDateTime): String? {
        val parts = mutableListOf<String>()
        val freqString =
            repeatPlan.frequencyType.toRRuleFreqString(repeatPlan.intervalUnit) ?: return null
        parts.add("FREQ=$freqString")

        val interval = repeatPlan.interval?.coerceAtLeast(1) ?: 1
        if (interval > 1) parts.add("INTERVAL=$interval")
        parts.add("WKST=MO")

        if (repeatPlan.repeatEndDate != null) {
            val untilDateTime = repeatPlan.repeatEndDate.atTime(LocalTime.MAX)
            parts.add("UNTIL=${RRULE_UNTIL_FORMATTER.format(untilDateTime)}")
        } else if (repeatPlan.repeatOccurrences != null) {
            parts.add("COUNT=${repeatPlan.repeatOccurrences.coerceAtLeast(1)}")
        }

        if (repeatPlan.ordinalsOfWeekdays.isNotEmpty()) {
            val byDayValues = repeatPlan.ordinalsOfWeekdays
                .filter { it.ordinal != 0 }
                .map { "${it.ordinal}${it.dayOfWeek.toRRuleWeekDayString()}" }
            if (byDayValues.isNotEmpty()) parts.add("BYDAY=${byDayValues.joinToString(",")}")
        } else if (repeatPlan.selectedDays.isNotEmpty()) {
            val byDayValues = repeatPlan.selectedDays.map { it.toRRuleWeekDayString() }
            if (byDayValues.isNotEmpty()) parts.add("BYDAY=${byDayValues.joinToString(",")}")
        }

        if (repeatPlan.daysOfMonth.isNotEmpty()) {
            val validDays = repeatPlan.daysOfMonth.filter { it in -31..-1 || it in 1..31 }
            if (validDays.isNotEmpty()) parts.add("BYMONTHDAY=${validDays.joinToString(",")}")
        } else if (freqString == "MONTHLY" || freqString == "YEARLY") {
            if (parts.none { it.startsWith("BYDAY=") }) {
                parts.add("BYMONTHDAY=${startDateTime.dayOfMonth}")
            }
        }

        if (repeatPlan.monthsOfYear.isNotEmpty()) {
            val validMonths = repeatPlan.monthsOfYear.filter { it in 1..12 }
            if (validMonths.isNotEmpty()) parts.add("BYMONTH=${validMonths.joinToString(",")}")
        } else if (freqString == "YEARLY") {
            if (parts.none { it.startsWith("BYDAY=") || it.startsWith("BYMONTHDAY=") }) {
                parts.add("BYMONTH=${startDateTime.monthValue}")
            }
        }

        if (repeatPlan.setPos.isNotEmpty()) {
            val validSetPos = repeatPlan.setPos.filter { it != 0 }
            if (validSetPos.isNotEmpty()) parts.add("BYSETPOS=${validSetPos.joinToString(",")}")
        }

        return parts.joinToString(";")
    }

    private fun FrequencyType.toRRuleFreqString(unit: IntervalUnit?): String? = when (this) {
        FrequencyType.DAILY -> "DAILY"; FrequencyType.WEEKLY -> "WEEKLY"
        FrequencyType.MONTHLY -> "MONTHLY"; FrequencyType.YEARLY -> "YEARLY"
        FrequencyType.CUSTOM -> when (unit) {
            IntervalUnit.DAY -> "DAILY"; IntervalUnit.WEEK -> "WEEKLY"
            IntervalUnit.MONTH -> "MONTHLY"; IntervalUnit.YEAR -> "YEARLY"; null -> null
        }

        FrequencyType.NONE -> null
    }

    private fun DayOfWeek.toRRuleWeekDayString(): String = when (this) {
        DayOfWeek.MON -> "MO"; DayOfWeek.TUE -> "TU"; DayOfWeek.WED -> "WE"
        DayOfWeek.THU -> "TH"; DayOfWeek.FRI -> "FR"; DayOfWeek.SAT -> "SA"; DayOfWeek.SUN -> "SU"
    }

    private fun LocalDateTime.toDmfsDateTime(): DateTime {
        val instant = this.atZone(RRULE_TIME_ZONE_ID).toInstant()
        return DateTime(RRULE_TIME_ZONE, instant.toEpochMilli())
    }

    private fun DateTime.toLocalDateTime(): LocalDateTime {
        val zonedDt = if (this.isFloating || this.timeZone == null) {
            this.shiftTimeZone(RRULE_TIME_ZONE)
        } else {
            this.shiftTimeZone(RRULE_TIME_ZONE)
        }
        val instant = Instant.ofEpochMilli(zonedDt.timestamp)
        return LocalDateTime.ofInstant(instant, RRULE_TIME_ZONE_ID)
    }
}

// --- TaskCategorizer --- (No significant changes needed)
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
                // Log.v("Categorizer", "Skipping already handled/conflicted task ${planningTask.id}")
                return@taskLoop
            }
            val task = planningTask.task

            // --- Recurrence Handling (Keep as is) ---
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
                    // Error handled by expander
                } else {
                    if (!taskFitsScope(task, scopeStart, scopeEnd)) return@taskLoop
                    // Log.d("Categorizer", "Recurring task ${task.id} had no occurrences in scope, categorizing based on start/end dates.")
                }
                if (occurrences.isNotEmpty() || planningTask.flags.isHardConflict) return@taskLoop
            }

            // --- Scope Check (Keep as is) ---
            if (!taskFitsScope(task, scopeStart, scopeEnd) && !planningTask.flags.isOverdue) {
                // Log.v("Categorizer", "Skipping task ${task.id} outside scope.")
                return@taskLoop
            }

            // --- Revised Categorization Logic ---
            val startDate = task.startDateConf.dateTime
            val startPeriod = task.startDateConf.dayPeriod
            val endDate = task.endDateConf?.dateTime
            val hasSpecificStartTime =
                startDate != null && startDate.toLocalTime() != LocalTime.MIDNIGHT // Check if time is not just midnight default
            val hasEndDate = endDate != null
            val hasPeriod = startPeriod != DayPeriod.NONE && startPeriod != DayPeriod.ALLDAY

            when {
                // 1. Overdue constrained to Today (Highest precedence after fixed recurrence)
                planningTask.flags.isOverdue && planningTask.flags.constraintDate == today -> {
                    Log.v("Categorizer", "Task ${task.id} - Type: Overdue Today -> DateFlex")
                    dateFlex.computeIfAbsent(today) { mutableListOf() }.add(planningTask)
                }

                // 2. Explicit Start Date & Time, NO End Date (FIXED)
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
                        fullFlex.add(planningTask) // Fallback if date outside scope
                    }
                }

                // 3. Explicit Start Date & Time, WITH End Date (Deadline Flexible)
                startDate != null && hasSpecificStartTime && hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Exact Start Time + End Date -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 4. Start Date + Period, NO End Date (Period Constrained)
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
                // 5. Start Date + Period, WITH End Date (Deadline Flexible - Period is preference)
                startDate != null && hasPeriod && hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Start Date + Period + End Date -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 6. Start Date Only (No Time/Period), NO End Date (Date-Constrained Flexible)
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

                // 7. Start Date Only (No Time/Period), WITH End Date (Deadline Flexible)
                startDate != null && !hasSpecificStartTime && !hasPeriod && hasEndDate -> {
                    Log.v(
                        "Categorizer",
                        "Task ${task.id} - Type: Start Date Only + End Date -> DeadlineFlex"
                    )
                    deadlineFlex.add(planningTask)
                }

                // 8. Only End Date (Deadline Flexible)
                hasEndDate -> { // Catches cases where only end date is set
                    Log.v("Categorizer", "Task ${task.id} - Type: End Date Only -> DeadlineFlex")
                    deadlineFlex.add(planningTask)
                }

                // 9. Fully Flexible (No dates/times/periods)
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

// --- TimelineManager --- (No significant changes needed)
class TimelineManager {
    private val timeline: MutableMap<LocalDate, DaySchedule> = mutableMapOf()
    private val bufferTimeMinutes = 10L
    private val breakTimeMinutes = 15L
    private val minSplitDurationMinutes = 30L

    fun initialize(
        start: LocalDate, end: LocalDate, workStart: LocalTime, workEnd: LocalTime,
        initialPeriodTasks: Map<LocalDate, Map<DayPeriod, List<PlanningTask>>>,
    ) {
        timeline.clear()
        var current = start
        while (!current.isAfter(end)) {
            val daySchedule = DaySchedule(current, workStart, workEnd)
            initializeDayBlocks(daySchedule)
            initialPeriodTasks[current]?.forEach { (period, planningTasks) ->
                daySchedule.pendingPeriodTasks.computeIfAbsent(period) { mutableListOf() }
                    .addAll(planningTasks.map { it.task })
            }
            timeline[current] = daySchedule
            current = current.plusDays(1)
        }
    }

    private fun initializeDayBlocks(daySchedule: DaySchedule) {
        val current = daySchedule.date
        val workStart = daySchedule.workStartTime
        val workEnd = daySchedule.workEndTime
        val startOfDay = current.atStartOfDay()
        val endOfDay = current.plusDays(1).atStartOfDay()
        val workStartDateTime = LocalDateTime.of(current, workStart)
        val workEndDateTime = if (workEnd <= workStart && workStart != workEnd) LocalDateTime.of(
            current.plusDays(1),
            workEnd
        ) else LocalDateTime.of(current, workEnd)
        val blocks = mutableListOf<TimeBlock>()
        var lastTime = startOfDay
        if (workStartDateTime > lastTime) {
            blocks.add(TimeBlock(lastTime, workStartDateTime, Occupancy.OUT_OF_HOURS)); lastTime =
                workStartDateTime
        }
        if (workStartDateTime < workEndDateTime) {
            blocks.add(TimeBlock(lastTime, workEndDateTime, Occupancy.FREE)); lastTime =
                workEndDateTime
        } else if (workStart == workEnd && workStartDateTime == workEndDateTime) {
            blocks.add(TimeBlock(lastTime, endOfDay, Occupancy.FREE)); lastTime = endOfDay
        }
        if (lastTime < endOfDay) {
            blocks.add(TimeBlock(lastTime, endOfDay, Occupancy.OUT_OF_HOURS))
        }
        daySchedule.blocks.addAll(mergeAdjacentBlocks(blocks.sorted()))
    }

    fun getDaySchedule(date: LocalDate): DaySchedule? = timeline[date]
    fun getDates(): Set<LocalDate> = timeline.keys
    fun getPendingPeriodTasks(): Map<LocalDate, Map<DayPeriod, MutableList<Task>>> =
        timeline.mapValues { it.value.pendingPeriodTasks }

    fun findSlot(
        durationNeeded: java.time.Duration,
        minSearchTime: LocalDateTime,
        maxSearchTime: LocalDateTime,
        heuristic: PlacementHeuristic,
        taskIdToIgnore: Int? = null,
    ): TimeBlock? {
        if (durationNeeded <= java.time.Duration.ZERO) return null
        val potentialSlots = mutableListOf<Pair<TimeBlock, TimeBlock>>()
        val searchBlocks = timeline.entries
            .filter {
                it.key >= minSearchTime.toLocalDate() && it.key <= (maxSearchTime.takeIf { t -> t != LocalDateTime.MAX }
                    ?.toLocalDate() ?: LocalDate.MAX)
            }
            .sortedBy { it.key }.flatMap { it.value.blocks }

        searchBlocks
            .filter { it.isFree() && it.end > minSearchTime && it.start < maxSearchTime }
            .forEach { block ->
                val potentialStart = maxOf(block.start, minSearchTime)
                val maxPossibleEndInBlock = minOf(block.end, maxSearchTime)
                if (potentialStart >= maxPossibleEndInBlock) return@forEach
                val availableDuration =
                    java.time.Duration.between(potentialStart, maxPossibleEndInBlock)
                if (availableDuration >= durationNeeded) {
                    val potentialEnd = potentialStart.plus(durationNeeded)
                    if (potentialStart < potentialEnd) {
                        potentialSlots.add(
                            TimeBlock(
                                potentialStart,
                                potentialEnd,
                                Occupancy.FREE
                            ).apply { block.duration } to block)
                    }
                }
            }
        if (potentialSlots.isEmpty()) return null
        return when (heuristic) {
            PlacementHeuristic.EARLIEST_FIT -> potentialSlots.minByOrNull { it.first.start }?.first
            PlacementHeuristic.BEST_FIT -> potentialSlots.minByOrNull { (slot, originalBlock) ->
                (originalBlock.duration - slot.duration).abs().toNanos()
            }?.first ?: potentialSlots.minByOrNull { it.first.start }?.first
        }
    }

    fun placeTask(
        task: Task, startTime: LocalDateTime, endTime: LocalDateTime,
        occupancyType: Occupancy, allowOverwriteFree: Boolean = true,
    ): PlacementResultInternal {
        if (startTime >= endTime) return PlacementResultInternal.Failure("Start time must be before end time")
        val targetDate = startTime.toLocalDate()
        val daySchedule = timeline[targetDate]
            ?: return PlacementResultInternal.Failure("No schedule found for date $targetDate")

        val conflictingBlock = daySchedule.blocks.find { block ->
            block.end > startTime && block.start < endTime &&
                    !(block.isFree() || (block.occupancy == Occupancy.BUFFER && occupancyType != Occupancy.FIXED_TASK)) &&
                    !(block.occupancy == Occupancy.OUT_OF_HOURS && occupancyType == Occupancy.FIXED_TASK) &&
                    block.taskId != task.id
        }
        if (conflictingBlock != null) {
            val reason = when (conflictingBlock.occupancy) {
                Occupancy.FIXED_TASK -> "Overlaps Fixed Task ${conflictingBlock.taskId}"
                Occupancy.PERIOD_TASK -> "Overlaps Period Task ${conflictingBlock.taskId}"
                Occupancy.FLEXIBLE_TASK -> "Overlaps Flexible Task ${conflictingBlock.taskId}"
                Occupancy.OUT_OF_HOURS -> "Cannot schedule non-fixed task outside work hours"
                else -> "Overlaps existing block (${conflictingBlock.occupancy})"
            }
            val type =
                if (conflictingBlock.occupancy == Occupancy.FIXED_TASK) ConflictType.FIXED_VS_FIXED else ConflictType.PLACEMENT_ERROR
            return PlacementResultInternal.Conflict(
                reason,
                conflictingBlock.taskId,
                maxOf(startTime, conflictingBlock.start),
                type
            )
        }

        val originalBlocks = daySchedule.blocks.toList()
        val newBlockList = mutableListOf<TimeBlock>()
        val taskBlock = TimeBlock(startTime, endTime, occupancyType, task.priority, task.id)
        var taskBlockInserted = false
        for (existingBlock in originalBlocks) {
            if (existingBlock.end <= startTime) {
                newBlockList.add(existingBlock); continue
            }
            if (existingBlock.start >= endTime) {
                if (!taskBlockInserted) {
                    newBlockList.add(taskBlock); taskBlockInserted = true
                }
                newBlockList.add(existingBlock); continue
            }
            if (existingBlock.start < startTime) newBlockList.add(existingBlock.copy(end = startTime))
            if (!taskBlockInserted) {
                newBlockList.add(taskBlock); taskBlockInserted = true
            }
            if (existingBlock.end > endTime) newBlockList.add(existingBlock.copy(start = endTime))
        }
        if (!taskBlockInserted) newBlockList.add(taskBlock)
        daySchedule.blocks.clear()
        daySchedule.blocks.addAll(mergeAdjacentBlocks(newBlockList.sorted()))
        return PlacementResultInternal.Success(taskBlock)
    }

    fun addBufferOrBreak(taskEndTime: LocalDateTime, strategy: DayOrganization) {
        val bufferDuration = getBufferDuration(strategy)
        if (bufferDuration <= Duration.ZERO) return
        val bufferStartTime = taskEndTime
        val bufferEndTime = taskEndTime.plus(bufferDuration)
        val targetDate = bufferStartTime.toLocalDate()
        val daySchedule = timeline[targetDate] ?: return
        val workEndTimeForDay =
            if (daySchedule.workEndTime <= daySchedule.workStartTime && daySchedule.workStartTime != daySchedule.workEndTime) {
                LocalDateTime.of(targetDate.plusDays(1), daySchedule.workEndTime)
            } else {
                LocalDateTime.of(targetDate, daySchedule.workEndTime)
            }
        if (bufferEndTime > workEndTimeForDay) return
        val exactSlot = findSlot(
            bufferDuration,
            bufferStartTime,
            bufferEndTime.plusNanos(1),
            PlacementHeuristic.EARLIEST_FIT
        )
        if (exactSlot != null && exactSlot.start == bufferStartTime && exactSlot.end == bufferEndTime) {
            val bufferTask = Task.Builder().id(-1).name("Buffer/Break").build()
            val result = placeTask(
                bufferTask,
                bufferStartTime,
                bufferEndTime,
                Occupancy.BUFFER,
                allowOverwriteFree = true
            )
            if (result is PlacementResultInternal.Success) Log.d(
                "TimelineManager",
                "Added buffer/break at $bufferStartTime on $targetDate"
            )
            else Log.w(
                "TimelineManager",
                "Failed to place buffer/break at $bufferStartTime on $targetDate: $result"
            )
        }
    }

    private fun mergeAdjacentBlocks(sortedBlocks: List<TimeBlock>): List<TimeBlock> {
        if (sortedBlocks.size <= 1) return sortedBlocks.toList()
        val merged = mutableListOf<TimeBlock>()
        var currentMerge = sortedBlocks.first().copy()
        for (i in 1 until sortedBlocks.size) {
            val nextBlock = sortedBlocks[i]
            val canMerge =
                nextBlock.start == currentMerge.end && nextBlock.occupancy == currentMerge.occupancy && !currentMerge.isOccupiedByTask()
            if (canMerge) currentMerge.end = nextBlock.end
            else {
                if (currentMerge.duration > Duration.ZERO) merged.add(currentMerge); currentMerge =
                    nextBlock.copy()
            }
        }
        if (currentMerge.duration > Duration.ZERO) merged.add(currentMerge)
        return merged
    }

    fun getBufferDuration(strategy: DayOrganization): Duration = when (strategy) {
        DayOrganization.FOCUS_URGENT_BUFFER -> Duration.ofMinutes(bufferTimeMinutes)
        DayOrganization.LOOSE_SCHEDULE_BREAKS -> Duration.ofMinutes(breakTimeMinutes)
        else -> Duration.ZERO
    }

    fun getPeriodStartTime(period: DayPeriod): LocalTime = when (period) {
        DayPeriod.MORNING -> LocalTime.of(6, 0); DayPeriod.EVENING -> LocalTime.of(12, 0)
        DayPeriod.NIGHT -> LocalTime.of(18, 0); else -> LocalTime.MIN
    }

    fun getPeriodEndTime(period: DayPeriod): LocalTime = when (period) {
        DayPeriod.MORNING -> LocalTime.of(12, 0); DayPeriod.EVENING -> LocalTime.of(18, 0)
        DayPeriod.NIGHT -> LocalTime.MAX; else -> LocalTime.MAX
    }
}

class TaskPlacer(
    private val taskPrioritizer: TaskPrioritizer, // Inject scorer
) {
    // Constants for placement logic
    private val MIN_SPLIT_DURATION_MINUTES = 30L
    private val FLEXIBLE_PLACEMENT_MAX_ATTEMPTS = 200

    // --- Phase 1: Fixed Tasks (Keep as is) ---
    fun placeFixedTasks(
        timelineManager: TimelineManager,
        fixedOccurrences: List<Pair<PlanningTask, LocalDateTime>>,
        context: PlanningContext,
    ) {
        // ... (Existing placeFixedTasks logic remains the same) ...
        // ... (Including multi-day handling and reportPlacementResult helper) ...
        fixedOccurrences
            .filterNot { context.placedTaskIds.contains(it.first.id) }
            .sortedBy { it.second }
            .forEach fixedLoop@{ (planningTask, occurrenceTime) ->
                val task = planningTask.task
                if (context.placedTaskIds.contains(task.id)) return@fixedLoop

                val duration =
                    Duration.ofMinutes(task.effectiveDurationMinutes.toLong().coerceAtLeast(1))
                if (duration <= Duration.ZERO) {
                    context.addConflict(
                        ConflictItem(
                            listOf(task),
                            "Zero or negative duration",
                            occurrenceTime,
                            ConflictType.ZERO_DURATION
                        ),
                        task.id
                    )
                    return@fixedLoop
                }
                val endTime = occurrenceTime.plus(duration)
                val startDate = occurrenceTime.toLocalDate()
                val endDate = endTime.toLocalDate() // Might be the next day

                // --- Validation for Multi-Day Fixed Tasks ---
                if (startDate != endDate) {
                    val startDaySchedule = timelineManager.getDaySchedule(startDate)
                    val endDaySchedule = timelineManager.getDaySchedule(endDate)

                    if (startDaySchedule == null || endDaySchedule == null) {
                        context.addConflict(
                            ConflictItem(
                                listOf(task),
                                "Fixed task spans midnight but target date(s) outside scope",
                                occurrenceTime,
                                ConflictType.OUTSIDE_SCOPE
                            ), task.id
                        )
                        return@fixedLoop
                    }

                    val endOfDay = startDate.atTime(LocalTime.MAX)
                    val startOfNextDay = endDate.atStartOfDay()

                    val result1 = timelineManager.placeTask(
                        task,
                        occurrenceTime,
                        endOfDay,
                        Occupancy.FIXED_TASK,
                        allowOverwriteFree = false
                    )
                    if (result1 !is PlacementResultInternal.Success) {
                        reportPlacementResult(result1, task, occurrenceTime, context)
                        return@fixedLoop
                    }

                    val result2 = timelineManager.placeTask(
                        task,
                        startOfNextDay,
                        endTime,
                        Occupancy.FIXED_TASK,
                        allowOverwriteFree = false
                    )
                    if (result2 !is PlacementResultInternal.Success) {
                        reportPlacementResult(result2, task, startOfNextDay, context)
                        context.placedTaskIds.add(task.id) // Mark placed because first part succeeded
                        return@fixedLoop
                    }

                    Log.d(
                        "TaskPlacer",
                        "Placed multi-day fixed ${task.id} from $occurrenceTime to $endTime"
                    )
                    context.addScheduledItem(
                        ScheduledTaskItem(
                            task,
                            occurrenceTime.toLocalTime(),
                            endTime.toLocalTime(),
                            startDate
                        ), task.id
                    )
                    checkAndLogOutsideWorkHours(
                        task,
                        occurrenceTime,
                        endTime,
                        startDaySchedule,
                        context
                    )

                } else {
                    // --- Single-Day Fixed Tasks ---
                    val daySchedule = timelineManager.getDaySchedule(startDate)
                    if (daySchedule == null) {
                        context.addConflict(
                            ConflictItem(
                                listOf(task),
                                "Fixed task date outside selected schedule scope",
                                occurrenceTime,
                                ConflictType.OUTSIDE_SCOPE
                            ),
                            task.id
                        )
                        return@fixedLoop
                    }
                    val result = timelineManager.placeTask(
                        task,
                        occurrenceTime,
                        endTime,
                        Occupancy.FIXED_TASK,
                        allowOverwriteFree = false
                    )
                    reportPlacementResult(result, task, occurrenceTime, context) // Use helper
                    if (result is PlacementResultInternal.Success) {
                        context.addScheduledItem(
                            ScheduledTaskItem(
                                task,
                                occurrenceTime.toLocalTime(),
                                endTime.toLocalTime(),
                                startDate
                            ), task.id
                        )
                        checkAndLogOutsideWorkHours(
                            task,
                            occurrenceTime,
                            endTime,
                            daySchedule,
                            context
                        )
                    }
                }
            }
    }

    private fun reportPlacementResult(
        result: PlacementResultInternal,
        task: Task,
        placementTime: LocalDateTime, // Time relevant to this specific placement attempt
        context: PlanningContext,
    ) {
        when (result) {
            is PlacementResultInternal.Success -> { /* Handled by caller */
            }

            is PlacementResultInternal.Conflict -> {
                Log.w(
                    "TaskPlacer",
                    "Conflict placing task ${task.id}: ${result.reason} at ${result.conflictTime}"
                )
                val conflictingTask =
                    result.conflictingTaskId?.let { cId -> context.planningTaskMap[cId]?.task }
                context.addConflict(
                    ConflictItem(
                        listOfNotNull(task, conflictingTask).distinctBy { it.id },
                        result.reason,
                        result.conflictTime,
                        result.type
                    ),
                    task.id // Mark this task as handled (due to conflict)
                )
                if (result.type == ConflictType.FIXED_VS_FIXED && result.conflictingTaskId != null) {
                    context.placedTaskIds.add(result.conflictingTaskId)
                    context.planningTaskMap[result.conflictingTaskId]?.flags?.isHardConflict = true
                }
            }

            is PlacementResultInternal.Failure -> {
                Log.e("TaskPlacer", "Failure placing task ${task.id}: ${result.reason}")
                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        result.reason,
                        placementTime,
                        ConflictType.PLACEMENT_ERROR
                    ),
                    task.id
                )
            }
        }
    }


    // --- Phase 2: Combined Prioritized Placement ---
    fun placePrioritizedTask(
        planningTask: PlanningTask,
        timelineManager: TimelineManager,
        context: PlanningContext,
        input: PlannerInput,
        today: LocalDate,
        scopeStartDate: LocalDate, // Pass scope for fallback window calculation
        scopeEndDate: LocalDate,   // Pass scope for fallback window calculation
    ) {
        val task = planningTask.task
        Log.d("TaskPlacer", "Attempting prioritized placement for Task ${task.id} ('${task.name}')")

        val totalDuration =
            Duration.ofMinutes(task.effectiveDurationMinutes.toLong().coerceAtLeast(1))
        if (totalDuration <= Duration.ZERO) {
            context.addConflict(
                ConflictItem(
                    listOf(task),
                    "Zero duration",
                    task.startDateConf.dateTime,
                    ConflictType.ZERO_DURATION
                ), task.id
            )
            return
        }

        // --- Determine Primary Search Window & Placement Type ---
        var minSearchTime: LocalDateTime
        var maxSearchTime: LocalDateTime
        var placementOccupancy = Occupancy.FLEXIBLE_TASK // Default
        var isFlexible = true // Assume flexible unless proven otherwise
        var primaryFailed = false // Track if the first attempt failed

        val taskStartDateTime = task.startDateConf.dateTime
        val taskStartDate = taskStartDateTime?.toLocalDate()
        val taskStartPeriod = task.startDateConf.dayPeriod
        val taskEndDate = task.endDateConf?.dateTime?.toLocalDate()
            ?: scopeEndDate // Use scope end if no deadline

        // Determine constraints based on task properties and flags
        when {
            // 1. Overdue constrained to Today
            planningTask.flags.isOverdue && planningTask.flags.constraintDate == today -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Overdue Today")
                val daySchedule = timelineManager.getDaySchedule(today)
                minSearchTime = today.atTime(daySchedule?.workStartTime ?: input.workStartTime)
                maxSearchTime = today.plusDays(1).atStartOfDay() // End of today
                isFlexible = false // Must be placed today
            }
            // 2. Period Constraint (on specific date or fallback date)
            taskStartPeriod != DayPeriod.NONE && taskStartPeriod != DayPeriod.ALLDAY -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Period (${taskStartPeriod})")
                val targetDate = taskStartDate ?: today // Use task date or today if null
                val daySchedule = timelineManager.getDaySchedule(targetDate)
                if (daySchedule != null && targetDate >= scopeStartDate && targetDate <= scopeEndDate) {
                    val (periodStart, periodEnd) = calculateEffectivePeriodWindow(
                        daySchedule,
                        taskStartPeriod,
                        timelineManager
                    )
                    minSearchTime = targetDate.atTime(periodStart)
                    maxSearchTime = targetDate.atTime(periodEnd)
                    placementOccupancy = Occupancy.PERIOD_TASK
                    isFlexible = taskEndDate > targetDate // Flexible if deadline allows other days
                } else {
                    // Period date outside scope, treat as fully flexible within scope/deadline
                    Log.v(
                        "TaskPlacer",
                        "Task ${task.id} - Period date outside scope, treating as flexible."
                    )
                    minSearchTime = maxOf(
                        taskStartDateTime ?: scopeStartDate.atStartOfDay(),
                        scopeStartDate.atStartOfDay()
                    )
                    maxSearchTime = minOf(
                        task.endDateConf?.dateTime ?: scopeEndDate.plusDays(1).atStartOfDay(),
                        scopeEndDate.plusDays(1).atStartOfDay()
                    )
                }
            }
            // 3. Date Constraint (Specific date, but no specific time/period)
            taskStartDate != null -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Date Constrained (${taskStartDate})")
                val daySchedule = timelineManager.getDaySchedule(taskStartDate)
                if (daySchedule != null && taskStartDate >= scopeStartDate && taskStartDate <= scopeEndDate) {
                    minSearchTime = taskStartDate.atTime(daySchedule.workStartTime)
                    // Handle overnight work end time correctly for maxSearchTime
                    maxSearchTime =
                        if (daySchedule.workEndTime <= daySchedule.workStartTime && daySchedule.workStartTime != daySchedule.workEndTime) {
                            taskStartDate.plusDays(1).atTime(daySchedule.workEndTime)
                        } else {
                            taskStartDate.atTime(daySchedule.workEndTime)
                        }
                    // Also consider task deadline if it's earlier than work end
                    task.endDateConf?.dateTime?.let { deadline ->
                        maxSearchTime = minOf(maxSearchTime, deadline)
                    }
                    isFlexible =
                        taskEndDate > taskStartDate // Flexible if deadline allows other days
                } else {
                    // Date outside scope, treat as fully flexible within scope/deadline
                    Log.v(
                        "TaskPlacer",
                        "Task ${task.id} - Date constraint outside scope, treating as flexible."
                    )
                    minSearchTime = maxOf(
                        taskStartDateTime ?: scopeStartDate.atStartOfDay(),
                        scopeStartDate.atStartOfDay()
                    )
                    maxSearchTime = minOf(
                        task.endDateConf?.dateTime ?: scopeEndDate.plusDays(1).atStartOfDay(),
                        scopeEndDate.plusDays(1).atStartOfDay()
                    )
                }
            }
            // 4. Deadline or Fully Flexible
            else -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Deadline/Fully Flexible")
                minSearchTime = maxOf(
                    taskStartDateTime ?: scopeStartDate.atStartOfDay(),
                    scopeStartDate.atStartOfDay()
                )
                maxSearchTime = minOf(
                    task.endDateConf?.dateTime ?: scopeEndDate.plusDays(1).atStartOfDay(),
                    scopeEndDate.plusDays(1).atStartOfDay()
                )
            }
        }

        // --- Attempt Placement (Primary Window) ---
        Log.d(
            "TaskPlacer",
            "Task ${task.id}: Primary search window: $minSearchTime -> $maxSearchTime"
        )
        var placementResult = findAndPlaceFlexibleTask(
            timelineManager = timelineManager,
            task = task,
            totalDuration = totalDuration,
            minSearchTime = minSearchTime,
            maxSearchTime = maxSearchTime,
            dayOrganization = input.dayOrganization,
            allowSplitting = input.allowSplitting,
            heuristic = input.flexiblePlacementHeuristic,
            context = context,
            occupancyType = placementOccupancy // Use determined type
        )

        // --- Handle Primary Placement Result ---
        when (placementResult) {
            is PlacementResult.Success -> {
                Log.d("TaskPlacer", "Successfully placed task ${task.id} in primary window.")
                context.placedTaskIds.add(task.id) // Ensure marked placed
                // Info item if placed outside preferred period (fallback from period constraint)
                if (planningTask.flags.failedPeriod) {
                    context.addInfoItem(
                        InfoItem(
                            task,
                            "Placed outside preferred period",
                            placementResult.blocks.firstOrNull()?.start?.toLocalDate()
                        )
                    )
                }
                return // Done with this task
            }

            is PlacementResult.Failure -> {
                Log.w(
                    "TaskPlacer",
                    "Task ${task.id}: Primary placement failed: ${placementResult.reason}"
                )
                primaryFailed = true // Mark primary attempt as failed
                // Continue to fallback if flexible
            }

            is PlacementResult.Conflict -> {
                Log.w(
                    "TaskPlacer",
                    "Task ${task.id}: Primary placement conflict: ${placementResult.reason}"
                )
                // Report conflict immediately if primary attempt causes conflict
                val conflictingTaskObj =
                    placementResult.conflictingTaskId?.let { cId -> context.planningTaskMap[cId]?.task }
                context.addConflict(
                    ConflictItem(
                        listOfNotNull(task, conflictingTaskObj).distinctBy { it.id },
                        placementResult.reason ?: "Overlap",
                        placementResult.conflictTime,
                        placementResult.type
                    ),
                    task.id
                )
                return // Done with this task (conflicted)
            }
        }

        // --- Attempt Fallback Placement (if primary failed AND task is flexible) ---
        if (primaryFailed && isFlexible) {
            Log.i(
                "TaskPlacer",
                "Task ${task.id}: Primary placement failed, attempting fallback placement."
            )
            // Define wider fallback window (entire scope up to deadline)
            val fallbackMinSearch = maxOf(
                task.startDateConf.dateTime ?: scopeStartDate.atStartOfDay(),
                scopeStartDate.atStartOfDay()
            )
            val fallbackMaxSearch = minOf(
                task.endDateConf?.dateTime ?: scopeEndDate.plusDays(1).atStartOfDay(),
                scopeEndDate.plusDays(1).atStartOfDay()
            )

            // Avoid redundant search if primary window was already the full scope
            if (fallbackMinSearch >= minSearchTime && fallbackMaxSearch <= maxSearchTime) {
                Log.d(
                    "TaskPlacer",
                    "Task ${task.id}: Fallback window same as primary, skipping redundant search."
                )
            } else {
                Log.d(
                    "TaskPlacer",
                    "Task ${task.id}: Fallback search window: $fallbackMinSearch -> $fallbackMaxSearch"
                )
                placementResult = findAndPlaceFlexibleTask(
                    timelineManager = timelineManager,
                    task = task,
                    totalDuration = totalDuration,
                    minSearchTime = fallbackMinSearch,
                    maxSearchTime = fallbackMaxSearch,
                    dayOrganization = input.dayOrganization,
                    allowSplitting = input.allowSplitting,
                    heuristic = input.flexiblePlacementHeuristic,
                    context = context,
                    occupancyType = Occupancy.FLEXIBLE_TASK // Fallback is always flexible
                )
            }
        }

        // --- Handle Final Placement Result (after primary and potential fallback) ---
        when (placementResult) {
            is PlacementResult.Success -> {
                // This case should ideally be handled after the primary/fallback attempt succeeds
                // If we reach here, it means fallback succeeded.
                Log.d("TaskPlacer", "Successfully placed task ${task.id} in fallback window.")
                context.placedTaskIds.add(task.id)
                // Add info item indicating fallback placement
                context.addInfoItem(
                    InfoItem(
                        task,
                        "Placed outside preferred time/period",
                        placementResult.blocks.firstOrNull()?.start?.toLocalDate()
                    )
                )
            }

            is PlacementResult.Failure -> {
                // If both primary and fallback (if attempted) failed
                Log.w(
                    "TaskPlacer",
                    "Task ${task.id}: All placement attempts failed: ${placementResult.reason}"
                )
                // Determine the final conflict type based on original constraints
                val finalConflictType = when {
                    planningTask.flags.isOverdue && planningTask.flags.constraintDate == today -> ConflictType.NO_SLOT_ON_DATE // Overdue couldn't fit today
                    taskStartPeriod != DayPeriod.NONE && taskStartPeriod != DayPeriod.ALLDAY -> ConflictType.CANNOT_FIT_PERIOD // Original period failed
                    taskStartDate != null -> ConflictType.NO_SLOT_ON_DATE // Original date failed
                    else -> ConflictType.NO_SLOT_IN_SCOPE // No slot anywhere
                }
                // Report the final conflict
                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        placementResult.reason ?: "No suitable time slot found",
                        minSearchTime,
                        finalConflictType
                    ), // Use original minSearchTime for context
                    task.id
                )
            }

            is PlacementResult.Conflict -> {
                // This case was handled after the primary attempt, should not be reached here
                // unless fallback itself caused a *new* conflict (less likely with findAndPlace logic)
                Log.e(
                    "TaskPlacer",
                    "Task ${task.id}: Fallback placement conflict: ${placementResult.reason}"
                )
                val conflictingTaskObj =
                    placementResult.conflictingTaskId?.let { cId -> context.planningTaskMap[cId]?.task }
                context.addConflict(
                    ConflictItem(
                        listOfNotNull(task, conflictingTaskObj).distinctBy { it.id },
                        placementResult.reason ?: "Overlap",
                        placementResult.conflictTime,
                        placementResult.type
                    ),
                    task.id
                )
            }
        }
    }


    // --- findAndPlaceFlexibleTask (Handles Splitting) ---
    // Add occupancyType parameter
    private fun findAndPlaceFlexibleTask(
        timelineManager: TimelineManager,
        task: Task,
        totalDuration: Duration,
        minSearchTime: LocalDateTime,
        maxSearchTime: LocalDateTime,
        dayOrganization: DayOrganization,
        allowSplitting: Boolean,
        heuristic: PlacementHeuristic,
        context: PlanningContext,
        occupancyType: Occupancy, // Added parameter
    ): PlacementResult {
        var remainingDuration = totalDuration
        val placedBlocksResult = mutableListOf<TimeBlock>()
        var lastBlockEndTime: LocalDateTime? = null
        var attempts = 0
        val buffer = timelineManager.getBufferDuration(dayOrganization)

        while (remainingDuration > Duration.ZERO && attempts < FLEXIBLE_PLACEMENT_MAX_ATTEMPTS) {
            attempts++
            val durationNeeded = if (allowSplitting && remainingDuration < totalDuration) maxOf(
                remainingDuration,
                Duration.ofMinutes(MIN_SPLIT_DURATION_MINUTES)
            ) else remainingDuration
            val earliestNextStart =
                maxOf(minSearchTime, lastBlockEndTime?.plus(buffer) ?: minSearchTime)
            if (earliestNextStart >= maxSearchTime) {
                Log.d(
                    "TaskPlacerFlex",
                    "Task ${task.id}: Search start $earliestNextStart is beyond max time $maxSearchTime. Stopping."
                )
                break
            }

            val bestFit = timelineManager.findSlot(
                durationNeeded,
                earliestNextStart,
                maxSearchTime,
                heuristic,
                task.id
            )
            if (bestFit == null) {
                Log.d(
                    "TaskPlacerFlex",
                    "Task ${task.id}: No slot found for chunk of ${durationNeeded.toMinutes()}m starting after $earliestNextStart."
                )
                break
            }

            val slotStartTime = bestFit.start
            val slotEndTime = bestFit.start.plus(durationNeeded)
            val actualEndTimeInBlock = minOf(bestFit.end, slotEndTime)
            val actualDurationPlaced = Duration.between(slotStartTime, actualEndTimeInBlock)

            if (actualDurationPlaced < Duration.ofMinutes(1)) {
                Log.w(
                    "TaskPlacerFlex",
                    "Task ${task.id}: Skipping near-zero duration chunk placement at ${bestFit.start}. Trying next slot."
                )
                lastBlockEndTime = bestFit.end
                continue
            }

            val placementStartTime = slotStartTime
            val placementEndTime = placementStartTime.plus(actualDurationPlaced)

            if (placementStartTime.toLocalDate() != placementEndTime.toLocalDate()) {
                if (!allowSplitting) {
                    Log.d(
                        "TaskPlacerFlex",
                        "Task ${task.id}: Found slot spans midnight ($placementStartTime -> $placementEndTime), but splitting not allowed. Trying next slot."
                    )
                    lastBlockEndTime = bestFit.end
                    continue
                }

                val endOfDay1 = placementStartTime.toLocalDate().atTime(LocalTime.MAX)
                val startOfDay2 = placementEndTime.toLocalDate().atStartOfDay()

                val result1 = timelineManager.placeTask(
                    task,
                    placementStartTime,
                    endOfDay1,
                    occupancyType
                ) // Use occupancyType
                if (result1 !is PlacementResultInternal.Success) {
                    Log.e(
                        "TaskPlacerFlex",
                        "Task ${task.id}: Failed to place first part of multi-day chunk: ${result1}"
                    )
                    return handlePlacementFailure(result1, task, placementStartTime)
                }

                val result2 = timelineManager.placeTask(
                    task,
                    startOfDay2,
                    placementEndTime,
                    occupancyType
                ) // Use occupancyType
                if (result2 !is PlacementResultInternal.Success) {
                    Log.e(
                        "TaskPlacerFlex",
                        "Task ${task.id}: Failed to place second part of multi-day chunk: ${result2}"
                    )
                    return handlePlacementFailure(result2, task, startOfDay2)
                }

                remainingDuration -= actualDurationPlaced
                placedBlocksResult.add(result1.placedBlock)
                placedBlocksResult.add(result2.placedBlock)
                lastBlockEndTime = placementEndTime
                context.addScheduledItem(
                    ScheduledTaskItem(
                        task,
                        placementStartTime.toLocalTime(),
                        placementEndTime.toLocalTime(),
                        placementStartTime.toLocalDate()
                    ), task.id
                )
                timelineManager.addBufferOrBreak(placementEndTime, dayOrganization)
                Log.d(
                    "TaskPlacerFlex",
                    "Task ${task.id}: Placed multi-day flex chunk ${result1.placedBlock.start.toLocalTime()}-${result2.placedBlock.end.toLocalTime()}. Rem: ${remainingDuration.toMinutes()}m"
                )

            } else {
                // --- Single-Day Placement ---
                val placementInternalResult = timelineManager.placeTask(
                    task,
                    placementStartTime,
                    placementEndTime,
                    occupancyType
                ) // Use occupancyType
                when (placementInternalResult) {
                    is PlacementResultInternal.Success -> {
                        val placedBlock = placementInternalResult.placedBlock
                        val durationPlaced = Duration.between(placedBlock.start, placedBlock.end)
                        if (durationPlaced <= Duration.ZERO) {
                            Log.e(
                                "TaskPlacerFlex",
                                "Task ${task.id}: Internal error - placed zero duration block."
                            )
                            return PlacementResult.Failure(
                                "Internal error placing task chunk (zero duration)",
                                ConflictType.PLACEMENT_ERROR
                            )
                        }
                        remainingDuration -= durationPlaced
                        placedBlocksResult.add(placedBlock)
                        lastBlockEndTime = placedBlock.end
                        context.addScheduledItem(
                            ScheduledTaskItem(
                                task,
                                placedBlock.start.toLocalTime(),
                                placedBlock.end.toLocalTime(),
                                placedBlock.start.toLocalDate()
                            ), task.id
                        )
                        timelineManager.addBufferOrBreak(placedBlock.end, dayOrganization)
                        Log.d(
                            "TaskPlacerFlex",
                            "Task ${task.id}: Placed flex chunk ${placedBlock.start.toLocalTime()}-${placedBlock.end.toLocalTime()} on ${placedBlock.start.toLocalDate()}. Rem: ${remainingDuration.toMinutes()}m"
                        )
                        if (!allowSplitting) {
                            remainingDuration = Duration.ZERO; break
                        }
                    }

                    is PlacementResultInternal.Conflict -> {
                        Log.e(
                            "TaskPlacerFlex",
                            "Task ${task.id}: Unexpected conflict placing chunk: ${placementInternalResult.reason}"
                        )
                        return PlacementResult.Conflict(
                            placementInternalResult.reason,
                            placementInternalResult.conflictingTaskId,
                            placementInternalResult.conflictTime,
                            placementInternalResult.type
                        )
                    }

                    is PlacementResultInternal.Failure -> {
                        Log.e(
                            "TaskPlacerFlex",
                            "Task ${task.id}: Unexpected failure placing chunk: ${placementInternalResult.reason}"
                        )
                        return PlacementResult.Failure(
                            placementInternalResult.reason,
                            ConflictType.PLACEMENT_ERROR
                        )
                    }
                }
            }
        } // End while loop

        if (attempts >= FLEXIBLE_PLACEMENT_MAX_ATTEMPTS) {
            Log.w("TaskPlacer", "Max placement attempts reached for flexible task ${task.id}")
        }
        return when {
            placedBlocksResult.isNotEmpty() && remainingDuration <= Duration.ZERO -> PlacementResult.Success(
                placedBlocksResult
            )

            placedBlocksResult.isNotEmpty() && remainingDuration > Duration.ZERO -> {
                Log.w(
                    "TaskPlacer",
                    "Task ${task.id} placed partially (${(totalDuration - remainingDuration).toMinutes()}m / ${totalDuration.toMinutes()}m). Could not place remaining ${remainingDuration.toMinutes()}m."
                )
                PlacementResult.Failure(
                    "Could only place partially (${(totalDuration - remainingDuration).toMinutes()}m placed)",
                    ConflictType.NO_SLOT_IN_SCOPE
                )
            }

            else -> PlacementResult.Failure(
                "No suitable time slot found",
                ConflictType.NO_SLOT_IN_SCOPE
            )
        }
    }

    // ... (handlePlacementFailure, calculateEffectivePeriodWindow, checkAndLogOutsideWorkHours, format, minOf, maxOf) ...
    private fun handlePlacementFailure(
        result: PlacementResultInternal,
        task: Task,
        placementTime: LocalDateTime,
    ): PlacementResult {
        when (result) {
            is PlacementResultInternal.Conflict -> {
                return PlacementResult.Conflict(
                    result.reason,
                    result.conflictingTaskId,
                    result.conflictTime,
                    result.type
                )
            }

            is PlacementResultInternal.Failure -> {
                return PlacementResult.Failure(result.reason, ConflictType.PLACEMENT_ERROR)
            }

            is PlacementResultInternal.Success -> {
                Log.e(
                    "TaskPlacerFlex",
                    "handlePlacementFailure called with Success result for task ${task.id}"
                )
                return PlacementResult.Failure(
                    "Internal logic error during placement",
                    ConflictType.PLACEMENT_ERROR
                )
            }
        }
    }

    private fun calculateEffectivePeriodWindow(
        daySchedule: DaySchedule,
        period: DayPeriod,
        timelineManager: TimelineManager,
    ): Pair<LocalTime, LocalTime> {
        val periodStartTime = timelineManager.getPeriodStartTime(period)
        val periodEndTime = timelineManager.getPeriodEndTime(period)
        val workStartTime = daySchedule.workStartTime
        val workEndTime = daySchedule.workEndTime
        val effectiveWorkEnd =
            if (workEndTime <= workStartTime && workStartTime != workEndTime) LocalTime.MAX else workEndTime
        val effectiveStart = maxOf(workStartTime, periodStartTime)
        val effectiveEnd = minOf(effectiveWorkEnd, periodEndTime)
        return if (effectiveStart >= effectiveEnd) (LocalTime.MIDNIGHT to LocalTime.MIDNIGHT) else (effectiveStart to effectiveEnd)
    }

    private fun checkAndLogOutsideWorkHours(
        task: Task,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        daySchedule: DaySchedule,
        context: PlanningContext,
    ) {
        val startsBeforeWork =
            startTime.toLocalTime() < daySchedule.workStartTime && daySchedule.workStartTime != daySchedule.workEndTime
        val endsAfterWork =
            if (daySchedule.workEndTime <= daySchedule.workStartTime && daySchedule.workStartTime != daySchedule.workEndTime) {
                endTime.toLocalDate() > daySchedule.date || endTime.toLocalTime() > daySchedule.workEndTime
            } else {
                endTime.toLocalTime() > daySchedule.workEndTime && endTime.toLocalTime() != LocalTime.MIDNIGHT
            }
        if (startsBeforeWork || endsAfterWork) {
            context.addInfoItem(
                InfoItem(
                    task,
                    "Placed outside defined work hours",
                    daySchedule.date
                )
            )
            Log.i(
                "TaskPlacer",
                "Fixed task ${task.id} placed partially/fully outside work hours (${daySchedule.workStartTime}-${daySchedule.workEndTime}) on ${daySchedule.date} at $startTime - $endTime"
            )
        }
    }

    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
    private fun minOf(a: LocalTime, b: LocalTime): LocalTime = if (a < b) a else b
    private fun maxOf(a: LocalTime, b: LocalTime): LocalTime = if (a > b) a else b
}
