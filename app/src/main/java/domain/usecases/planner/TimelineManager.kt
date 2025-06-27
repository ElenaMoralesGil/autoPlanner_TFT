package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.ConflictType
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DaySchedule
import com.elena.autoplanner.domain.models.Occupancy
import com.elena.autoplanner.domain.models.PlacementHeuristic
import com.elena.autoplanner.domain.models.PlacementResultInternal
import com.elena.autoplanner.domain.models.PlanningTask
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimeBlock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

class TimelineManager {
    private val timeline: MutableMap<LocalDate, DaySchedule> = mutableMapOf()

    fun initialize(
        start: LocalDate, end: LocalDate, workStart: LocalTime, workEnd: LocalTime,
        initialPeriodTasks: Map<LocalDate, Map<DayPeriod, List<PlanningTask>>>,
        dayOrganization: DayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
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
        timeline.values.forEach { daySchedule ->
            addRegularBreaks(daySchedule, dayOrganization)
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
        durationNeeded: Duration,
        minSearchTime: LocalDateTime,
        maxSearchTime: LocalDateTime,
        heuristic: PlacementHeuristic,
    ): TimeBlock? {
        if (durationNeeded <= Duration.ZERO) return null
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
                    Duration.between(potentialStart, maxPossibleEndInBlock)
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
            PlacementHeuristic.EARLIEST_FIT ->
                potentialSlots.minByOrNull { it.first.start }?.first

            PlacementHeuristic.BEST_FIT ->
                potentialSlots.minByOrNull { (originalBlock) ->
                    val blockStart = maxOf(originalBlock.start, minSearchTime)
                    val blockEnd = minOf(originalBlock.end, maxSearchTime)
                    val availableSpace = Duration.between(blockStart, blockEnd)
                    (availableSpace - durationNeeded).toNanos()
                }?.first ?: potentialSlots.minByOrNull { it.first.start }?.first
        }
    }

    fun placeTask(
        task: Task, startTime: LocalDateTime, endTime: LocalDateTime,
        occupancyType: Occupancy,
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

    fun addBufferOrBreak(taskEndTime: LocalDateTime, task: Task, strategy: DayOrganization) {
        val bufferDuration = when (strategy) {
            DayOrganization.SMART_BUFFERS, DayOrganization.BALANCED_SCHEDULE -> calculateAdaptiveBuffer(
                task
            )

            else -> Duration.ZERO
        }
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
        DayOrganization.SMART_BUFFERS, DayOrganization.BALANCED_SCHEDULE -> Duration.ofMinutes(10) // Temporal
        else -> Duration.ZERO
    }

    private fun calculateAdaptiveBuffer(task: Task): Duration = when {
        task.priority == Priority.HIGH -> Duration.ofMinutes(20)
        task.effectiveDurationMinutes >= 120 -> Duration.ofMinutes(15)
        task.effectiveDurationMinutes >= 60 -> Duration.ofMinutes(10)
        else -> Duration.ofMinutes(5)
    }

    private fun addRegularBreaks(daySchedule: DaySchedule, strategy: DayOrganization) {
        if (strategy != DayOrganization.BALANCED_SCHEDULE) return

        val workStart = daySchedule.workStartTime
        val workEnd = daySchedule.workEndTime
        val date = daySchedule.date

        var currentBreakTime = workStart.plusHours(2).plusMinutes(30)
        while (currentBreakTime.plusMinutes(15).isBefore(workEnd.minusHours(1))) {
            val breakStart = date.atTime(currentBreakTime)
            val breakEnd = breakStart.plusMinutes(15)

            val exactSlot = findSlot(
                Duration.ofMinutes(15),
                breakStart,
                breakEnd.plusNanos(1),
                PlacementHeuristic.EARLIEST_FIT
            )

            if (exactSlot?.start == breakStart && exactSlot.end == breakEnd) {
                val breakTask = Task.Builder().id(-2).name("Coffee Break").build()
                placeTask(breakTask, breakStart, breakEnd, Occupancy.BUFFER)
            }

            currentBreakTime = currentBreakTime.plusHours(3)
        }

        val lunchStart = findBestLunchTime(date, workStart, workEnd)
        if (lunchStart != null) {
            val lunchEnd = lunchStart.plusMinutes(60)
            val lunchTask = Task.Builder().id(-3).name("Lunch Break").build()
            placeTask(lunchTask, lunchStart, lunchEnd, Occupancy.BUFFER)
        }
    }

    private fun findBestLunchTime(
        date: LocalDate,
        workStart: LocalTime,
        workEnd: LocalTime,
    ): LocalDateTime? {
        val preferredStart = LocalTime.of(12, 30)
        return if (preferredStart.isAfter(workStart) && preferredStart.plusHours(1)
                .isBefore(workEnd)
        ) {
            date.atTime(preferredStart)
        } else {
            findSlot(
                Duration.ofMinutes(60),
                date.atTime(maxOf(workStart, LocalTime.of(12, 0))),
                date.atTime(minOf(workEnd, LocalTime.of(14, 0))),
                PlacementHeuristic.EARLIEST_FIT
            )?.start
        }
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