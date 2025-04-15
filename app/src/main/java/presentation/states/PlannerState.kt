// =============== Content from: src\main\java\presentation\states\PlannerState.kt ===============

package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.InfoItem
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PlacementHeuristic
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
    val selectedPriority: PrioritizationStrategy? = PrioritizationStrategy.URGENT_FIRST,
    val selectedDayOrganization: DayOrganization? = DayOrganization.MAXIMIZE_PRODUCTIVITY,
    val allowSplitting: Boolean? = true,
    val selectedOverdueHandling: OverdueTaskHandling? = null,
    val selectedPlacementHeuristic: PlacementHeuristic = PlacementHeuristic.EARLIEST_FIT,

    // Loaded Data
    val numOverdueTasks: Int = 0,

    // Plan Review State
    val generatedPlan: Map<LocalDate, List<ScheduledTaskItem>> = emptyMap(),
    val expiredTasksToResolve: List<Task> = emptyList(),
    val conflictsToResolve: List<ConflictItem> = emptyList(),
    val infoMessages: List<InfoItem> = emptyList(),
    val postponedTasks: List<Task> = emptyList(), // <-- ADD THIS LINE
    val taskResolutions: Map<Int, ResolutionOption> = emptyMap(),
    val conflictResolutions: Map<Int, ResolutionOption> = emptyMap(),
    val tasksFlaggedForManualEdit: Set<Int> = emptySet(),

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,
    val planSuccessfullyAdded: Boolean = false,
) {
    val canMoveToStep2: Boolean
        get() = scheduleScope != null // Simplified: Allow moving even if times are same initially

    val canMoveToStep3: Boolean
        get() = selectedPriority != null && selectedDayOrganization != null

    val canGeneratePlan: Boolean
        get() = allowSplitting != null &&
                (numOverdueTasks == 0 || selectedOverdueHandling != null) &&
                scheduleScope != null && // Ensure scope is selected
                selectedPriority != null && // Ensure priority is selected
                selectedDayOrganization != null // Ensure organization is selected

    val requiresResolution: Boolean
        get() {
            val needsExpiredResolution =
                expiredTasksToResolve.any { taskResolutions[it.id] == null }
            val needsConflictResolution =
                conflictsToResolve.any { conflictResolutions[it.hashCode()] == null }
            return needsExpiredResolution || needsConflictResolution
        }
}