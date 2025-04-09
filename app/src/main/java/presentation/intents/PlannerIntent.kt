package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.viewmodel.Intent
import java.time.LocalTime

sealed class PlannerIntent : Intent {

    object GoToNextStep : PlannerIntent()
    object GoToPreviousStep : PlannerIntent()
    object CancelPlanner : PlannerIntent()

    data class UpdateWorkStartTime(val time: LocalTime) : PlannerIntent()
    data class UpdateWorkEndTime(val time: LocalTime) : PlannerIntent()
    data class SelectScheduleScope(val scope: ScheduleScope) : PlannerIntent()

    // Step 2 Inputs
    data class SelectPriority(val priority: PrioritizationStrategy) : PlannerIntent()
    data class SelectDayOrganization(val organization: DayOrganization) : PlannerIntent()

    // Step 3 Inputs
    data class SelectShowSubtasks(val show: Boolean) : PlannerIntent()
    data class SelectOverdueHandling(val handling: OverdueTaskHandling) : PlannerIntent()

    // Plan Generation & Review
    object GeneratePlan : PlannerIntent()
    data class ResolveExpiredTask(val task: Task, val resolution: ResolutionOption) :
        PlannerIntent()

    data class ResolveConflict(val conflict: ConflictItem, val resolution: ResolutionOption) :
        PlannerIntent() // Use conflict object or unique ID

    object AddPlanToCalendar : PlannerIntent()

}