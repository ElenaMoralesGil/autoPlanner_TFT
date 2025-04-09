package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import java.time.LocalDate
import java.time.LocalTime

data class PlannerState(

    val currentStep: PlannerStep = PlannerStep.TIME_INPUT,
    val workStartTime: LocalTime = LocalTime.of(8, 0),
    val workEndTime: LocalTime = LocalTime.of(20, 0),
    val scheduleScope: ScheduleScope? = null,
    val selectedPriority: PrioritizationStrategy? = null,
    val selectedDayOrganization: DayOrganization? = null,
    val showSubtasksWithDuration: Boolean? = null,
    val selectedOverdueHandling: OverdueTaskHandling? = null,
    val numOverdueTasks: Int = 0,

    val generatedPlan: Map<LocalDate, List<ScheduledTaskItem>> = emptyMap(),
    val expiredTasksToResolve: List<Task> = emptyList(),
    val conflictsToResolve: List<ConflictItem> = emptyList(),
    val taskResolutions: Map<Int, ResolutionOption> = emptyMap(),
    val conflictResolutions: Map<Int, ResolutionOption> = emptyMap(),

    val isLoading: Boolean = false,
    val error: String? = null,
    val planSuccessfullyAdded: Boolean = false,
) {
    val canMoveToStep2: Boolean
        get() = scheduleScope != null && workStartTime < workEndTime

    val canMoveToStep3: Boolean
        get() = selectedPriority != null && selectedDayOrganization != null

    val canGeneratePlan: Boolean
        get() = showSubtasksWithDuration != null &&
                (numOverdueTasks == 0 || selectedOverdueHandling != null)

    val requiresResolution: Boolean // Logic remains the same
        get() = expiredTasksToResolve.any { taskResolutions[it.id] == null } ||
                conflictsToResolve.any { conflictResolutions[it.hashCode()] == null }
}