package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteAllTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.presentation.effects.TaskEffect
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.utils.NewTaskData
import com.elena.autoplanner.presentation.utils.toTask
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val deleteAllTasksUseCase: DeleteAllTasksUseCase,
    private val filterTasksUseCase: FilterTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
) : BaseTaskViewModel<TaskIntent, TaskState, TaskEffect>() {

    override fun createInitialState(): TaskState = TaskState()

    override suspend fun handleIntent(intent: TaskIntent) {
        when (intent) {
            is TaskIntent.LoadTasks -> loadTasks()
            is TaskIntent.CreateTask -> createTask(intent.newTaskData)
            is TaskIntent.UpdateTask -> updateTask(intent.task)
            is TaskIntent.DeleteTask -> deleteTask(intent.task)
            is TaskIntent.ToggleTaskCompletion -> toggleTaskCompletion(
                intent.task.id,
                intent.checked
            )
            is TaskIntent.UpdateStatusFilter -> updateStatusFilter(intent.status)
            is TaskIntent.UpdateTimeFrameFilter -> updateTimeFrameFilter(intent.timeFrame)
            is TaskIntent.AddSubtask -> addSubtask(intent.task, intent.subtaskName)
            is TaskIntent.ToggleSubtask -> toggleSubtask(
                intent.task,
                intent.subtask,
                intent.checked
            )
            is TaskIntent.DeleteSubtask -> deleteSubtask(intent.task, intent.subtask)
            is TaskIntent.ClearError -> clearError()
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            setEffect(TaskEffect.ShowLoading(true))
            setState { copy(uiState = TaskState.UiState.Loading) }

            getTasksUseCase()
                .catch { error ->
                    setState {
                        copy(
                            uiState = TaskState.UiState.Error(error.message),
                            tasks = emptyList(),
                            filteredTasks = emptyList()
                        )
                    }
                    setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                    setEffect(TaskEffect.ShowLoading(false))
                }
                .collect { tasks ->
                    applyFiltersAndUpdateState(tasks)
                    setEffect(TaskEffect.ShowLoading(false))
                }
        }
    }

    private fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            // Validate task data
            if (newTaskData.name.isBlank()) {
                setState { copy(uiState = TaskState.UiState.Error("Task name cannot be empty")) }
                setEffect(TaskEffect.Error("Task name cannot be empty"))
                return@launch
            }

            // Set loading state
            setState { copy(uiState = TaskState.UiState.Loading) }
            setEffect(TaskEffect.ShowLoading(true))

            try {
                val task = newTaskData.toTask()
                executeTaskOperation(
                    setLoadingState = { /* Loading state already set */ },
                    operation = { saveTaskUseCase(task) },
                    onSuccess = { taskId ->
                        loadTasks()
                        setState { copy(uiState = TaskState.UiState.Success("Task created")) }
                        setEffect(TaskEffect.Success("Task created"))
                    },
                    onError = { errorMessage ->
                        setState { copy(uiState = TaskState.UiState.Error(errorMessage)) }
                        setEffect(TaskEffect.Error(errorMessage))
                        setEffect(TaskEffect.ShowLoading(false))
                    }
                )
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
                setEffect(TaskEffect.Error(e.message ?: "Unknown error"))
                setEffect(TaskEffect.ShowLoading(false))
            }
        }
    }

    private fun toggleTaskCompletion(taskId: Int, completed: Boolean) {
        viewModelScope.launch {
            // Perform optimistic update
            val currentTasks = currentState.tasks
            val updatedTasksList = currentTasks.map {
                if (it.id == taskId) it.copy(isCompleted = completed) else it
            }

            setState {
                copy(
                    tasks = updatedTasksList,
                    filteredTasks = applyFilters(updatedTasksList),
                    uiState = TaskState.UiState.Loading
                )
            }

            executeTaskOperation(
                setLoadingState = { /* Already set optimistically */ },
                operation = { toggleTaskCompletionUseCase(taskId, completed) },
                onSuccess = {
                    setState { copy(uiState = TaskState.UiState.Success("Task updated")) }
                    setEffect(TaskEffect.Success("Task updated"))
                },
                onError = { errorMessage ->
                    // Revert the optimistic update
                    setState {
                        copy(
                            tasks = currentTasks,
                            filteredTasks = applyFilters(currentTasks),
                            uiState = TaskState.UiState.Error(errorMessage)
                        )
                    }
                    setEffect(TaskEffect.Error(errorMessage))
                }
            )
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            setState { copy(uiState = TaskState.UiState.Loading) }
            setEffect(TaskEffect.ShowLoading(true))

            executeTaskOperation(
                setLoadingState = { /* Loading state already set */ },
                operation = { saveTaskUseCase(task) },
                onSuccess = { _ ->
                    val updatedTasks = currentState.tasks.map {
                        if (it.id == task.id) task else it
                    }

                    setState {
                        copy(
                            tasks = updatedTasks,
                            filteredTasks = applyFilters(updatedTasks),
                            uiState = TaskState.UiState.Success("Task updated")
                        )
                    }
                    setEffect(TaskEffect.Success("Task updated"))
                    setEffect(TaskEffect.ShowLoading(false))
                },
                onError = { errorMessage ->
                    setState { copy(uiState = TaskState.UiState.Error(errorMessage)) }
                    setEffect(TaskEffect.Error(errorMessage))
                    setEffect(TaskEffect.ShowLoading(false))
                }
            )
        }
    }

    private fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            setState { copy(uiState = TaskState.UiState.Loading) }
            setEffect(TaskEffect.ShowLoading(true))

            executeTaskOperation(
                setLoadingState = { /* Loading state already set */ },
                operation = { deleteTaskUseCase(taskId) },
                onSuccess = { _ ->
                    val updatedTasks = currentState.tasks.filter { it.id != taskId }

                    setState {
                        copy(
                            tasks = updatedTasks,
                            filteredTasks = applyFilters(updatedTasks),
                            uiState = TaskState.UiState.Success("Task deleted")
                        )
                    }
                    setEffect(TaskEffect.Success("Task deleted"))
                    setEffect(TaskEffect.ShowLoading(false))
                },
                onError = { errorMessage ->
                    setState { copy(uiState = TaskState.UiState.Error(errorMessage)) }
                    setEffect(TaskEffect.Error(errorMessage))
                    setEffect(TaskEffect.ShowLoading(false))
                }
            )
        }
    }

    private fun updateStatusFilter(status: TaskStatus) {
        viewModelScope.launch {
            setState {
                copy(
                    filters = currentState.filters.copy(status = status),
                    filteredTasks = applyFilters(
                        currentState.tasks,
                        status,
                        currentState.filters.timeFrame
                    )
                )
            }
        }
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        viewModelScope.launch {
            setState {
                copy(
                    filters = currentState.filters.copy(timeFrame = timeFrame),
                    filteredTasks = applyFilters(
                        currentState.tasks,
                        currentState.filters.status,
                        timeFrame
                    )
                )
            }
        }
    }


    private fun addSubtask(taskId: Int, subtaskName: String) {
        viewModelScope.launch {
            if (subtaskName.isBlank()) {
                setState { copy(uiState = TaskState.UiState.Error("Subtask name cannot be empty")) }
                setEffect(TaskEffect.Error("Subtask name cannot be empty"))
                return@launch
            }

            executeTaskOperation(
                setLoadingState = { setState { copy(uiState = TaskState.UiState.Loading) } },
                operation = { addSubtaskUseCase(taskId, subtaskName) },
                onSuccess = { updatedTask ->
                    updateTaskInState(taskId, updatedTask, "Subtask added")
                },
                onError = { errorMessage ->
                    setState { copy(uiState = TaskState.UiState.Error(errorMessage)) }
                    setEffect(TaskEffect.Error(errorMessage))
                }
            )
        }
    }


    private fun toggleSubtask(taskId: Int, subtaskId: Int, checked: Boolean) {
        viewModelScope.launch {
            executeTaskOperation(
                setLoadingState = { /* No loading state needed for quick operation */ },
                operation = { toggleSubtaskUseCase(taskId, subtaskId, checked) },
                onSuccess = { updatedTask ->
                    updateTaskInState(taskId, updatedTask)
                },
                onError = { errorMessage ->
                    setState { copy(uiState = TaskState.UiState.Error(errorMessage)) }
                    setEffect(TaskEffect.Error(errorMessage))
                }
            )
        }
    }


    private fun deleteSubtask(taskId: Int, subtaskId: Int) {
        viewModelScope.launch {
            executeTaskOperation(
                setLoadingState = { setState { copy(uiState = TaskState.UiState.Loading) } },
                operation = { deleteSubtaskUseCase(taskId, subtaskId) },
                onSuccess = { updatedTask ->
                    updateTaskInState(taskId, updatedTask, "Subtask deleted")
                },
                onError = { errorMessage ->
                    setState { copy(uiState = TaskState.UiState.Error(errorMessage)) }
                    setEffect(TaskEffect.Error(errorMessage))
                }
            )
        }
    }


    private fun clearError() {
        setState { copy(uiState = TaskState.UiState.Idle) }
    }


    private fun deleteAllTasks() {
        viewModelScope.launch {
            executeTaskOperation(
                setLoadingState = { setState { copy(uiState = TaskState.UiState.Loading) } },
                operation = { deleteAllTasksUseCase() },
                onSuccess = {
                    setState {
                        copy(
                            tasks = emptyList(),
                            filteredTasks = emptyList(),
                            uiState = TaskState.UiState.Success("All tasks deleted")
                        )
                    }
                    setEffect(TaskEffect.Success("All tasks deleted"))
                },
                onError = { errorMessage ->
                    setState { copy(uiState = TaskState.UiState.Error(errorMessage)) }
                    setEffect(TaskEffect.Error(errorMessage))
                }
            )
        }
    }


    private fun updateTaskInState(taskId: Int, updatedTask: Task, successMessage: String? = null) {
        val updatedTasks = currentState.tasks.map {
            if (it.id == taskId) updatedTask else it
        }

        setState {
            copy(
                tasks = updatedTasks,
                filteredTasks = applyFilters(updatedTasks),
                uiState = if (successMessage != null)
                    TaskState.UiState.Success(successMessage)
                else
                    currentState.uiState
            )
        }

        if (successMessage != null) {
            setEffect(TaskEffect.Success(successMessage))
        }
    }


    private fun applyFiltersAndUpdateState(tasks: List<Task>) {
        val filteredTasks = applyFilters(tasks)
        setState {
            copy(
                tasks = tasks,
                filteredTasks = filteredTasks,
                uiState = TaskState.UiState.Success()
            )
        }
    }

    private fun applyFilters(tasks: List<Task>): List<Task> {
        return filterTasksUseCase(
            tasks,
            currentState.filters.status,
            currentState.filters.timeFrame
        )
    }

    private fun applyFilters(
        tasks: List<Task>,
        status: TaskStatus,
        timeFrame: TimeFrame
    ): List<Task> {
        return filterTasksUseCase(tasks, status, timeFrame)
    }


    fun seedTasks(count: Int = 25) {
        viewModelScope.launch {
            deleteAllTasks()

            repeat(count) { index ->
                val dayOffset = (-2..14).random()
                val hour = (6..21).random()
                val minute = listOf(0, 15, 30, 45).random()

                val baseDateTime = LocalDateTime.now()
                    .plusDays(dayOffset.toLong())
                    .withHour(hour)
                    .withMinute(minute)

                val chosenPeriod = listOf(
                    DayPeriod.MORNING,
                    DayPeriod.EVENING,
                    DayPeriod.NIGHT,
                    DayPeriod.ALLDAY,
                    DayPeriod.NONE,
                    DayPeriod.NONE,
                    DayPeriod.NONE
                ).random()

                val adjustedDateTime = when (chosenPeriod) {
                    DayPeriod.MORNING -> baseDateTime.withHour((5..11).random())
                    DayPeriod.EVENING -> baseDateTime.withHour((12..17).random())
                    DayPeriod.NIGHT -> baseDateTime.withHour((18..23).random())
                    DayPeriod.ALLDAY -> baseDateTime.withHour(0).withMinute(0)
                    else -> baseDateTime
                }

                val hasEndDate = (0..1).random() == 0
                val endDateOffset = (1..3).random()
                val endDateTime = if (hasEndDate) {
                    adjustedDateTime.plusDays(endDateOffset.toLong())
                } else null

                val randomDuration = if ((0..1).random() == 0) {
                    listOf(30, 60, 90, 120, 150, 180).random()
                } else null

                val priority = listOf(
                    Priority.HIGH,
                    Priority.MEDIUM,
                    Priority.LOW,
                    Priority.NONE
                ).random()

                val subtasks = if ((0..1).random() == 0) {
                    (1..(1..3).random()).map { subIndex ->
                        Subtask(
                            id = 0,
                            name = "Subtask #$subIndex of Task #$index",
                            isCompleted = false,
                            estimatedDurationInMinutes = listOf(15, 30, 45, 60).random()
                        )
                    }
                } else emptyList()

                val sampleName = generateConfigBasedTaskName(
                    index = index,
                    dayOffset = dayOffset,
                    chosenPeriod = chosenPeriod,
                    hasEndDate = hasEndDate,
                    durationMinutes = randomDuration,
                    priority = priority,
                    subtaskCount = subtasks.size
                )

                val sampleTask = NewTaskData(
                    name = sampleName,
                    priority = priority,
                    startDateConf = TimePlanning(
                        dateTime = adjustedDateTime,
                        dayPeriod = chosenPeriod
                    ),
                    endDateConf = endDateTime?.let {
                        TimePlanning(it, dayPeriod = DayPeriod.NONE)
                    },
                    durationConf = randomDuration?.let { DurationPlan(it) },
                    subtasks = subtasks
                )

                createTask(sampleTask)
            }
        }
    }

    private fun generateConfigBasedTaskName(
        index: Int,
        dayOffset: Int,
        chosenPeriod: DayPeriod,
        hasEndDate: Boolean,
        durationMinutes: Int?,
        priority: Priority,
        subtaskCount: Int
    ): String {
        val sign = if (dayOffset >= 0) "+" else ""
        val offsetDescriptor = "start in $sign$dayOffset days"
        val periodDescriptor = "Period=${chosenPeriod.name}"
        val durationDescriptor = durationMinutes?.let { "${it}min" } ?: "--"
        val endDateDescriptor = if (hasEndDate) "yes" else "no"
        val priorityLabel = "Priority=${priority.name}"
        val subtaskDescriptor = subtaskCount.toString()

        return "Task #$index ($priorityLabel) [$offsetDescriptor, $periodDescriptor, Duration=$durationDescriptor, EndDate=$endDateDescriptor, Subtasks=$subtaskDescriptor]"
    }
}