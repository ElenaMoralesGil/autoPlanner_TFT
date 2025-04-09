package com.elena.autoplanner.domain.models

import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleScope { TODAY, TOMORROW, THIS_WEEK }
enum class PrioritizationStrategy { URGENT_FIRST, HIGH_PRIORITY_FIRST, SHORT_TASKS_FIRST, EARLIER_DEADLINES_FIRST }
enum class DayOrganization { MAXIMIZE_PRODUCTIVITY, FOCUS_URGENT_BUFFER, LOOSE_SCHEDULE_BREAKS }
enum class OverdueTaskHandling { ADD_TODAY_FREE_TIME, MANAGE_WHEN_FREE, POSTPONE_TO_TOMORROW }
enum class ResolutionOption { MOVE_TO_NEAREST_FREE, MOVE_TO_TOMORROW, MANUALLY_SCHEDULE, LEAVE_IT_LIKE_THAT /* For Conflicts */, RESOLVED /* Internal state */ }

data class ScheduledTaskItem(
    val task: Task,
    val scheduledStartTime: LocalTime,
    val scheduledEndTime: LocalTime,
    val date: LocalDate,
)

data class ConflictItem(
    val conflictingTasks: List<Task>,
    val reason: String = "Overlap or resource contention",
)

enum class PlannerStep {
    TIME_INPUT,
    PRIORITY_INPUT,
    ADDITIONAL_OPTIONS,
    REVIEW_PLAN
}

data class PlannerInput(
    val tasks: List<Task>,
    val workStartTime: LocalTime,
    val workEndTime: LocalTime,
    val scheduleScope: ScheduleScope,
    val prioritizationStrategy: PrioritizationStrategy,
    val dayOrganization: DayOrganization,
    val showSubtasksWithDuration: Boolean,
    val overdueTaskHandling: OverdueTaskHandling,
)

data class PlannerOutput(
    val scheduledTasks: Map<LocalDate, List<ScheduledTaskItem>>,
    val unresolvedExpired: List<Task>,
    val unresolvedConflicts: List<ConflictItem>,
)
