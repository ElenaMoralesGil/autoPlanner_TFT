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
import com.elena.autoplanner.presentation.effects.TaskEffect
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.utils.BaseViewModel
import com.elena.autoplanner.presentation.utils.NewTaskData
import com.elena.autoplanner.presentation.utils.toTask
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
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
    private val filterTasksUseCase: FilterTasksUseCase
) : BaseViewModel<TaskIntent, TaskState, TaskEffect>() {

    override fun createInitialState(): TaskState = TaskState()

    override suspend fun handleIntent(intent: TaskIntent) {
        when (intent) {
            is TaskIntent.LoadTasks -> loadTasks()
            is TaskIntent.CreateTask -> createTask(intent.newTaskData)
            is TaskIntent.UpdateTask -> updateTask(intent.task)
            is TaskIntent.DeleteTask -> deleteTask(intent.task)
            is TaskIntent.ToggleTaskCompletion -> toggleTaskCompletion(intent.task, intent.checked)
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
                }
                .collect { tasks ->
                    // Apply current filters
                    val filteredTasks = filterTasksUseCase(
                        tasks,
                        currentState.filters.status,
                        currentState.filters.timeFrame
                    )

                    setState {
                        copy(
                            tasks = tasks,
                            filteredTasks = filteredTasks,
                            uiState = TaskState.UiState.Success()
                        )
                    }
                    setEffect(TaskEffect.ShowLoading(false))
                }
        }
    }

    private fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            if (newTaskData.name.isBlank()) {
                setState { copy(uiState = TaskState.UiState.Error("Task name cannot be empty")) }
                setEffect(TaskEffect.Error("Task name cannot be empty"))
                return@launch
            }

            setState { copy(uiState = TaskState.UiState.Loading) }
            setEffect(TaskEffect.ShowLoading(true))

            try {
                val task = newTaskData.toTask()
                saveTaskUseCase(task).fold(
                    onSuccess = { taskId ->
                        // Reload tasks to get the newly created task with its ID
                        loadTasks()
                        setState { copy(uiState = TaskState.UiState.Success("Task created")) }
                        setEffect(TaskEffect.Success("Task created"))
                    },
                    onFailure = { error ->
                        setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                        setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                    }
                )
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
                setEffect(TaskEffect.Error(e.message ?: "Unknown error"))
            } finally {
                setEffect(TaskEffect.ShowLoading(false))
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            setState { copy(uiState = TaskState.UiState.Loading) }
            setEffect(TaskEffect.ShowLoading(true))

            saveTaskUseCase(task).fold(
                onSuccess = { _ ->
                    val updatedTasks = currentState.tasks.map {
                        if (it.id == task.id) task else it
                    }

                    val filteredTasks = filterTasksUseCase(
                        updatedTasks,
                        currentState.filters.status,
                        currentState.filters.timeFrame
                    )

                    setState {
                        copy(
                            tasks = updatedTasks,
                            filteredTasks = filteredTasks,
                            uiState = TaskState.UiState.Success("Task updated")
                        )
                    }
                    setEffect(TaskEffect.Success("Task updated"))
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                    setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                }
            )
            setEffect(TaskEffect.ShowLoading(false))
        }
    }

    private fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            setState { copy(uiState = TaskState.UiState.Loading) }
            setEffect(TaskEffect.ShowLoading(true))

            deleteTaskUseCase(taskId).fold(
                onSuccess = { _ ->
                    val updatedTasks = currentState.tasks.filter { it.id != taskId }
                    val filteredTasks = filterTasksUseCase(
                        updatedTasks,
                        currentState.filters.status,
                        currentState.filters.timeFrame
                    )

                    setState {
                        copy(
                            tasks = updatedTasks,
                            filteredTasks = filteredTasks,
                            uiState = TaskState.UiState.Success("Task deleted")
                        )
                    }
                    setEffect(TaskEffect.Success("Task deleted"))
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                    setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                }
            )
            setEffect(TaskEffect.ShowLoading(false))
        }
    }

    private fun toggleTaskCompletion(task: Task, checked: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = checked)
            updateTask(updatedTask)
        }
    }

    private fun updateStatusFilter(status: TaskStatus) {
        viewModelScope.launch {
            val filteredTasks = filterTasksUseCase(
                currentState.tasks,
                status,
                currentState.filters.timeFrame
            )

            setState {
                copy(
                    filters = currentState.filters.copy(status = status),
                    filteredTasks = filteredTasks
                )
            }
        }
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        viewModelScope.launch {
            val filteredTasks = filterTasksUseCase(
                currentState.tasks,
                currentState.filters.status,
                timeFrame
            )

            setState {
                copy(
                    filters = currentState.filters.copy(timeFrame = timeFrame),
                    filteredTasks = filteredTasks
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

            addSubtaskUseCase(taskId, subtaskName).fold(
                onSuccess = { updatedTask ->
                    val updatedTasks = currentState.tasks.map {
                        if (it.id == taskId) updatedTask else it
                    }

                    val filteredTasks = filterTasksUseCase(
                        updatedTasks,
                        currentState.filters.status,
                        currentState.filters.timeFrame
                    )

                    setState {
                        copy(
                            tasks = updatedTasks,
                            filteredTasks = filteredTasks,
                            uiState = TaskState.UiState.Success("Subtask added")
                        )
                    }
                    setEffect(TaskEffect.Success("Subtask added"))
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                    setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                }
            )
        }
    }

    private fun toggleSubtask(taskId: Int, subtaskId: Int, checked: Boolean) {
        viewModelScope.launch {
            toggleSubtaskUseCase(taskId, subtaskId, checked).fold(
                onSuccess = { updatedTask ->
                    val updatedTasks = currentState.tasks.map {
                        if (it.id == taskId) updatedTask else it
                    }

                    val filteredTasks = filterTasksUseCase(
                        updatedTasks,
                        currentState.filters.status,
                        currentState.filters.timeFrame
                    )

                    setState {
                        copy(
                            tasks = updatedTasks,
                            filteredTasks = filteredTasks
                        )
                    }
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                    setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                }
            )
        }
    }

    private fun deleteSubtask(taskId: Int, subtaskId: Int) {
        viewModelScope.launch {
            deleteSubtaskUseCase(taskId, subtaskId).fold(
                onSuccess = { updatedTask ->
                    val updatedTasks = currentState.tasks.map {
                        if (it.id == taskId) updatedTask else it
                    }

                    val filteredTasks = filterTasksUseCase(
                        updatedTasks,
                        currentState.filters.status,
                        currentState.filters.timeFrame
                    )

                    setState {
                        copy(
                            tasks = updatedTasks,
                            filteredTasks = filteredTasks,
                            uiState = TaskState.UiState.Success("Subtask deleted")
                        )
                    }
                    setEffect(TaskEffect.Success("Subtask deleted"))
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                    setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                }
            )
        }
    }

    private fun clearError() {
        setState { copy(uiState = TaskState.UiState.Idle) }
    }

    private fun deleteAllTasks() {
        viewModelScope.launch {
            deleteAllTasksUseCase().fold(
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
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                    setEffect(TaskEffect.Error(error.message ?: "Unknown error"))
                }
            )
        }
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

                val priority =
                    listOf(Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.NONE).random()
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