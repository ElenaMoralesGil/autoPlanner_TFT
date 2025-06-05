package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope

fun PrioritizationStrategy.toDisplayString(): String = when (this) {
    PrioritizationStrategy.BY_URGENCY -> "By Urgency (tasks with closer deadline and priority)"
    PrioritizationStrategy.BY_IMPORTANCE -> "By Importance (priority level)"
    PrioritizationStrategy.BY_DURATION -> "By Duration (shortest tasks first)"
}

fun DayOrganization.toDisplayString(): String = when (this) {
    DayOrganization.MAXIMIZE_PRODUCTIVITY -> "No gaps between tasks (tight schedule)"
    DayOrganization.FOCUS_URGENT_BUFFER -> "Extra time between tasks (add buffers)"
    DayOrganization.LOOSE_SCHEDULE_BREAKS -> "Breaks every few hours (add breaks)"
}

fun OverdueTaskHandling.toDisplayString(): String = when (this) {
    OverdueTaskHandling.ADD_TODAY_FREE_TIME -> "Schedule automatically"
    OverdueTaskHandling.MANAGE_WHEN_FREE -> "Schedule manually later"
    OverdueTaskHandling.POSTPONE_TO_TOMORROW -> "Postpone to tomorrow"
}

fun ScheduleScope.toDisplayString(): String =
    this.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

fun ResolutionOption.toDisplayString(): String = when (this) {
    ResolutionOption.MOVE_TO_NEAREST_FREE -> "Move Auto"
    ResolutionOption.MOVE_TO_TOMORROW -> "Move Tomorrow"
    ResolutionOption.MANUALLY_SCHEDULE -> "Schedule Manually"
    ResolutionOption.LEAVE_IT_LIKE_THAT -> "Leave As Is"
    ResolutionOption.RESOLVED -> "Resolved"
}