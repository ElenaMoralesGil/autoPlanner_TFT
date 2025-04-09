package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.IntervalUnit
import com.elena.autoplanner.domain.models.PlannerInput
import com.elena.autoplanner.domain.models.PlannerOutput
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters


private data class TimeBlock(
    val start: LocalDateTime,
    var end: LocalDateTime,
    var isFree: Boolean = true,
    var taskId: Int? = null,
) : Comparable<TimeBlock> {
    val duration: Duration get() = Duration.between(start, end)
    override fun compareTo(other: TimeBlock): Int = this.start.compareTo(other.start)
}


class GeneratePlanUseCase {


    private val BUFFER_TIME_MINUTES = 10L
    private val BREAK_TIME_MINUTES = 15L
    private val MIN_SPLIT_DURATION_MINUTES = 15L

    suspend operator fun invoke(input: PlannerInput): PlannerOutput =
        withContext(Dispatchers.Default) {
            println("--- Starting Plan Generation ---")
            println("Input Strategy: ${input.prioritizationStrategy}") // Log single strategy

            val today = LocalDate.now()
            val (scheduleStartDate, scheduleEndDate) = determineSchedulingWindow(
                input.scheduleScope,
                today
            )
            println("Scheduling Window: $scheduleStartDate to $scheduleEndDate")

            val allRelevantTasks = input.tasks.filter { task ->
                !task.isCompleted && taskFitsScope(task, scheduleStartDate, scheduleEndDate)
            }

            val fixedOccurrences = mutableListOf<Pair<Task, LocalDateTime>>()
            val flexibleTasks = mutableListOf<Task>()

            allRelevantTasks.forEach { task ->
                if (task.startDateConf.dateTime != null && task.startDateConf.dayPeriod == DayPeriod.NONE) {
                    fixedOccurrences.add(task to task.startDateConf.dateTime!!)
                } else if (task.repeatPlan != null && task.repeatPlan.frequencyType != FrequencyType.NONE) {
                    val occurrences = expandRecurringTask(
                        task,
                        scheduleStartDate,
                        scheduleEndDate,
                        input.workStartTime
                    )
                    occurrences.forEach { occurrenceTime ->
                        fixedOccurrences.add(task to occurrenceTime)
                    }
                } else {
                    flexibleTasks.add(task)
                }
            }
            println("Total Relevant Tasks: ${allRelevantTasks.size}")
            println("Fixed Occurrences: ${fixedOccurrences.size}")
            println("Flexible Tasks: ${flexibleTasks.size}")

            val timeline = createTimeline(
                scheduleStartDate,
                scheduleEndDate,
                input.workStartTime,
                input.workEndTime
            )
            println("Initial Timeline Blocks: ${timeline.values.sumOf { it.size }}")

            val fixedPlacementConflicts = mutableListOf<ConflictItem>()
            val successfullyPlacedFixed = mutableSetOf<Pair<Int, LocalDateTime>>()

            fixedOccurrences.sortedBy { it.second }.forEach { (task, occurrenceTime) ->
                val duration = Duration.ofMinutes(task.effectiveDurationMinutes.toLong())
                val endTime = occurrenceTime.plus(duration)

                // --- Improved boundary checks for fixed tasks ---
                if (occurrenceTime.toLocalDate() !in scheduleStartDate..scheduleEndDate ||
                    occurrenceTime.toLocalTime() < input.workStartTime ||
                    endTime.toLocalTime()
                        .let { it > input.workEndTime && it != LocalTime.MIDNIGHT } || // Allow ending exactly at midnight
                    endTime.toLocalDate() != occurrenceTime.toLocalDate() // Doesn't span midnight into next day (unless it ends exactly at 00:00)
                ) {
                    println("Fixed Occurrence Skipped (Outside Window/Hours): Task ${task.id} at $occurrenceTime")
                    if (task.endDateConf?.dateTime?.let { it.toLocalDate() >= scheduleStartDate && it.toLocalDate() <= scheduleEndDate } == true) { // Check if deadline was within scope
                        fixedPlacementConflicts.add(
                            ConflictItem(
                                listOf(task),
                                "Fixed occurrence at $occurrenceTime falls outside working hours or scope"
                            )
                        )
                    }
                    return@forEach // Skip this fixed occurrence
                }


                val placementResult = placeTaskOnTimeline(
                    timeline,
                    task,
                    occurrenceTime,
                    duration,
                    allowSplitting = false
                )

                when (placementResult) {
                    is PlacementResult.Success -> {
                        println("Placed Fixed: Task ${task.id} at $occurrenceTime")
                        successfullyPlacedFixed.add(task.id to occurrenceTime)
                    }

                    is PlacementResult.Conflict -> {
                        println("Conflict placing Fixed: Task ${task.id} at $occurrenceTime (Overlap with Task ${placementResult.conflictingTaskId})")
                        val conflictingTask =
                            input.tasks.find { it.id == placementResult.conflictingTaskId }
                        fixedPlacementConflicts.add(
                            ConflictItem(
                                listOfNotNull(
                                    task,
                                    conflictingTask
                                ), "Fixed time slot conflict at $occurrenceTime"
                            )
                        )
                    }

                    is PlacementResult.Failure -> {
                        println("Failed placing Fixed (Reason: ${placementResult.reason}): Task ${task.id} at $occurrenceTime")
                        fixedPlacementConflicts.add(
                            ConflictItem(
                                listOf(task),
                                placementResult.reason
                                    ?: "Unknown error placing fixed task at $occurrenceTime"
                            )
                        )
                    }
                }
            }
            println("Fixed Placement Conflicts: ${fixedPlacementConflicts.size}")

            // Score and Sort Flexible Tasks using the SINGLE strategy
            val scoredFlexibleTasks = flexibleTasks
                .filterNot { task -> successfullyPlacedFixed.any { it.first == task.id } }
                .map { task ->
                    task to calculateRobustScore(
                        task,
                        input.prioritizationStrategy,
                        today
                    )
                } // Pass single strategy
                .sortedByDescending { it.second }
            println(
                "Scored Flexible Tasks for Scheduling: ${
                    scoredFlexibleTasks.map {
                        "${it.first.name}(${it.first.id}) Score:${
                            it.second.format(
                                2
                            )
                        }"
                    }
                }"
            )

            val placementResults = mutableMapOf<Int, PlacementResult>()

            for ((task, score) in scoredFlexibleTasks) {
                println(
                    "Attempting to place flexible task ${task.id} ('${task.name}') Score: ${
                        score.format(
                            2
                        )
                    }"
                )
                val duration = Duration.ofMinutes(task.effectiveDurationMinutes.toLong())
                val minStart =
                    task.startDateConf.dateTime // Use original task start as earliest possible time
                val maxEnd = task.endDateConf?.dateTime

                val result = findAndPlaceTask(
                    timeline = timeline,
                    task = task,
                    totalDuration = duration,
                    minStartTime = minStart,
                    maxEndTime = maxEnd,
                    workStartTime = input.workStartTime,
                    workEndTime = input.workEndTime,
                    dayOrganization = input.dayOrganization,
                    allowSplitting = input.showSubtasksWithDuration
                )
                placementResults[task.id] = result
            }

            // Consolidate Results
            val scheduledTasksMap = mutableMapOf<LocalDate, MutableList<ScheduledTaskItem>>()
            val unresolvedConflicts = mutableListOf<ConflictItem>()
            unresolvedConflicts.addAll(fixedPlacementConflicts) // Start with fixed conflicts
            val unresolvedExpired = mutableListOf<Task>()

            placementResults.forEach { (taskId, result) ->
                val task = flexibleTasks.find { it.id == taskId } ?: return@forEach
                when (result) {
                    is PlacementResult.Success -> {
                        result.blocks.forEach { block ->
                            scheduledTasksMap
                                .computeIfAbsent(block.start.toLocalDate()) { mutableListOf() }
                                .add(
                                    ScheduledTaskItem(
                                        task,
                                        block.start.toLocalTime(),
                                        block.end.toLocalTime(),
                                        block.start.toLocalDate()
                                    )
                                )
                        }
                    }

                    is PlacementResult.Conflict -> {
                        println("Conflict placing Flexible Task ${task.id}: ${result.reason}")
                        val conflictingTask = input.tasks.find { it.id == result.conflictingTaskId }
                        unresolvedConflicts.add(
                            ConflictItem(
                                listOfNotNull(task, conflictingTask),
                                result.reason ?: "Scheduling conflict"
                            )
                        )
                    }

                    is PlacementResult.Failure -> {
                        println("Failure placing Flexible Task ${task.id}: ${result.reason}")
                        // Check if it expired *within the scheduling window* or before
                        val deadline = task.endDateConf?.dateTime
                        val deadlineDate = deadline?.toLocalDate()
                        // Consider expired if deadline exists, is before now, AND was within the scheduling window the user requested
                        if (deadline != null && deadline.isBefore(LocalDateTime.now()) && deadlineDate != null && !deadlineDate.isBefore(
                                scheduleStartDate
                            ) && !deadlineDate.isAfter(scheduleEndDate)
                        ) {
                            if (unresolvedExpired.none { it.id == task.id }) unresolvedExpired.add(
                                task
                            )
                        } else {
                            // It didn't fit, but isn't necessarily expired relative to the scope
                            unresolvedConflicts.add(
                                ConflictItem(
                                    listOf(task),
                                    result.reason ?: "Could not find suitable time slot"
                                )
                            )
                        }
                    }
                }
            }

            // Add tasks that were already expired *before* planning started for this scope AND fit the scope originally
            flexibleTasks.forEach { task ->
                if (!placementResults.containsKey(task.id) && task.isExpired() && taskFitsScope(
                        task,
                        scheduleStartDate,
                        scheduleEndDate
                    )
                ) {
                    if (unresolvedExpired.none { it.id == task.id }) {
                        println("Adding pre-expired task to unresolved: ${task.id} ('${task.name}')")
                        unresolvedExpired.add(task)
                    }
                }
            }


            println("--- Plan Generation Complete ---")
            println("Scheduled Blocks: ${scheduledTasksMap.values.sumOf { it.size }}")
            println(
                "Unresolved Expired: ${unresolvedExpired.distinctBy { it.id }.size} tasks ${
                    unresolvedExpired.distinctBy { it.id }.map { "[${it.id}]${it.name}" }
                }"
            )
            println("Unresolved Conflicts: ${unresolvedConflicts.size} items")

            scheduledTasksMap.values.forEach { it.sortBy { item -> item.scheduledStartTime } }

            PlannerOutput(
                scheduledTasks = scheduledTasksMap,
                unresolvedExpired = unresolvedExpired.distinctBy { it.id },
                unresolvedConflicts = unresolvedConflicts.distinctBy { it.hashCode() } // Attempt to remove duplicate conflict reports
            )
        }
    // --- **** END UPDATE **** ---

    // --- Helper Functions (determineSchedulingWindow, taskFitsScope, createTimeline, expandRecurringTask remain mostly the same, minor scope logic tweak) ---
    private fun determineSchedulingWindow(
        scope: ScheduleScope,
        today: LocalDate,
    ): Pair<LocalDate, LocalDate> {
        return when (scope) {
            ScheduleScope.TODAY -> today to today
            ScheduleScope.TOMORROW -> today.plusDays(1) to today.plusDays(1)
            ScheduleScope.THIS_WEEK -> {
                // Start from today, end at the end of the current week (Sunday)
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                today to endOfWeek
            }
        }
    }

    private fun taskFitsScope(task: Task, scopeStart: LocalDate, scopeEnd: LocalDate): Boolean {
        val taskStart = task.startDateConf.dateTime?.toLocalDate()
        val taskEnd = task.endDateConf?.dateTime?.toLocalDate()

        // Task is relevant if it starts within the scope OR ends within the scope
        // OR starts before and ends after (spans the scope)
        // OR has no specific date (always consider, might be filtered by priority/deadline later)
        return when {
            taskStart == null && taskEnd == null -> true // No dates, always consider? Or filter elsewhere?
            taskStart != null && taskStart.isAfter(scopeEnd) -> false // Starts after scope ends
            taskEnd != null && taskEnd.isBefore(scopeStart) -> false // Ends before scope starts
            else -> true // Overlaps or fits within
        }
    }

    private fun createTimeline(
        start: LocalDate,
        end: LocalDate,
        workStart: LocalTime,
        workEnd: LocalTime,
    ): MutableMap<LocalDate, MutableList<TimeBlock>> {
        val timeline = mutableMapOf<LocalDate, MutableList<TimeBlock>>()
        var current = start
        while (!current.isAfter(end)) {
            val dayBlocks = mutableListOf<TimeBlock>()
            val startDateTime = LocalDateTime.of(current, workStart)
            // Handle overnight work end time correctly
            val endDateTime = if (workEnd <= workStart) {
                LocalDateTime.of(current.plusDays(1), workEnd) // End time is on the next day
            } else {
                LocalDateTime.of(current, workEnd)
            }

            // Create initial free block only if start is before end
            if (startDateTime < endDateTime) {
                dayBlocks.add(TimeBlock(startDateTime, endDateTime, isFree = true))
            } else if (workStart == workEnd) { // Handle 24h availability case
                dayBlocks.add(TimeBlock(startDateTime, startDateTime.plusDays(1), isFree = true))
            }


            timeline[current] = dayBlocks
            current = current.plusDays(1)
        }
        return timeline
    }

    private fun expandRecurringTask(
        task: Task,
        scopeStart: LocalDate,
        scopeEnd: LocalDate,
        defaultTime: LocalTime,
    ): List<LocalDateTime> {
        // (Keep the improved implementation from the previous response)
        val occurrences = mutableListOf<LocalDateTime>()
        val repeatPlan = task.repeatPlan ?: return emptyList()
        // Start checking from the task's start date OR the scope start, whichever is later
        // to avoid generating occurrences too far in the past.
        var nextOccurrenceDate =
            (task.startDateConf.dateTime?.toLocalDate() ?: scopeStart).coerceAtLeast(scopeStart)
        val occurrenceTime = task.startDateConf.dateTime?.toLocalTime() ?: defaultTime

        var iterations = 0
        val maxIterations = 365

        while (!nextOccurrenceDate.isAfter(scopeEnd) && iterations < maxIterations) {
            iterations++
            // Only add if the date itself is within the scope (already handled by loop condition, but safer)
            if (!nextOccurrenceDate.isBefore(scopeStart)) {
                val fitsDayOfWeek = when (repeatPlan.frequencyType) {
                    FrequencyType.WEEKLY -> repeatPlan.selectedDays.isEmpty() || repeatPlan.selectedDays.any { it.name == nextOccurrenceDate.dayOfWeek.name } // Match DayOfWeek enum correctly
                    // TODO: Add checks for MONTHLY, YEARLY based on day of month/year etc.
                    else -> true
                }

                if (fitsDayOfWeek) {
                    occurrences.add(LocalDateTime.of(nextOccurrenceDate, occurrenceTime))
                }
            }

            // Calculate next date based on frequency
            val intervalValue = repeatPlan.interval?.toLong() ?: 1L // Default interval to 1 if null
            nextOccurrenceDate = try {
                when (repeatPlan.frequencyType) {
                    FrequencyType.DAILY -> nextOccurrenceDate.plusDays(intervalValue)
                    FrequencyType.WEEKLY -> nextOccurrenceDate.plusWeeks(intervalValue)
                    FrequencyType.MONTHLY -> nextOccurrenceDate.plusMonths(intervalValue)
                    FrequencyType.YEARLY -> nextOccurrenceDate.plusYears(intervalValue)
                    FrequencyType.CUSTOM -> {
                        when (repeatPlan.intervalUnit) {
                            IntervalUnit.DAY -> nextOccurrenceDate.plusDays(intervalValue)
                            IntervalUnit.WEEK -> nextOccurrenceDate.plusWeeks(intervalValue)
                            IntervalUnit.MONTH -> nextOccurrenceDate.plusMonths(intervalValue)
                            null -> nextOccurrenceDate.plusDays(1) // Fallback if unit is missing
                        }
                    }

                    FrequencyType.NONE -> break // Stop if frequency is None
                }
            } catch (e: Exception) {
                println("Error calculating next recurrence date for task ${task.id}: ${e.message}")
                break // Stop recurrence on error
            }
        }

        if (iterations >= maxIterations) {
            println("Warning: Max iterations reached expanding recurring task ${task.id}. Potential infinite loop.")
        }

        println("Expanded Task ${task.id} (${repeatPlan.frequencyType}): Found ${occurrences.size} occurrences in scope $scopeStart - $scopeEnd")
        return occurrences
    }

    // Attempts to place a task (potentially splitting it) and updates the timeline
    private fun findAndPlaceTask(
        timeline: MutableMap<LocalDate, MutableList<TimeBlock>>,
        task: Task,
        totalDuration: Duration,
        minStartTime: LocalDateTime?,
        maxEndTime: LocalDateTime?,
        workStartTime: LocalTime,
        workEndTime: LocalTime,
        dayOrganization: DayOrganization,
        allowSplitting: Boolean,
    ): PlacementResult {
        var remainingDuration = totalDuration
        val placedBlocks = mutableListOf<TimeBlock>()
        var attemptCount = 0 // Prevent infinite loops

        while (remainingDuration > Duration.ZERO && attemptCount < 100) { // Loop until placed or too many attempts
            attemptCount++
            val durationNeeded = if (allowSplitting && remainingDuration < totalDuration) {
                // If splitting, ensure parts are reasonably sized
                maxOf(remainingDuration, Duration.ofMinutes(MIN_SPLIT_DURATION_MINUTES))
            } else {
                remainingDuration
            }

            // Find the best *next* available slot based on strategy
            val bestFit = findNextAvailableSlot(
                timeline = timeline,
                durationNeeded = durationNeeded,
                taskId = task.id, // For logging/debug
                minStartTime = maxOf(
                    minStartTime ?: LocalDateTime.MIN,
                    placedBlocks.lastOrNull()?.end ?: LocalDateTime.MIN
                ), // Start after last placed block or task minStart
                maxEndTime = maxEndTime,
                workStartTime = workStartTime,
                workEndTime = workEndTime,
                preferredDayPeriod = task.startDateConf.dayPeriod.takeIf { it != DayPeriod.NONE },
                strategy = dayOrganization
            )

            if (bestFit == null) {
                println("Task ${task.id}: No suitable slot found for remaining ${remainingDuration.toMinutes()} mins.")
                // If some parts were placed, return success with partial placement, otherwise failure
                return if (placedBlocks.isNotEmpty()) PlacementResult.Success(placedBlocks)
                else PlacementResult.Failure("No suitable time slot found")
            }

            // Try to place in the found slot
            val actualStartTime = bestFit.start
            val actualEndTime = actualStartTime.plus(durationNeeded) // End time for this chunk
            val placementResult =
                placeTaskOnTimeline(timeline, task, actualStartTime, durationNeeded, allowSplitting)

            when (placementResult) {
                is PlacementResult.Success -> {
                    val placedBlock =
                        placementResult.blocks.first() // Assuming success places one block at a time here
                    placedBlocks.add(placedBlock)
                    remainingDuration -= Duration.between(
                        placedBlock.start,
                        placedBlock.end
                    ) // Reduce remaining duration
                    println("Task ${task.id}: Placed part at ${placedBlock.start} - ${placedBlock.end}. Remaining: ${remainingDuration.toMinutes()} mins")

                    // Apply Buffer/Break *after* successful placement for this block
                    addBufferOrBreak(timeline, placedBlock.end, dayOrganization)
                }

                else -> {
                    // Should ideally not happen if findNextAvailableSlot worked correctly, but handle defensively
                    println("Task ${task.id}: Failed to place on timeline even after finding slot at ${bestFit.start}. Result: $placementResult")
                    return if (placedBlocks.isNotEmpty()) PlacementResult.Success(placedBlocks) // Return partial success
                    else PlacementResult.Failure(
                        "Timeline placement failed unexpectedly after finding slot",
                        (placementResult as? PlacementResult.Conflict)?.conflictingTaskId
                    )
                }
            }

            // Break loop if splitting wasn't allowed and we placed something
            if (!allowSplitting && placedBlocks.isNotEmpty()) {
                break
            }
        } // End while remainingDuration > ZERO

        return if (placedBlocks.isNotEmpty()) {
            if (remainingDuration > Duration.ZERO) {
                println("Task ${task.id}: Partially placed. ${remainingDuration.toMinutes()} mins remaining (Couldn't fit).")
                // Decide if partial placement is a "success" or should be marked as conflict/failure
                PlacementResult.Success(placedBlocks) // Treat as success for now, user sees duration difference
            } else {
                PlacementResult.Success(placedBlocks) // Fully placed
            }
        } else {
            PlacementResult.Failure("Could not place task ${task.id}") // Never placed any part
        }
    }


    // Finds the next best free slot according to strategy
    private fun findNextAvailableSlot(
        timeline: Map<LocalDate, List<TimeBlock>>,
        durationNeeded: Duration,
        taskId: Int, // for logging
        minStartTime: LocalDateTime?,
        maxEndTime: LocalDateTime?,
        workStartTime: LocalTime,
        workEndTime: LocalTime,
        preferredDayPeriod: DayPeriod?,
        strategy: DayOrganization,
    ): TimeBlock? {
        println("Task $taskId: Searching for slot of ${durationNeeded.toMinutes()} mins. MinStart: $minStartTime, MaxEnd: $maxEndTime, PrefPeriod: $preferredDayPeriod, Strategy: $strategy")
        val searchStart = minStartTime ?: LocalDateTime.MIN

        // Iterate through dates in the timeline's keys, sorted
        for (date in timeline.keys.sorted()) {
            val dayBlocks = timeline[date] ?: continue

            val potentialSlots = dayBlocks.filter { block ->
                block.isFree &&
                        block.duration >= durationNeeded &&
                        block.start < (maxEndTime
                    ?: LocalDateTime.MAX) && // Slot must start before task must end
                        block.end > searchStart // Slot must end after we can start searching
            }
                .mapNotNull { block ->
                    // Calculate the actual possible start/end within this block respecting constraints
                    val actualStart = maxOf(block.start, searchStart)
                    val actualEnd = actualStart.plus(durationNeeded)

                    // Check if this fits within the block and respects maxEndTime
                    if (actualEnd <= block.end && actualEnd <= (maxEndTime ?: LocalDateTime.MAX)) {
                        // Check work hours for the *actual* start/end
                        if (actualStart.toLocalTime() >= workStartTime && actualEnd.toLocalTime() <= workEndTime) {
                            TimeBlock(
                                actualStart,
                                actualEnd,
                                true
                            ) // Return a potential placement block
                        } else null
                    } else null
                }

            // Apply strategy to choose from potential slots on this day
            if (potentialSlots.isNotEmpty()) {
                val chosenSlot = when (strategy) {
                    // TODO: Add preference for DayPeriod if specified
                    DayOrganization.MAXIMIZE_PRODUCTIVITY -> potentialSlots.minByOrNull { it.start } // Earliest start
                    DayOrganization.FOCUS_URGENT_BUFFER, DayOrganization.LOOSE_SCHEDULE_BREAKS -> potentialSlots.minByOrNull { it.start } // For now, also use earliest
                    // else -> potentialSlots.firstOrNull()
                }
                if (chosenSlot != null) {
                    println("Task $taskId: Found potential slot on $date: ${chosenSlot.start} - ${chosenSlot.end}")
                    return chosenSlot
                }
            }
        }
        println("Task $taskId: No suitable slot found in timeline.")
        return null // No suitable slot found across all days
    }

    // Modifies the timeline to mark a task's placement. Returns result.
    private sealed class PlacementResult {
        data class Success(val blocks: List<TimeBlock>) :
            PlacementResult() // Can contain multiple blocks if split

        data class Conflict(val reason: String? = null, val conflictingTaskId: Int?) :
            PlacementResult()

        data class Failure(val reason: String? = null, val conflictingTaskId: Int? = null) :
            PlacementResult()
    }

    private fun placeTaskOnTimeline(
        timeline: MutableMap<LocalDate, MutableList<TimeBlock>>,
        task: Task,
        startTime: LocalDateTime,
        duration: Duration,
        allowSplitting: Boolean, // Note: This function places *one* block, splitting logic is in findAndPlaceTask
    ): PlacementResult {
        val date = startTime.toLocalDate()
        val dayBlocks =
            timeline[date] ?: return PlacementResult.Failure("Date not found in timeline")
        val endTime = startTime.plus(duration)

        // Find the free block that contains this exact start time
        val targetBlockIndex =
            dayBlocks.indexOfFirst { it.isFree && it.start <= startTime && it.end >= endTime }

        if (targetBlockIndex == -1) {
            // Check for partial overlap conflicts (more robust check)
            val conflictingBlock =
                dayBlocks.find { !it.isFree && it.start < endTime && it.end > startTime }
            if (conflictingBlock != null) {
                return PlacementResult.Conflict(
                    "Overlaps with existing Task ${conflictingBlock.taskId}",
                    conflictingBlock.taskId
                )
            }
            return PlacementResult.Failure("No containing free block found or doesn't fit")
        }

        val freeBlock = dayBlocks[targetBlockIndex]

        // Remove the old free block
        dayBlocks.removeAt(targetBlockIndex)

        // Add the new occupied block
        val taskBlock = TimeBlock(startTime, endTime, isFree = false, taskId = task.id)
        dayBlocks.add(taskBlock)

        // Add back remaining free time before the task, if any
        if (freeBlock.start < startTime) {
            dayBlocks.add(TimeBlock(freeBlock.start, startTime, isFree = true))
        }
        // Add back remaining free time after the task, if any
        if (freeBlock.end > endTime) {
            dayBlocks.add(TimeBlock(endTime, freeBlock.end, isFree = true))
        }

        // Keep the list sorted
        dayBlocks.sort()

        return PlacementResult.Success(listOf(taskBlock))
    }

    // Adds buffer or break after a task placement if strategy requires
    private fun addBufferOrBreak(
        timeline: MutableMap<LocalDate, MutableList<TimeBlock>>,
        taskEndTime: LocalDateTime,
        strategy: DayOrganization,
    ) {
        val bufferDuration = when (strategy) {
            DayOrganization.FOCUS_URGENT_BUFFER -> Duration.ofMinutes(BUFFER_TIME_MINUTES)
            DayOrganization.LOOSE_SCHEDULE_BREAKS -> Duration.ofMinutes(BREAK_TIME_MINUTES)
            else -> Duration.ZERO
        }

        if (bufferDuration > Duration.ZERO) {
            val date = taskEndTime.toLocalDate()
            val dayBlocks = timeline[date] ?: return

            // Find the free block immediately following the task
            val nextFreeBlockIndex = dayBlocks.indexOfFirst { it.isFree && it.start == taskEndTime }
            if (nextFreeBlockIndex != -1) {
                val freeBlock = dayBlocks[nextFreeBlockIndex]
                if (freeBlock.duration > bufferDuration) { // Only add buffer if there's enough space
                    val bufferEnd = freeBlock.start.plus(bufferDuration)
                    // Remove original free block
                    dayBlocks.removeAt(nextFreeBlockIndex)
                    // Add buffer block (marked as not free)
                    dayBlocks.add(
                        TimeBlock(
                            freeBlock.start,
                            bufferEnd,
                            isFree = false,
                            taskId = null
                        )
                    ) // Buffer block
                    // Add remaining free time
                    dayBlocks.add(TimeBlock(bufferEnd, freeBlock.end, isFree = true))
                    dayBlocks.sort()
                    println("Added ${bufferDuration.toMinutes()} min buffer/break after task ending at $taskEndTime")
                }
            }
        }
    }


    // Calculate a more robust score
    private fun calculateRobustScore(
        task: Task,
        strategy: PrioritizationStrategy,
        today: LocalDate,
    ): Double {
        var score = 0.0

        // Base Priority Score
        score += when (task.priority) {
            Priority.HIGH -> 10000.0
            Priority.MEDIUM -> 5000.0
            Priority.LOW -> 1000.0
            Priority.NONE -> 100.0
        }

        // Deadline Urgency
        task.endDateConf?.dateTime?.let { deadline ->
            val hoursUntilDeadline = Duration.between(LocalDateTime.now(), deadline).toHours()
            score += when {
                hoursUntilDeadline < 0 -> 50000.0 // Overdue
                hoursUntilDeadline <= 8 -> 20000.0 / (hoursUntilDeadline + 1).coerceAtLeast(1L) // Very urgent (within 8 hours)
                hoursUntilDeadline <= 24 -> 10000.0 / (hoursUntilDeadline + 1).coerceAtLeast(1L) // Urgent (within 1 day)
                hoursUntilDeadline <= 72 -> 5000.0 / (hoursUntilDeadline + 1).coerceAtLeast(1L)  // Less urgent (within 3 days)
                else -> 1000.0 / (hoursUntilDeadline + 1).coerceAtLeast(1L) // Normal deadline pressure
            }
        } ?: run {
            // Task without deadline - lower urgency unless HIGH priority
            if (task.priority != Priority.HIGH) score *= 0.7 // Reduce score more significantly
        }

        // Apply the SINGLE selected User Strategy Adjustment
        val taskDurationHours = task.effectiveDurationMinutes / 60.0
        when (strategy) {
            PrioritizationStrategy.URGENT_FIRST -> {
                // Slightly boost tasks with deadlines vs no deadline
                if (task.endDateConf != null) score *= 1.1
            }

            PrioritizationStrategy.HIGH_PRIORITY_FIRST -> {
                // Boost high/medium priority tasks even more
                if (task.priority == Priority.HIGH) score *= 1.2
                else if (task.priority == Priority.MEDIUM) score *= 1.1
            }

            PrioritizationStrategy.SHORT_TASKS_FIRST -> {
                // Give significant boost to shorter tasks
                score += 1000.0 / (taskDurationHours + 0.1).coerceAtLeast(0.1)
            }

            PrioritizationStrategy.EARLIER_DEADLINES_FIRST -> {
                // Similar to URGENT_FIRST, maybe a small extra boost for having *any* deadline.
                if (task.endDateConf != null) score *= 1.05
            }
        }


        // Start Date Proximity (Slight preference for tasks starting soon)
        task.startDateConf.dateTime?.let { start ->
            val hoursFromNow = Duration.between(LocalDateTime.now(), start).toHours()
            if (hoursFromNow > 0 && hoursFromNow < 48) {
                score += 200.0 / (hoursFromNow + 1).coerceAtLeast(1L) // Small boost for near-future tasks
            } else if (hoursFromNow <= 0) {
                score += 100.0 // Small boost if start time is now or past
            }
        }

        return maxOf(0.1, score) // Ensure a positive score
    }

    // Helper to format score for logging
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

} // End of GeneratePlanUseCase