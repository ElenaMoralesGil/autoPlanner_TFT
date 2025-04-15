package com.elena.autoplanner.domain.models

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ScheduleScope { TODAY, TOMORROW, THIS_WEEK }
enum class PrioritizationStrategy { URGENT_FIRST, HIGH_PRIORITY_FIRST, SHORT_TASKS_FIRST, EARLIER_DEADLINES_FIRST }
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
    FIXED_VS_FIXED,         // Two non-movable tasks clash
    CANNOT_FIT_PERIOD,      // Task doesn't fit its required Morning/Evening/Night slot
    NO_SLOT_ON_DATE,        // Task must be on a specific date, but no time works
    NO_SLOT_IN_SCOPE,       // Task is flexible but no time works anywhere in the schedule range
    OUTSIDE_WORK_HOURS,     // Informational: A fixed task spans outside work hours (not a blocking conflict)
    PLACEMENT_ERROR,        // Internal error during placement attempt
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
    // Override equals/hashCode based on task IDs and approximate time for better deduplication
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
        // Don't include conflictType in hashcode for deduplication purposes if reason is same
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

// --- Data Classes (Keep near relevant components or in a separate models file) ---

// Result from Categorizer
data class CategorizationResult(
    val fixedOccurrences: List<Pair<PlanningTask, LocalDateTime>>, // Task + specific time
    val periodTasksPending: Map<LocalDate, Map<DayPeriod, List<PlanningTask>>>, // Tasks needing placement within a period on a date
    val dateFlexPending: Map<LocalDate, List<PlanningTask>>, // Tasks needing placement on a specific date, flexible time
    val deadlineFlexibleTasks: List<PlanningTask>, // Tasks with only a deadline constraint
    val fullyFlexibleTasks: List<PlanningTask>,     // Tasks with no time/date constraints (within scope)
)

// Internal result used by TimelineManager.placeTask
sealed class PlacementResultInternal {
    data class Success(val placedBlock: TimeBlock) : PlacementResultInternal()
    data class Conflict(
        val reason: String,
        val conflictingTaskId: Int?,
        val conflictTime: LocalDateTime,
        val type: ConflictType = ConflictType.PLACEMENT_ERROR,
    ) : PlacementResultInternal() // Added type

    data class Failure(val reason: String) : PlacementResultInternal()
}

// Public result used by TaskPlacer.findAndPlaceFlexibleTask and returned by UseCase potentially
sealed class PlacementResult {
    data class Success(val blocks: List<TimeBlock>) :
        PlacementResult() // Can be multiple blocks if split

    data class Failure(val reason: String, val type: ConflictType) :
        PlacementResult() // Type indicates general reason

    data class Conflict(
        val reason: String,
        val conflictingTaskId: Int?,
        val conflictTime: LocalDateTime,
        val type: ConflictType,
    ) : PlacementResult()
}

// Wrapper for Task during planning
data class PlanningTask(
    val task: Task,
    val flags: PlanningFlags = PlanningFlags(),
) {
    val id: Int get() = task.id
}

// Flags associated with a task during planning
data class PlanningFlags(
    var isHardConflict: Boolean = false,       // e.g., Fixed vs Fixed, Recurrence error
    var isOverdue: Boolean = false,            // Marked by OverdueHandler
    var constraintDate: LocalDate? = null,    // For Overdue ADD_TODAY strategy
    var isPostponed: Boolean = false,          // Handled by OverdueHandler
    var needsManualResolution: Boolean = false,// Handled by OverdueHandler
    var failedPeriod: Boolean = false,          // Flagged if strict period placement failed
)