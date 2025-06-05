package com.elena.autoplanner.domain.models

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ScheduleScope { TODAY, TOMORROW, THIS_WEEK }
enum class PrioritizationStrategy {
    BY_URGENCY,
    BY_IMPORTANCE,
    BY_DURATION
}
enum class DayOrganization { MAXIMIZE_PRODUCTIVITY, FOCUS_URGENT_BUFFER, LOOSE_SCHEDULE_BREAKS }
enum class OverdueTaskHandling { ADD_TODAY_FREE_TIME, MANAGE_WHEN_FREE, POSTPONE_TO_TOMORROW }
enum class ResolutionOption { MOVE_TO_NEAREST_FREE, MOVE_TO_TOMORROW, MANUALLY_SCHEDULE, LEAVE_IT_LIKE_THAT, RESOLVED }

data class ScheduledTaskItem(
    val task: Task,
    val scheduledStartTime: LocalTime,
    val scheduledEndTime: LocalTime,
    val date: LocalDate,
)

data class DaySchedule(
    val date: LocalDate,
    val workStartTime: LocalTime,
    val workEndTime: LocalTime,
    val blocks: MutableList<TimeBlock> = mutableListOf(),
    val pendingPeriodTasks: MutableMap<DayPeriod, MutableList<Task>> = mutableMapOf(),
)

enum class ConflictType {
    FIXED_VS_FIXED,
    CANNOT_FIT_PERIOD,
    NO_SLOT_ON_DATE,
    NO_SLOT_IN_SCOPE,
    OUTSIDE_WORK_HOURS,
    PLACEMENT_ERROR,        
    UNKNOWN,
    ZERO_DURATION,
    OUTSIDE_SCOPE,
    RECURRENCE_ERROR
}

enum class Occupancy { FREE, FIXED_TASK, PERIOD_TASK, FLEXIBLE_TASK, BUFFER, OUT_OF_HOURS }

data class ConflictItem(
    val conflictingTasks: List<Task>,
    val reason: String = "Overlap or resource contention",
    val conflictTime: LocalDateTime? = null,
    val conflictType: ConflictType = ConflictType.UNKNOWN,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConflictItem

        val thisTaskIds = conflictingTasks.map { it.id }.sorted()
        val otherTaskIds = other.conflictingTasks.map { it.id }.sorted()
        val thisTime = conflictTime?.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
        val otherTime = other.conflictTime?.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)

        return thisTaskIds == otherTaskIds && reason == other.reason && thisTime == otherTime
    }

    override fun hashCode(): Int {
        var result = conflictingTasks.map { it.id }.sorted().hashCode()
        result = 31 * result + reason.hashCode()
        result = 31 * result + (conflictTime?.truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
            ?.hashCode() ?: 0)

        return result
    }
}

enum class PlannerStep {
    TIME_INPUT,
    PRIORITY_INPUT,
    ADDITIONAL_OPTIONS,
    REVIEW_PLAN
}


data class TimeBlock(
    val start: LocalDateTime,
    var end: LocalDateTime,
    var occupancy: Occupancy = Occupancy.FREE,
    var taskPriority: Priority = Priority.NONE,
    var taskId: Int? = null,
) : Comparable<TimeBlock> {
    val duration: Duration get() = Duration.between(start, end)
    override fun compareTo(other: TimeBlock): Int = this.start.compareTo(other.start)
    fun isFree(): Boolean = occupancy == Occupancy.FREE
    fun isOccupiedByTask(): Boolean =
        occupancy == Occupancy.FIXED_TASK || occupancy == Occupancy.PERIOD_TASK || occupancy == Occupancy.FLEXIBLE_TASK

    fun allowsOverlap(): Boolean = false
}


data class PlannerInput(
    val tasks: List<Task>,
    val workStartTime: LocalTime,
    val workEndTime: LocalTime,
    val scheduleScope: ScheduleScope,
    val prioritizationStrategy: PrioritizationStrategy,
    val dayOrganization: DayOrganization,
    val flexiblePlacementHeuristic: PlacementHeuristic = PlacementHeuristic.EARLIEST_FIT,
    val allowSplitting: Boolean,
    val overdueTaskHandling: OverdueTaskHandling,
)

enum class PlacementHeuristic {
    EARLIEST_FIT,
    BEST_FIT
}


data class PlannerOutput(
    val scheduledTasks: Map<LocalDate, List<ScheduledTaskItem>>,
    val unresolvedExpired: List<Task>,
    val unresolvedConflicts: List<ConflictItem>,
    val postponedTasks: List<Task> = emptyList(),
    val infoItems: List<InfoItem> = emptyList(),
)

data class InfoItem(
    val task: Task,
    val message: String,
    val relevantDate: LocalDate? = null,
)




data class CategorizationResult(
    val fixedOccurrences: List<Pair<PlanningTask, LocalDateTime>>,
    val periodTasksPending: Map<LocalDate, Map<DayPeriod, List<PlanningTask>>>,
    val dateFlexPending: Map<LocalDate, List<PlanningTask>>,
    val deadlineFlexibleTasks: List<PlanningTask>,
    val fullyFlexibleTasks: List<PlanningTask>,     
)


sealed class PlacementResultInternal {
    data class Success(val placedBlock: TimeBlock) : PlacementResultInternal()
    data class Conflict(
        val reason: String,
        val conflictingTaskId: Int?,
        val conflictTime: LocalDateTime,
        val type: ConflictType = ConflictType.PLACEMENT_ERROR,
    ) : PlacementResultInternal() 

    data class Failure(val reason: String) : PlacementResultInternal()
}


sealed class PlacementResult {
    data class Success(val blocks: List<TimeBlock>) :
        PlacementResult() 

    data class Failure(val reason: String, val type: ConflictType) :
        PlacementResult() 

    data class Conflict(
        val reason: String,
        val conflictingTaskId: Int?,
        val conflictTime: LocalDateTime,
        val type: ConflictType,
    ) : PlacementResult()
}


data class PlanningTask(
    val task: Task,
    val flags: PlanningFlags = PlanningFlags(),
) {
    val id: Int get() = task.id
}


data class PlanningFlags(
    var isHardConflict: Boolean = false,
    var isOverdue: Boolean = false,
    var constraintDate: LocalDate? = null,
    var isPostponed: Boolean = false,
    var needsManualResolution: Boolean = false,
    var failedPeriod: Boolean = false,          
)