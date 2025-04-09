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


class PlannerViewModel(
    private val generatePlanUseCase: GeneratePlanUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) : BaseViewModel<PlannerIntent, PlannerState, PlannerEffect>() {

    override fun createInitialState(): PlannerState = PlannerState(isLoading = true)

    init {
        // Load initial data like overdue count when ViewModel is created
        viewModelScope.launch {
            setState {
                copy(
                    isLoading = true,
                    error = null
                )
            } // Ensure loading state and clear previous error
            var finalOverdueCount = 0
            var errorMessage: String? = null

            try {
                // Use firstOrNull to handle empty flow safely
                val tasks: List<Task>? = getTasksUseCase().firstOrNull()

                if (tasks != null) {
                    finalOverdueCount = tasks.count { !it.isCompleted && it.isExpired() }
                    Log.d("PlannerVM", "Initial overdue count = $finalOverdueCount")
                } else {
                    Log.d("PlannerVM", "Initial task list was null or empty.")
                    // Optionally set an error or message if tasks are expected but missing
                    // errorMessage = "Could not load tasks."
                }
            } catch (e: Exception) {
                Log.e("PlannerVM", "Error loading initial tasks", e)
                errorMessage = "Error loading initial tasks: ${e.localizedMessage}"
                // Set effect might be better here if the screen is already visible
                // setEffect(PlannerEffect.ShowSnackbar(errorMessage))
            } finally {
                // Always update state, ensuring loading is false
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
            is PlannerIntent.SelectPriority -> {
                setState { copy(selectedPriority = intent.priority) }
            }

            is PlannerIntent.SelectDayOrganization -> setState { copy(selectedDayOrganization = intent.organization) }
            is PlannerIntent.SelectShowSubtasks -> setState { copy(showSubtasksWithDuration = intent.show) }
            is PlannerIntent.SelectOverdueHandling -> setState { copy(selectedOverdueHandling = intent.handling) }
            is PlannerIntent.GeneratePlan -> executePlanGeneration()
            is PlannerIntent.ResolveExpiredTask -> handleExpiredResolution(
                intent.task,
                intent.resolution
            )

            is PlannerIntent.ResolveConflict -> handleConflictResolution(
                intent.conflict,
                intent.resolution
            )

            is PlannerIntent.AddPlanToCalendar -> savePlan()
            is PlannerIntent.CancelPlanner -> setEffect(PlannerEffect.NavigateBack)
        }
    }

    private fun handleNextStep() {
        val currentState = currentState
        when (currentState.currentStep) {
            PlannerStep.TIME_INPUT -> if (currentState.canMoveToStep2) {
                setState { copy(currentStep = PlannerStep.PRIORITY_INPUT) }
            } else {
                setEffect(PlannerEffect.ShowSnackbar("Please select availability hours and a schedule scope."))
            }

            PlannerStep.PRIORITY_INPUT -> if (currentState.canMoveToStep3) { // State logic now checks selectedPriority
                setState { copy(currentStep = PlannerStep.ADDITIONAL_OPTIONS) }
            } else {
                setEffect(PlannerEffect.ShowSnackbar("Please select a prioritization strategy and day organization style."))
            }

            PlannerStep.ADDITIONAL_OPTIONS -> if (currentState.canGeneratePlan) {
                sendIntent(PlannerIntent.GeneratePlan)
            } else {
                setEffect(PlannerEffect.ShowSnackbar("Please select task splitting and overdue task options."))
            }

            PlannerStep.REVIEW_PLAN -> { /* Final step */
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
                    planSuccessfullyAdded = false
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
                    planSuccessfullyAdded = false
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
                val scope = currentState.scheduleScope!!
                val org = currentState.selectedDayOrganization!!
                val priority = currentState.selectedPriority!!
                val showSubtasks = currentState.showSubtasksWithDuration!!
                val overdueHandling =
                    if (currentState.numOverdueTasks > 0) currentState.selectedOverdueHandling!! else OverdueTaskHandling.ADD_TODAY_FREE_TIME // !! safe if overdue > 0

                val plannerInput = PlannerInput(
                    tasks = allTasks.filter { !it.isCompleted },
                    workStartTime = currentState.workStartTime,
                    workEndTime = currentState.workEndTime,
                    scheduleScope = scope,
                    prioritizationStrategy = priority,
                    dayOrganization = org,
                    showSubtasksWithDuration = showSubtasks,
                    overdueTaskHandling = overdueHandling
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
                        currentStep = PlannerStep.REVIEW_PLAN,
                        taskResolutions = emptyMap(),
                        conflictResolutions = emptyMap()
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
        // Update the resolution map in the state
        setState {
            copy(taskResolutions = taskResolutions + (task.id to resolution))
        }
        Log.d(
            "PlannerVM",
            "Resolved Expired Task ${task.id} with $resolution. Current resolutions: ${currentState.taskResolutions}"
        )
    }

    private fun handleConflictResolution(conflict: ConflictItem, resolution: ResolutionOption) {
        // Using hashCode as key - be aware of potential limitations if ConflictItem structure changes frequently
        // or if identical conflicts can occur. A unique ID per conflict might be more robust.
        val conflictId = conflict.hashCode()
        setState {
            copy(conflictResolutions = conflictResolutions + (conflictId to resolution))
        }
        Log.d(
            "PlannerVM",
            "Resolved Conflict $conflictId with $resolution. Current resolutions: ${currentState.conflictResolutions}"
        )
    }


    private fun savePlan() {
        viewModelScope.launch {
            val state = currentState // Capture current state for processing

            // Final check: Ensure all resolutions are made before attempting to save
            if (state.requiresResolution) {
                Log.w("PlannerVM", "Attempted to save plan while resolutions are required.")
                setEffect(PlannerEffect.ShowSnackbar("Please resolve all expired tasks and conflicts first."))
                return@launch
            }

            Log.d("PlannerVM", "Starting savePlan...")
            setState {
                copy(
                    isLoading = true,
                    error = null,
                    planSuccessfullyAdded = false
                )
            } // Set loading, clear error

            try {
                val updateJobs = mutableListOf<Deferred<TaskResult<Int>>>()

                // 1. Process Scheduled Tasks from the generated plan
                state.generatedPlan.forEach { (date, items) ->
                    items.forEach { scheduledItem ->
                        val originalTask = scheduledItem.task
                        val newStartTime = LocalDateTime.of(date, scheduledItem.scheduledStartTime)

                        // Only schedule update if time or date actually changed
                        if (originalTask.startDateConf.dateTime != newStartTime || originalTask.startDateConf.dayPeriod != DayPeriod.NONE) {
                            Log.i(
                                "PlannerVM",
                                "Updating Task ${originalTask.id} scheduled time to $newStartTime"
                            )
                            val updatedTask = Task.from(originalTask)
                                .startDateConf(
                                    TimePlanning(
                                        dateTime = newStartTime,
                                        dayPeriod = DayPeriod.NONE
                                    )
                                ) // Use specific time, clear period
                                // Optionally update duration if planner adjusted it (though current use case doesn't modify duration)
                                // .durationConf(...)
                                .build()
                            updateJobs.add(async { saveTaskUseCase(updatedTask) })
                        } else {
                            Log.d(
                                "PlannerVM",
                                "Task ${originalTask.id} scheduled time ($newStartTime) unchanged, skipping save."
                            )
                        }
                    }
                }

                // 2. Process Expired Task Resolutions
                state.taskResolutions.forEach { (taskId, resolution) ->
                    val task =
                        state.expiredTasksToResolve.find { it.id == taskId } ?: return@forEach
                    val tomorrow = LocalDate.now().plusDays(1)
                    val keepTime = task.startDateConf.dateTime?.toLocalTime() ?: state.workStartTime
                    val newTaskTime = LocalDateTime.of(tomorrow, keepTime)

                    when (resolution) {
                        ResolutionOption.MOVE_TO_TOMORROW,
                        ResolutionOption.MOVE_TO_NEAREST_FREE,
                        -> {
                            val updatedTask = Task.from(task)
                                .startDateConf(
                                    TimePlanning(
                                        dateTime = newTaskTime,
                                        dayPeriod = DayPeriod.NONE
                                    )
                                )
                                .endDateConf(null)
                                .build()
                            updateJobs.add(async { saveTaskUseCase(updatedTask) })
                        }

                        ResolutionOption.MANUALLY_SCHEDULE, ResolutionOption.LEAVE_IT_LIKE_THAT -> {
                        }

                        ResolutionOption.RESOLVED -> {
                        }
                    }
                }

                // 3. Process Conflict Resolutions
                state.conflictResolutions.forEach { (conflictHash, resolution) ->
                    val conflict = state.conflictsToResolve.find { it.hashCode() == conflictHash }
                        ?: return@forEach
                    val taskToModify = conflict.conflictingTasks.minByOrNull { it.priority.ordinal }

                    if (taskToModify != null) {
                        val tomorrow = LocalDate.now().plusDays(1)
                        val keepTime = taskToModify.startDateConf.dateTime?.toLocalTime()
                            ?: state.workStartTime
                        val newTaskTime = LocalDateTime.of(tomorrow, keepTime)

                        when (resolution) {
                            ResolutionOption.MOVE_TO_TOMORROW,
                            ResolutionOption.MOVE_TO_NEAREST_FREE,
                            -> {
                                Log.i(
                                    "PlannerVM",
                                    "Resolving Conflict $conflictHash by moving Task ${taskToModify.id} to $newTaskTime"
                                )
                                val updatedTask = Task.from(taskToModify)
                                    .startDateConf(
                                        TimePlanning(
                                            dateTime = newTaskTime,
                                            dayPeriod = DayPeriod.NONE
                                        )
                                    )
                                    .endDateConf(null)
                                    .build()
                                updateJobs.add(async { saveTaskUseCase(updatedTask) })
                            }
                            ResolutionOption.MANUALLY_SCHEDULE, ResolutionOption.LEAVE_IT_LIKE_THAT -> {
                            }

                            ResolutionOption.RESOLVED -> {
                            }
                        }
                    } else {
                    }
                }

                val results: List<TaskResult<Int>> = updateJobs.awaitAll()
                val errors = results.filterIsInstance<TaskResult.Error>()

                if (errors.isNotEmpty()) {
                    setState {
                        copy(
                            isLoading = false,
                            error = "Some tasks failed to save. See logs for details."
                        )
                    }
                    setEffect(PlannerEffect.ShowSnackbar("Error saving parts of the plan."))
                } else {
                    setState { copy(isLoading = false, planSuccessfullyAdded = true) }
                    setEffect(PlannerEffect.ShowSnackbar("Plan added successfully!"))
                    setEffect(PlannerEffect.NavigateBack) // Navigate back after successful save
                }

            } catch (e: Exception) {
                Log.e("PlannerVM", "Failed to save plan", e)
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
}