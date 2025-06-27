package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PlacementHeuristic
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.viewmodel.Intent
import java.time.LocalTime

sealed class PlannerIntent : Intent {

    data object GoToNextStep : PlannerIntent()
    data object GoToPreviousStep : PlannerIntent()
    data object CancelPlanner : PlannerIntent()

    data class UpdateWorkStartTime(val time: LocalTime) : PlannerIntent()
    data class UpdateWorkEndTime(val time: LocalTime) : PlannerIntent()
    data class SelectScheduleScope(val scope: ScheduleScope) : PlannerIntent()


    data class SelectPriority(val priority: PrioritizationStrategy) : PlannerIntent()
    data class SelectDayOrganization(val organization: DayOrganization) : PlannerIntent()
    data class SelectPlacementHeuristic(val heuristic: PlacementHeuristic) : PlannerIntent()
    data class SelectOverdueHandling(val handling: OverdueTaskHandling) : PlannerIntent()


    data object GeneratePlan : PlannerIntent()
    data class ResolveExpiredTask(val task: Task, val resolution: ResolutionOption) :
        PlannerIntent()

    data class ResolveConflict(val conflict: ConflictItem, val resolution: ResolutionOption) :
        PlannerIntent()

    object AcknowledgeManualEdits : PlannerIntent()
    data class FlagTaskForManualEdit(val taskId: Int) : PlannerIntent()
    data class UnflagTaskForManualEdit(val taskId: Int) : PlannerIntent()
    data object AddPlanToCalendar : PlannerIntent()

}