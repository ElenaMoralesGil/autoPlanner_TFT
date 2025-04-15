// =============== Content from: src\main\java\presentation\viewmodel\PlannerViewModel.kt ===============

package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repository.TaskResult
import com.elena.autoplanner.domain.usecases.planner.GeneratePlanUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.presentation.effects.PlannerEffect
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

class PlannerViewModel(
    private val generatePlanUseCase: GeneratePlanUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) : BaseViewModel<PlannerIntent, PlannerState, PlannerEffect>() {

    override fun createInitialState(): PlannerState = PlannerState(
        isLoading = true,
        selectedPriority = PrioritizationStrategy.URGENT_FIRST,
        selectedDayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
        allowSplitting = true,
        selectedPlacementHeuristic = PlacementHeuristic.EARLIEST_FIT
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            var finalOverdueCount = 0
            var errorMessage: String? = null
            try {
                val tasks: List<Task>? = getTasksUseCase().firstOrNull()
                if (tasks != null) {
                    finalOverdueCount = tasks.count { !it.isCompleted && it.isExpired() }
                    Log.d("PlannerVM", "Initial overdue count = $finalOverdueCount")
                } else {
                    Log.d("PlannerVM", "Initial task list was null or empty.")
                }
            } catch (e: Exception) {
                Log.e("PlannerVM", "Error loading initial tasks", e)
                errorMessage = "Error loading initial tasks: ${e.localizedMessage}"
            } finally {
                setState {
                    copy(
                        numOverdueTasks = finalOverdueCount,
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    override suspend fun handleIntent(intent: PlannerIntent) {
        when (intent) {
            is PlannerIntent.GoToNextStep -> handleNextStep()
            is PlannerIntent.GoToPreviousStep -> handlePreviousStep()
            is PlannerIntent.UpdateWorkStartTime -> setState { copy(workStartTime = intent.time) }
            is PlannerIntent.UpdateWorkEndTime -> setState { copy(workEndTime = intent.time) }
            is PlannerIntent.SelectScheduleScope -> setState { copy(scheduleScope = intent.scope) }
            is PlannerIntent.SelectPriority -> setState { copy(selectedPriority = intent.priority) }
            is PlannerIntent.SelectDayOrganization -> setState { copy(selectedDayOrganization = intent.organization) }
            is PlannerIntent.SelectAllowSplitting -> setState { copy(allowSplitting = intent.allow) }
            is PlannerIntent.SelectOverdueHandling -> setState { copy(selectedOverdueHandling = intent.handling) }
            is PlannerIntent.SelectPlacementHeuristic -> setState { copy(selectedPlacementHeuristic = intent.heuristic) }
            is PlannerIntent.GeneratePlan -> executePlanGeneration()
            is PlannerIntent.ResolveExpiredTask -> handleExpiredResolution(
                intent.task,
                intent.resolution
            )

            is PlannerIntent.ResolveConflict -> handleConflictResolution(
                intent.conflict,
                intent.resolution
            )

            is PlannerIntent.FlagTaskForManualEdit -> handleFlagForManualEdit(intent.taskId)
            is PlannerIntent.UnflagTaskForManualEdit -> handleUnflagForManualEdit(intent.taskId)
            is PlannerIntent.AcknowledgeManualEdits -> handleAcknowledgeManualEdits()
            is PlannerIntent.AddPlanToCalendar -> savePlan()
            is PlannerIntent.CancelPlanner -> setEffect(PlannerEffect.NavigateBack)
        }
    }

    private fun handleUnflagForManualEdit(taskId: Int) {
        Log.d("PlannerVM", "Unflagging task $taskId for manual edit UI")
        setState { copy(tasksFlaggedForManualEdit = tasksFlaggedForManualEdit - taskId) }
    }

    private fun handleNextStep() {
        val currentState = currentState
        when (currentState.currentStep) {
            PlannerStep.TIME_INPUT -> if (currentState.canMoveToStep2) {
                setState { copy(currentStep = PlannerStep.PRIORITY_INPUT) }
            } else {
                setEffect(PlannerEffect.ShowSnackbar("Please select availability hours and a schedule scope."))
            }
            PlannerStep.PRIORITY_INPUT -> if (currentState.canMoveToStep3) {
                setState { copy(currentStep = PlannerStep.ADDITIONAL_OPTIONS) }
            } else {
                setEffect(PlannerEffect.ShowSnackbar("Please select a prioritization strategy and day organization style."))
            }
            PlannerStep.ADDITIONAL_OPTIONS -> if (currentState.canGeneratePlan) {
                sendIntent(PlannerIntent.GeneratePlan)
            } else {
                setEffect(PlannerEffect.ShowSnackbar("Please select task splitting and overdue task options."))
            }
            PlannerStep.REVIEW_PLAN -> { /* Final step, no next */
            }
        }
    }

    private fun handlePreviousStep() {
        val currentState = currentState
        when (currentState.currentStep) {
            PlannerStep.PRIORITY_INPUT -> setState { copy(currentStep = PlannerStep.TIME_INPUT) }
            PlannerStep.ADDITIONAL_OPTIONS -> setState { copy(currentStep = PlannerStep.PRIORITY_INPUT) }
            PlannerStep.REVIEW_PLAN -> setState {
                copy(
                    currentStep = PlannerStep.ADDITIONAL_OPTIONS,
                    generatedPlan = emptyMap(),
                    expiredTasksToResolve = emptyList(),
                    conflictsToResolve = emptyList(),
                    taskResolutions = emptyMap(),
                    conflictResolutions = emptyMap(),
                    error = null,
                    planSuccessfullyAdded = false,
                    tasksFlaggedForManualEdit = emptySet() // Clear flags when going back
                )
            }
            PlannerStep.TIME_INPUT -> setEffect(PlannerEffect.NavigateBack)
        }
    }

    private fun executePlanGeneration() {
        val currentState = currentState
        if (!currentState.canGeneratePlan) {
            setEffect(PlannerEffect.ShowSnackbar("Please complete all options before generating."))
            return
        }

        viewModelScope.launch {
            setState {
                copy(
                    isLoading = true,
                    error = null,
                    generatedPlan = emptyMap(),
                    expiredTasksToResolve = emptyList(),
                    conflictsToResolve = emptyList(),
                    taskResolutions = emptyMap(),
                    conflictResolutions = emptyMap(),
                    planSuccessfullyAdded = false,
                    tasksFlaggedForManualEdit = emptySet()
                )
            }
            val allTasks: List<Task>? = try {
                getTasksUseCase().firstOrNull()
            } catch (e: Exception) {
                Log.e("PlannerVM", "Failed to load tasks for planning", e)
                setState {
                    copy(
                        isLoading = false,
                        error = "Failed to load tasks: ${e.localizedMessage}"
                    )
                }
                setEffect(PlannerEffect.ShowSnackbar("Error loading tasks for planning."))
                return@launch
            }

            if (allTasks == null || allTasks.isEmpty()) {
                Log.d("PlannerVM", "No tasks found for planning.")
                setState {
                    copy(
                        isLoading = false,
                        error = "No tasks available to generate a plan."
                    )
                }
                setEffect(PlannerEffect.ShowSnackbar("No tasks found to plan."))
                return@launch
            }
            Log.d("PlannerVM", "Tasks loaded for planning: ${allTasks.size}")

            try {
                val plannerInput = PlannerInput(
                    tasks = allTasks,
                    workStartTime = currentState.workStartTime,
                    workEndTime = currentState.workEndTime,
                    scheduleScope = currentState.scheduleScope!!,
                    prioritizationStrategy = currentState.selectedPriority!!,
                    dayOrganization = currentState.selectedDayOrganization!!,
                    flexiblePlacementHeuristic = currentState.selectedPlacementHeuristic,
                    allowSplitting = currentState.allowSplitting!!,
                    overdueTaskHandling = if (currentState.numOverdueTasks > 0) currentState.selectedOverdueHandling!! else OverdueTaskHandling.POSTPONE_TO_TOMORROW
                )
                Log.d("PlannerVM", "Calling GeneratePlanUseCase with input...")
                val plannerOutput = generatePlanUseCase(plannerInput)
                Log.d(
                    "PlannerVM",
                    "GeneratePlanUseCase output: Scheduled=${plannerOutput.scheduledTasks.values.sumOf { it.size }}, Expired=${plannerOutput.unresolvedExpired.size}, Conflicts=${plannerOutput.unresolvedConflicts.size}"
                )
                setState {
                    copy(
                        isLoading = false,
                        generatedPlan = plannerOutput.scheduledTasks,
                        expiredTasksToResolve = plannerOutput.unresolvedExpired,
                        conflictsToResolve = plannerOutput.unresolvedConflicts,
                        infoMessages = plannerOutput.infoItems,
                        postponedTasks = plannerOutput.postponedTasks,
                        currentStep = PlannerStep.REVIEW_PLAN,
                        taskResolutions = emptyMap(),
                        conflictResolutions = emptyMap(),
                        tasksFlaggedForManualEdit = emptySet()
                    )
                }
            } catch (e: Exception) {
                Log.e("PlannerVM", "Planning failed", e)
                setState {
                    copy(
                        isLoading = false,
                        error = "Planning failed: ${e.localizedMessage}"
                    )
                }
                setEffect(PlannerEffect.ShowSnackbar("Error generating plan: ${e.localizedMessage}"))
            }
        }
    }

    private fun handleExpiredResolution(task: Task, resolution: ResolutionOption) {
        Log.d("PlannerVM", "Handling Expired Resolution: Task ${task.id}, Option $resolution")
        setState { copy(taskResolutions = taskResolutions + (task.id to resolution)) }
        // Flag/unflag based on resolution
        if (resolution == ResolutionOption.MANUALLY_SCHEDULE) {
            handleFlagForManualEdit(task.id)
        } else {
            handleUnflagForManualEdit(task.id)
        }
    }

    private fun handleConflictResolution(conflict: ConflictItem, resolution: ResolutionOption) {
        val conflictId = conflict.hashCode()
        Log.d("PlannerVM", "Handling Conflict Resolution: Hash $conflictId, Option $resolution")
        setState { copy(conflictResolutions = conflictResolutions + (conflictId to resolution)) }
        // Flag/unflag based on resolution
        val taskToFlag = conflict.conflictingTasks.minByOrNull { it.priority.ordinal }
        taskToFlag?.let {
            if (resolution == ResolutionOption.MANUALLY_SCHEDULE || resolution == ResolutionOption.LEAVE_IT_LIKE_THAT) {
                handleFlagForManualEdit(it.id)
            } else {
                handleUnflagForManualEdit(it.id)
            }
        }
    }

    private fun handleFlagForManualEdit(taskId: Int) {
        Log.d("PlannerVM", "Flagging task $taskId for manual edit UI")
        setState { copy(tasksFlaggedForManualEdit = tasksFlaggedForManualEdit + taskId) }
    }

    private fun handleAcknowledgeManualEdits() {
        Log.d("PlannerVM", "User acknowledged manual edits (or lack thereof)")
        // This might trigger saving if all other resolutions are done
    }

    private fun savePlan() {
        viewModelScope.launch {
            val state = currentState
            if (state.requiresResolution) {
                Log.w("PlannerVM", "Attempted save while resolutions pending.")
                setEffect(PlannerEffect.ShowSnackbar("Please resolve all required items first."))
                return@launch
            }

            Log.d("PlannerVM", "Starting savePlan...")
            setState { copy(isLoading = true, error = null, planSuccessfullyAdded = false) }

            try {
                val originalTasks: Map<Int, Task> = try {
                    getTasksUseCase().firstOrNull()?.associateBy { it.id } ?: emptyMap()
                } catch (e: Exception) {
                    Log.e("PlannerVM", "Error fetching original tasks during savePlan", e)
                    setState {
                        copy(
                            isLoading = false,
                            error = "Failed to load tasks for saving: ${e.localizedMessage}"
                        )
                    }
                    setEffect(PlannerEffect.ShowSnackbar("Error preparing plan for saving."))
                    return@launch
                }

                val tasksToUpdate = mutableMapOf<Int, Task>()
                val today = LocalDate.now()

                // 1. Process Resolved Expired Tasks (Only 'Move Tomorrow')
                state.taskResolutions.forEach { (taskId, resolution) ->
                    if (state.tasksFlaggedForManualEdit.contains(taskId)) return@forEach // Skip flagged
                    if (resolution == ResolutionOption.MOVE_TO_TOMORROW) {
                        originalTasks[taskId]?.let { originalTask ->
                            val targetDate = today.plusDays(1)
                            val keepTime = originalTask.startDateConf.dateTime?.toLocalTime()
                                ?: state.workStartTime
                            val newDateTime = LocalDateTime.of(targetDate, keepTime)
                            val newEndDateConf = originalTask.endDateConf?.takeIf {
                                it.dateTime != null && it.dateTime.isAfter(newDateTime)
                            }
                            tasksToUpdate[taskId] = Task.from(originalTask)
                                .startDateConf(
                                    TimePlanning(
                                        dateTime = newDateTime,
                                        dayPeriod = DayPeriod.NONE
                                    )
                                )
                                .endDateConf(newEndDateConf)
                                .build()
                            Log.d(
                                "PlannerVM",
                                "Task $taskId marked for update (Expired Resolved: MoveTomorrow): New Start $newDateTime"
                            )
                        } ?: Log.w(
                            "PlannerVM",
                            "Original task $taskId not found for expired resolution."
                        )
                    }
                }

                // 2. Process Resolved Conflicts (Only 'Move Tomorrow')
                state.conflictResolutions.forEach { (conflictHash, resolution) ->
                    if (resolution == ResolutionOption.MOVE_TO_TOMORROW) {
                        val conflict =
                            state.conflictsToResolve.find { it.hashCode() == conflictHash }
                        val taskToModify =
                            conflict?.conflictingTasks?.minByOrNull { it.priority.ordinal }
                                ?: conflict?.conflictingTasks?.firstOrNull()

                        if (taskToModify != null) {
                            val taskId = taskToModify.id
                            if (state.tasksFlaggedForManualEdit.contains(taskId)) return@forEach // Skip flagged

                            originalTasks[taskId]?.let { originalTask ->
                                val targetDate = today.plusDays(1)
                                val keepTime = originalTask.startDateConf.dateTime?.toLocalTime()
                                    ?: state.workStartTime
                                val newDateTime = LocalDateTime.of(targetDate, keepTime)
                                val newEndDateConf = originalTask.endDateConf?.takeIf {
                                    it.dateTime != null && it.dateTime.isAfter(newDateTime)
                                }
                                tasksToUpdate[taskId] = Task.from(originalTask)
                                    .startDateConf(
                                        TimePlanning(
                                            dateTime = newDateTime,
                                            dayPeriod = DayPeriod.NONE
                                        )
                                    )
                                    .endDateConf(newEndDateConf)
                                    .build()
                                Log.d(
                                    "PlannerVM",
                                    "Task $taskId marked for update (Conflict Resolved: MoveTomorrow): New Start $newDateTime"
                                )
                            } ?: Log.w(
                                "PlannerVM",
                                "Original task $taskId not found for conflict resolution."
                            )
                        }
                    }
                }

                // 3. Process Postponed Tasks (from initial handling)
                state.postponedTasks.forEach { task ->
                    if (state.tasksFlaggedForManualEdit.contains(task.id)) return@forEach // Skip flagged
                    originalTasks[task.id]?.let { originalTask ->
                        val targetDate = calculatePostponeDate(
                            state.scheduleScope ?: ScheduleScope.THIS_WEEK,
                            today
                        )
                        val keepTime = originalTask.startDateConf.dateTime?.toLocalTime()
                            ?: state.workStartTime
                        val newDateTime = LocalDateTime.of(targetDate, keepTime)
                        val newEndDateConf = originalTask.endDateConf?.takeIf {
                            it.dateTime != null && it.dateTime.isAfter(newDateTime)
                        }
                        tasksToUpdate[task.id] = Task.from(originalTask)
                            .startDateConf(
                                TimePlanning(
                                    dateTime = newDateTime,
                                    dayPeriod = DayPeriod.NONE
                                )
                            )
                            .endDateConf(newEndDateConf)
                            .build()
                        Log.d(
                            "PlannerVM",
                            "Task ${task.id} marked for update (Postponed): New Start $newDateTime"
                        )
                    } ?: Log.w("PlannerVM", "Original task ${task.id} not found for postponement.")
                }

                // 4. Create Save Jobs ONLY for tasks that need updating
                Log.d("PlannerVM", "Creating ${tasksToUpdate.size} save jobs for updated tasks.")
                val updateJobs = tasksToUpdate.values.map { finalTaskToSave ->
                    Log.d("PlannerVM", "Async save job for Task ID: ${finalTaskToSave.id}")
                    async { saveTaskUseCase(finalTaskToSave) }
                }

                // 5. Wait and Process Results
                if (updateJobs.isNotEmpty()) {
                    Log.d("PlannerVM", "Waiting for ${updateJobs.size} save operations...")
                    val results: List<TaskResult<Int>> = updateJobs.awaitAll()
                    val errors = results.filterIsInstance<TaskResult.Error>()

                    if (errors.isNotEmpty()) {
                        val errorMessages = errors.joinToString("\n") { "- ${it.message}" }
                        Log.e("PlannerVM", "Save plan failed for some tasks:\n$errorMessages")
                        setState { copy(isLoading = false, error = "Some tasks failed to save.") }
                        setEffect(PlannerEffect.ShowSnackbar("Error saving parts of the plan."))
                    } else {
                        Log.d("PlannerVM", "Save plan successful for ${tasksToUpdate.size} tasks.")
                        setState { copy(isLoading = false, planSuccessfullyAdded = true) }
                        setEffect(PlannerEffect.ShowSnackbar("Plan updates applied successfully!"))
                        setEffect(PlannerEffect.NavigateBack)
                    }
                } else {
                    Log.d("PlannerVM", "No tasks required updates.")
                    setState {
                        copy(
                            isLoading = false,
                            planSuccessfullyAdded = true
                        )
                    } // Still successful if nothing needed saving
                    setEffect(PlannerEffect.ShowSnackbar("Plan reviewed. No updates needed."))
                    setEffect(PlannerEffect.NavigateBack)
                }

            } catch (e: Exception) {
                Log.e("PlannerVM", "Exception during savePlan execution", e)
                setState {
                    copy(
                        isLoading = false,
                        error = "Failed to save plan: ${e.localizedMessage}"
                    )
                }
                setEffect(PlannerEffect.ShowSnackbar("Error saving plan: ${e.localizedMessage}"))
            }
        }
    }

    private fun calculatePostponeDate(originalScope: ScheduleScope, today: LocalDate): LocalDate {
        return when (originalScope) {
            ScheduleScope.TODAY -> today.plusDays(1)
            ScheduleScope.TOMORROW -> today.plusDays(2)
            ScheduleScope.THIS_WEEK -> today.with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
        }
    }
}