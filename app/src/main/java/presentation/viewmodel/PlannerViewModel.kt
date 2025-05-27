

package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PlacementHeuristic
import com.elena.autoplanner.domain.models.PlannerInput
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.results.TaskResult
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

            PlannerStep.REVIEW_PLAN -> { 
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
                    tasksFlaggedForManualEdit = emptySet() 
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
                val originalTasks: Map<Int, Task> = fetchOriginalTasks() ?: return@launch
                val tasksToUpdate = mutableMapOf<Int, Task>()

                applyResolutions(state, originalTasks, tasksToUpdate)
                applyScheduledPlan(state, originalTasks, tasksToUpdate)
                clearUnscheduledOrManualTasks(state, originalTasks, tasksToUpdate)

                executeSaveOperations(tasksToUpdate)

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

    private suspend fun fetchOriginalTasks(): Map<Int, Task>? {
        return try {
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
            null
        }
    }

    private fun applyResolutions(
        state: PlannerState,
        originalTasks: Map<Int, Task>,
        tasksToUpdate: MutableMap<Int, Task>,
    ) {
        val today = LocalDate.now()

        state.taskResolutions.forEach { (taskId, resolution) ->
            if (state.tasksFlaggedForManualEdit.contains(taskId)) return@forEach
            if (resolution == ResolutionOption.MOVE_TO_TOMORROW) {
                val task = tasksToUpdate[taskId] ?: originalTasks[taskId]
                task?.let {
                    val targetDate = today.plusDays(1)
                    val keepTime = it.startDateConf.dateTime?.toLocalTime() ?: state.workStartTime
                    val newDateTime = LocalDateTime.of(targetDate, keepTime)
                    val newEndDateConf = it.endDateConf?.takeIf { conf ->
                        conf.dateTime != null && conf.dateTime.isAfter(newDateTime)
                    }
                    tasksToUpdate[taskId] = Task.from(it)
                        .startDateConf(
                            TimePlanning(
                                dateTime = newDateTime,
                                dayPeriod = DayPeriod.NONE
                            )
                        )
                        .endDateConf(newEndDateConf)
                        .scheduledStartDateTime(null) 
                        .scheduledEndDateTime(null)
                        .build()
                    Log.d(
                        "PlannerVM",
                        "Task $taskId updated (Expired Resolved: MoveTomorrow): New Start $newDateTime"
                    )
                }
            }
        }

        state.conflictResolutions.forEach { (conflictHash, resolution) ->
            if (resolution == ResolutionOption.MOVE_TO_TOMORROW) {
                val conflict = state.conflictsToResolve.find { it.hashCode() == conflictHash }
                val taskToModify = conflict?.conflictingTasks?.minByOrNull { it.priority.ordinal }
                    ?: conflict?.conflictingTasks?.firstOrNull()

                taskToModify?.let { task ->
                    val taskId = task.id
                    if (state.tasksFlaggedForManualEdit.contains(taskId)) return@let
                    val originalTask = tasksToUpdate[taskId] ?: originalTasks[taskId]
                    originalTask?.let {
                        val targetDate = today.plusDays(1)
                        val keepTime =
                            it.startDateConf.dateTime?.toLocalTime() ?: state.workStartTime
                        val newDateTime = LocalDateTime.of(targetDate, keepTime)
                        val newEndDateConf = it.endDateConf?.takeIf { conf ->
                            conf.dateTime != null && conf.dateTime.isAfter(newDateTime)
                        }
                        tasksToUpdate[taskId] = Task.from(it)
                            .startDateConf(
                                TimePlanning(
                                    dateTime = newDateTime,
                                    dayPeriod = DayPeriod.NONE
                                )
                            )
                            .endDateConf(newEndDateConf)
                            .scheduledStartDateTime(null) 
                            .scheduledEndDateTime(null)
                            .build()
                        Log.d(
                            "PlannerVM",
                            "Task $taskId updated (Conflict Resolved: MoveTomorrow): New Start $newDateTime"
                        )
                    }
                }
            }
        }

        state.postponedTasks.forEach { task ->
            if (state.tasksFlaggedForManualEdit.contains(task.id)) return@forEach
            val originalTask = tasksToUpdate[task.id] ?: originalTasks[task.id]
            originalTask?.let {
                val targetDate =
                    calculatePostponeDate(state.scheduleScope ?: ScheduleScope.THIS_WEEK, today)
                val keepTime = it.startDateConf.dateTime?.toLocalTime() ?: state.workStartTime
                val newDateTime = LocalDateTime.of(targetDate, keepTime)
                val newEndDateConf = it.endDateConf?.takeIf { conf ->
                    conf.dateTime != null && conf.dateTime.isAfter(newDateTime)
                }
                tasksToUpdate[task.id] = Task.from(it)
                    .startDateConf(TimePlanning(dateTime = newDateTime, dayPeriod = DayPeriod.NONE))
                    .endDateConf(newEndDateConf)
                    .scheduledStartDateTime(null) 
                    .scheduledEndDateTime(null)
                    .build()
                Log.d("PlannerVM", "Task ${task.id} updated (Postponed): New Start $newDateTime")
            }
        }
    }

    private fun applyScheduledPlan(
        state: PlannerState,
        originalTasks: Map<Int, Task>,
        tasksToUpdate: MutableMap<Int, Task>,
    ) {
        state.generatedPlan.forEach { (date, scheduledItems) ->
            scheduledItems.forEach { item ->
                val taskId = item.task.id
                if (state.tasksFlaggedForManualEdit.contains(taskId)) return@forEach

                val taskBeforeScheduling = tasksToUpdate[taskId] ?: originalTasks[taskId]
                if (taskBeforeScheduling != null) {
                    val newScheduledStartTime = LocalDateTime.of(item.date, item.scheduledStartTime)
                    val newScheduledEndTime = LocalDateTime.of(item.date, item.scheduledEndTime)

                    val finalStartDateConf = TimePlanning(
                        dateTime = newScheduledStartTime,
                        dayPeriod = DayPeriod.NONE
                    )

                    tasksToUpdate[taskId] = Task.from(taskBeforeScheduling)
                        .startDateConf(finalStartDateConf)
                        .scheduledStartDateTime(newScheduledStartTime)
                        .scheduledEndDateTime(newScheduledEndTime)
                        .build()
                    Log.d(
                        "PlannerVM",
                        "Task $taskId updated (Scheduled): StartConf=${finalStartDateConf.dateTime}, SchedStart=${newScheduledStartTime}"
                    )
                } else {
                    Log.w("PlannerVM", "Original task $taskId not found for applying schedule.")
                }
            }
        }
    }

    private fun clearUnscheduledOrManualTasks(
        state: PlannerState,
        originalTasks: Map<Int, Task>,
        tasksToUpdate: MutableMap<Int, Task>,
    ) {
        val scheduledTaskIds =
            state.generatedPlan.values.flatMap { list -> list.map { it.task.id } }.toSet()
        val allOriginalTaskIds = originalTasks.keys

        allOriginalTaskIds.forEach { taskId ->
            val isScheduled = scheduledTaskIds.contains(taskId)
            val isManual = state.tasksFlaggedForManualEdit.contains(taskId)
            val isResolvedToMove =
                state.taskResolutions[taskId] == ResolutionOption.MOVE_TO_TOMORROW ||
                        state.conflictResolutions.any { (hash, res) ->
                            res == ResolutionOption.MOVE_TO_TOMORROW &&
                                    state.conflictsToResolve.find { it.hashCode() == hash }?.conflictingTasks?.any { it.id == taskId } == true
                        }

            val shouldClear = (isManual || (!isScheduled && !isResolvedToMove))

            if (shouldClear) {
                val taskToClear = tasksToUpdate[taskId] ?: originalTasks[taskId]
                if (taskToClear != null && (taskToClear.scheduledStartDateTime != null || taskToClear.scheduledEndDateTime != null)) {
                    tasksToUpdate[taskId] = Task.from(taskToClear)
                        .scheduledStartDateTime(null)
                        .scheduledEndDateTime(null)
                        .build()
                    val reason = if (isManual) "Flagged Manual" else "Not in Plan Schedule"
                    Log.d("PlannerVM", "Task $taskId cleared scheduled times ($reason).")
                }
            }
        }
    }

    private suspend fun executeSaveOperations(tasksToUpdate: Map<Int, Task>) {
        if (tasksToUpdate.isNotEmpty()) {
            Log.d("PlannerVM", "Creating ${tasksToUpdate.size} save jobs for updated tasks.")
            val updateJobs = mutableListOf<Deferred<TaskResult<Int>>>()
            for (finalTaskToSave in tasksToUpdate.values) {
                Log.d("PlannerVM", "Async save job for Task ID: ${finalTaskToSave.id}")
                val job: Deferred<TaskResult<Int>> = viewModelScope.async {
                    saveTaskUseCase(finalTaskToSave)
                }
                updateJobs.add(job)
            }

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
            setState { copy(isLoading = false, planSuccessfullyAdded = true) }
            setEffect(PlannerEffect.ShowSnackbar("Plan reviewed. No updates needed."))
            setEffect(PlannerEffect.NavigateBack)
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