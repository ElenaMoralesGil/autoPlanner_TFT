package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ConflictType
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DaySchedule
import com.elena.autoplanner.domain.models.InfoItem
import com.elena.autoplanner.domain.models.Occupancy
import com.elena.autoplanner.domain.models.PlacementHeuristic
import com.elena.autoplanner.domain.models.PlacementResult
import com.elena.autoplanner.domain.models.PlacementResultInternal
import com.elena.autoplanner.domain.models.PlannerInput
import com.elena.autoplanner.domain.models.PlanningTask
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimeBlock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskPlacer(
) {

    private val minMinutesToSplitTask = 30L
    private val maxAttemptsAtPlacing = 200

    fun placeFixedTasks(
        timelineManager: TimelineManager,
        fixedOccurrences: List<Pair<PlanningTask, LocalDateTime>>,
        context: PlanningContext,
    ) {

        fixedOccurrences
            .filterNot { context.placedTaskIds.contains(it.first.id) }
            .sortedBy { it.second }
            .forEach fixedLoop@{ (planningTask, occurrenceTime) ->
                val task = planningTask.task
                if (context.placedTaskIds.contains(task.id)) return@fixedLoop

                Log.d("DEBUG_PLACEMENT", "")
                Log.d("DEBUG_PLACEMENT", "=== COLOCANDO TAREA FIJA: '${task.name}' ===")
                Log.d("DEBUG_PLACEMENT", "occurrenceTime: $occurrenceTime")
                Log.d("DEBUG_PLACEMENT", "duration: ${task.effectiveDurationMinutes} minutos")

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
                val endDate = endTime.toLocalDate()

                Log.d("DEBUG_PLACEMENT", "Calculado endTime: $endTime")
                Log.d("DEBUG_PLACEMENT", "startDate: $startDate, endDate: $endDate")

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
                    )
                    if (result2 !is PlacementResultInternal.Success) {
                        reportPlacementResult(result2, task, startOfNextDay, context)
                        context.placedTaskIds.add(task.id)
                        return@fixedLoop
                    }

                    Log.d(
                        "DEBUG_PLACEMENT",
                        "✅ Colocada tarea multi-día: ${task.name} de $occurrenceTime a $endTime"
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
                    )
                    reportPlacementResult(result, task, occurrenceTime, context)
                    if (result is PlacementResultInternal.Success) {
                        Log.d(
                            "DEBUG_PLACEMENT",
                            "✅ Colocada tarea fija: ${task.name} de ${occurrenceTime.toLocalTime()} a ${endTime.toLocalTime()} el $startDate"
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
        placementTime: LocalDateTime,
        context: PlanningContext,
    ) {
        when (result) {
            is PlacementResultInternal.Success -> {
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
                    task.id
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

    fun placePrioritizedTask(
        planningTask: PlanningTask,
        timelineManager: TimelineManager,
        context: PlanningContext,
        input: PlannerInput,
        today: LocalDate,
        scopeStartDate: LocalDate,
        scopeEndDate: LocalDate,
    ) {
        val task = planningTask.task

        Log.d("DEBUG_PLACEMENT", "")
        Log.d("DEBUG_PLACEMENT", "=== COLOCANDO TAREA PRIORIZADA: '${task.name}' ===")
        Log.d("DEBUG_PLACEMENT", "taskStartPeriod: ${task.startDateConf?.dayPeriod}")
        Log.d("DEBUG_PLACEMENT", "isOverdue: ${planningTask.flags.isOverdue}")
        Log.d("DEBUG_PLACEMENT", "constraintDate: ${planningTask.flags.constraintDate}")

        val totalDuration =
            Duration.ofMinutes(task.effectiveDurationMinutes.toLong().coerceAtLeast(1))
        if (totalDuration <= Duration.ZERO) {
            context.addConflict(
                ConflictItem(
                    listOf(task),
                    "Zero duration",
                    task.startDateConf?.dateTime,
                    ConflictType.ZERO_DURATION
                ), task.id
            )
            return
        }

        var minSearchTime: LocalDateTime
        var maxSearchTime: LocalDateTime
        var placementOccupancy = Occupancy.FLEXIBLE_TASK
        var isFlexible = true
        var primaryFailed = false

        val taskStartDateTime = task.startDateConf?.dateTime
        val taskStartDate = taskStartDateTime?.toLocalDate()
        val taskStartPeriod = task.startDateConf?.dayPeriod ?: DayPeriod.NONE
        val taskEndDate = task.endDateConf?.dateTime?.toLocalDate()
            ?: scopeEndDate

        when {
            // ✅ CORRECCIÓN CRÍTICA: Manejo mejorado de tareas vencidas con constraint date
            planningTask.flags.isOverdue && planningTask.flags.constraintDate != null -> {
                val constraintDate = planningTask.flags.constraintDate!!
                Log.v(
                    "TaskPlacer",
                    "Task ${task.id} - Type: Overdue with constraint date ($constraintDate)"
                )

                val daySchedule = timelineManager.getDaySchedule(constraintDate)

                // ✅ CORRECCIÓN: Para tareas vencidas, usar la constraint date como referencia temporal
                minSearchTime = when {
                    constraintDate == today -> maxOf(
                        LocalDateTime.now(),
                        constraintDate.atTime(daySchedule?.workStartTime ?: input.workStartTime)
                    )

                    else -> constraintDate.atTime(daySchedule?.workStartTime ?: input.workStartTime)
                }

                // ✅ CORRECCIÓN CRÍTICA: Para tareas vencidas, maxSearchTime debe ser DESPUÉS de minSearchTime
                maxSearchTime = when {
                    // Si la tarea tiene período específico, respetar la ventana del período
                    taskStartPeriod != DayPeriod.NONE && taskStartPeriod != DayPeriod.ALLDAY -> {
                        val (periodStart, periodEnd) = calculateEffectivePeriodWindow(
                            daySchedule ?: DaySchedule(
                                constraintDate,
                                input.workStartTime,
                                input.workEndTime
                            ),
                            taskStartPeriod,
                            timelineManager
                        )
                        constraintDate.atTime(periodEnd)
                    }
                    // Para tareas vencidas sin período, usar todo el día de trabajo
                    else -> {
                        val workEndTime = daySchedule?.workEndTime ?: input.workEndTime
                        if (workEndTime <= (daySchedule?.workStartTime ?: input.workStartTime)) {
                            constraintDate.plusDays(1).atTime(workEndTime)
                        } else {
                            constraintDate.atTime(workEndTime)
                        }
                    }
                }

                // Si la tarea tiene período específico, ajustar la occupancy
                if (taskStartPeriod != DayPeriod.NONE && taskStartPeriod != DayPeriod.ALLDAY) {
                    placementOccupancy = Occupancy.PERIOD_TASK
                }

                isFlexible = true

                Log.d(
                    "DEBUG_PLACEMENT",
                    "Constraint-based search window: $minSearchTime -> $maxSearchTime"
                )
            }

            planningTask.flags.isOverdue && planningTask.flags.constraintDate == today -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Overdue Today")
                val daySchedule = timelineManager.getDaySchedule(today)

                minSearchTime = LocalDateTime.now()
                maxSearchTime = today.plusDays(1).atStartOfDay()

                val workStartTime = daySchedule?.workStartTime ?: input.workStartTime
                val todayWorkStart = today.atTime(workStartTime)
                if (minSearchTime.isBefore(todayWorkStart)) {
                    minSearchTime = todayWorkStart
                }

                isFlexible = true
            }

            planningTask.flags.isOverdue && planningTask.flags.constraintDate == null -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Overdue Flexible (scope-wide)")

                minSearchTime = maxOf(
                    LocalDateTime.now(),
                    scopeStartDate.atTime(input.workStartTime)
                )
                maxSearchTime = scopeEndDate.plusDays(1).atStartOfDay()

                isFlexible = true
            }

            taskStartPeriod != DayPeriod.NONE && taskStartPeriod != DayPeriod.ALLDAY -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Period (${taskStartPeriod})")

                // ✅ CORRECCIÓN CRÍTICA: Para tareas con período, usar la fecha objetivo del scope
                val targetDate = when {
                    // Si la tarea está marcada como vencida con constraint date, usar esa fecha
                    planningTask.flags.isOverdue && planningTask.flags.constraintDate != null ->
                        planningTask.flags.constraintDate!!
                    // ✅ NUEVO: Si solo tiene constraint date (sin ser overdue), usar esa fecha
                    planningTask.flags.constraintDate != null ->
                        planningTask.flags.constraintDate!!
                    // Si tiene fecha específica y está dentro del scope, usarla
                    taskStartDate != null && taskStartDate in scopeStartDate..scopeEndDate ->
                        taskStartDate
                    // ✅ NUEVO: Si es para scope mañana, usar mañana independientemente de la fecha original
                    else -> scopeStartDate
                }

                Log.d("DEBUG_PLACEMENT", "targetDate calculado: $targetDate")

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
                    isFlexible = taskEndDate?.let { it > targetDate } ?: false

                    Log.d("DEBUG_PLACEMENT", "Period window: $minSearchTime to $maxSearchTime")
                } else {

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

            taskStartDate != null -> {
                Log.v("TaskPlacer", "Task ${task.id} - Type: Date Constrained (${taskStartDate})")
                val daySchedule = timelineManager.getDaySchedule(taskStartDate)
                if (daySchedule != null && taskStartDate >= scopeStartDate && taskStartDate <= scopeEndDate) {
                    minSearchTime = taskStartDate.atTime(daySchedule.workStartTime)

                    maxSearchTime =
                        if (daySchedule.workEndTime <= daySchedule.workStartTime && daySchedule.workStartTime != daySchedule.workEndTime) {
                            taskStartDate.plusDays(1).atTime(daySchedule.workEndTime)
                        } else {
                            taskStartDate.atTime(daySchedule.workEndTime)
                        }

                    task.endDateConf?.dateTime?.let { deadline ->
                        maxSearchTime = minOf(maxSearchTime, deadline)
                    }
                    isFlexible =
                        taskEndDate > taskStartDate
                } else {

                    Log.v(
                        "TaskPlacer",
                        "Task ${task.id} - Date constraint outside scope, treating as flexible."
                    )
                    minSearchTime = maxOf(
                        taskStartDateTime,
                        scopeStartDate.atStartOfDay()
                    )
                    maxSearchTime = minOf(
                        task.endDateConf?.dateTime ?: scopeEndDate.plusDays(1).atStartOfDay(),
                        scopeEndDate.plusDays(1).atStartOfDay()
                    )
                }
            }

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

        // ✅ VALIDACIÓN CRÍTICA: Asegurar que maxSearchTime > minSearchTime
        if (maxSearchTime <= minSearchTime) {
            Log.w(
                "TaskPlacer",
                "Task ${task.id}: Invalid search window - maxSearchTime ($maxSearchTime) <= minSearchTime ($minSearchTime). Adjusting..."
            )

            // Para tareas vencidas, extender el rango hasta el final del scope
            if (planningTask.flags.isOverdue) {
                maxSearchTime = scopeEndDate.plusDays(1).atStartOfDay()
                Log.d(
                    "TaskPlacer",
                    "Task ${task.id}: Extended overdue task search window to: $minSearchTime -> $maxSearchTime"
                )
            } else {
                // Para tareas no vencidas, mover minSearchTime al inicio del scope
                minSearchTime = scopeStartDate.atStartOfDay()
                Log.d(
                    "TaskPlacer",
                    "Task ${task.id}: Adjusted search window to: $minSearchTime -> $maxSearchTime"
                )
            }
        }

        // Asegurar que nunca se programe en el pasado
        val now = LocalDateTime.now()
        minSearchTime = maxOf(minSearchTime, now)

        Log.d("DEBUG_PLACEMENT", "Final search window: $minSearchTime -> $maxSearchTime")

        var placementResult = findAndPlaceFlexibleTask(
            timelineManager = timelineManager,
            task = task,
            totalDuration = totalDuration,
            minSearchTime = minSearchTime,
            maxSearchTime = maxSearchTime,
            dayOrganization = input.dayOrganization,
            heuristic = input.flexiblePlacementHeuristic,
            context = context,
            occupancyType = placementOccupancy
        )

        when (placementResult) {
            is PlacementResult.Success -> {
                Log.d("DEBUG_PLACEMENT", "✅ Successfully placed task ${task.id} in primary window.")
                context.placedTaskIds.add(task.id)

                if (planningTask.flags.failedPeriod) {
                    context.addInfoItem(
                        InfoItem(
                            task,
                            "Placed outside preferred period",
                            placementResult.blocks.firstOrNull()?.start?.toLocalDate()
                        )
                    )
                }
                return
            }

            is PlacementResult.Failure -> {
                Log.w(
                    "TaskPlacer",
                    "Task ${task.id}: Primary placement failed: ${placementResult.reason}"
                )
                primaryFailed = true

            }

            is PlacementResult.Conflict -> {
                Log.w(
                    "TaskPlacer",
                    "Task ${task.id}: Primary placement conflict: ${placementResult.reason}"
                )

                val conflictingTaskObj =
                    placementResult.conflictingTaskId?.let { cId -> context.planningTaskMap[cId]?.task }
                context.addConflict(
                    ConflictItem(
                        listOfNotNull(task, conflictingTaskObj).distinctBy { it.id },
                        placementResult.reason,
                        placementResult.conflictTime,
                        placementResult.type
                    ),
                    task.id
                )
                return
            }
        }

        if (primaryFailed && isFlexible) {
            Log.i(
                "TaskPlacer",
                "Task ${task.id}: Primary placement failed, attempting fallback placement."
            )

            val fallbackMinSearch = maxOf(
                task.startDateConf?.dateTime ?: scopeStartDate.atStartOfDay(),
                scopeStartDate.atStartOfDay()
            )
            val fallbackMaxSearch = minOf(
                task.endDateConf?.dateTime ?: scopeEndDate.plusDays(1).atStartOfDay(),
                scopeEndDate.plusDays(1).atStartOfDay()
            )

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
                    heuristic = input.flexiblePlacementHeuristic,
                    context = context,
                    occupancyType = Occupancy.FLEXIBLE_TASK
                )
            }
        }

        when (placementResult) {
            is PlacementResult.Success -> {

                Log.d("TaskPlacer", "Successfully placed task ${task.id} in fallback window.")
                context.placedTaskIds.add(task.id)

                context.addInfoItem(
                    InfoItem(
                        task,
                        "Placed outside preferred time/period",
                        placementResult.blocks.firstOrNull()?.start?.toLocalDate()
                    )
                )
            }

            is PlacementResult.Failure -> {

                Log.w(
                    "TaskPlacer",
                    "Task ${task.id}: All placement attempts failed: ${placementResult.reason}"
                )

                val finalConflictType = when {
                    planningTask.flags.isOverdue && planningTask.flags.constraintDate == today -> ConflictType.NO_SLOT_ON_DATE
                    taskStartPeriod != DayPeriod.NONE && taskStartPeriod != DayPeriod.ALLDAY -> ConflictType.CANNOT_FIT_PERIOD
                    taskStartDate != null -> ConflictType.NO_SLOT_ON_DATE
                    else -> ConflictType.NO_SLOT_IN_SCOPE
                }

                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        placementResult.reason,
                        minSearchTime,
                        finalConflictType
                    ),
                    task.id
                )
            }

            is PlacementResult.Conflict -> {

                Log.e(
                    "TaskPlacer",
                    "Task ${task.id}: Fallback placement conflict: ${placementResult.reason}"
                )
                val conflictingTaskObj =
                    placementResult.conflictingTaskId?.let { cId -> context.planningTaskMap[cId]?.task }
                context.addConflict(
                    ConflictItem(
                        listOfNotNull(task, conflictingTaskObj).distinctBy { it.id },
                        placementResult.reason,
                        placementResult.conflictTime,
                        placementResult.type
                    ),
                    task.id
                )
            }
        }
    }

    private fun findAndPlaceFlexibleTask(
        timelineManager: TimelineManager,
        task: Task,
        totalDuration: Duration,
        minSearchTime: LocalDateTime,
        maxSearchTime: LocalDateTime,
        dayOrganization: DayOrganization,
        heuristic: PlacementHeuristic,
        context: PlanningContext,
        occupancyType: Occupancy,
    ): PlacementResult {
        var remainingDuration = totalDuration
        val placedBlocksResult = mutableListOf<TimeBlock>()
        var lastBlockEndTime: LocalDateTime? = null
        val allowSplitting = task.effectiveAllowSplitting
        var attempts = 0
        val buffer = timelineManager.getBufferDuration(dayOrganization)

        while (remainingDuration > Duration.ZERO && attempts < maxAttemptsAtPlacing) {
            attempts++

            val durationNeeded = if (allowSplitting && remainingDuration < totalDuration) maxOf(
                remainingDuration,
                Duration.ofMinutes(minMinutesToSplitTask)
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
                        "Task ${task.id}: Found slot spans midnight ($placementStartTime -> $placementEndTime), but task splitting disabled. Trying next slot."
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
                )
                if (result1 !is PlacementResultInternal.Success) {
                    Log.e(
                        "TaskPlacerFlex",
                        "Task ${task.id}: Failed to place first part of multi-day chunk: $result1"
                    )
                    return handlePlacementFailure(result1, task)
                }

                val result2 = timelineManager.placeTask(
                    task,
                    startOfDay2,
                    placementEndTime,
                    occupancyType
                )
                if (result2 !is PlacementResultInternal.Success) {
                    Log.e(
                        "TaskPlacerFlex",
                        "Task ${task.id}: Failed to place second part of multi-day chunk: $result2"
                    )
                    return handlePlacementFailure(result2, task)
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
                timelineManager.addBufferOrBreak(placementEndTime, task, dayOrganization)
                Log.d(
                    "TaskPlacerFlex",
                    "Task ${task.id}: Placed multi-day flex chunk ${result1.placedBlock.start.toLocalTime()}-${result2.placedBlock.end.toLocalTime()}. Rem: ${remainingDuration.toMinutes()}m"
                )

            } else {

                val placementInternalResult = timelineManager.placeTask(
                    task,
                    placementStartTime,
                    placementEndTime,
                    occupancyType
                )
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
                        timelineManager.addBufferOrBreak(placedBlock.end, task, dayOrganization)
                        Log.d(
                            "TaskPlacerFlex",
                            "Task ${task.id}: Placed flex chunk ${placedBlock.start.toLocalTime()}-${placedBlock.end.toLocalTime()} on ${placedBlock.start.toLocalDate()}. Rem: ${remainingDuration.toMinutes()}m"
                        )
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
        }

        if (attempts >= maxAttemptsAtPlacing) {
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

    private fun handlePlacementFailure(
        result: PlacementResultInternal,
        task: Task,
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

    private fun minOf(a: LocalTime, b: LocalTime): LocalTime = if (a < b) a else b
    private fun maxOf(a: LocalTime, b: LocalTime): LocalTime = if (a > b) a else b
}