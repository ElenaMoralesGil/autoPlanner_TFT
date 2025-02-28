package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.usecases.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.AddTaskUseCase
import com.elena.autoplanner.domain.usecases.DeleteAllTasksUseCase
import com.elena.autoplanner.domain.usecases.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.UpdateTaskUseCase
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.utils.BaseViewModel
import com.elena.autoplanner.presentation.utils.NewTaskData
import com.elena.autoplanner.presentation.utils.isDueThisMonth
import com.elena.autoplanner.presentation.utils.isDueThisWeek
import com.elena.autoplanner.presentation.utils.isDueToday
import com.elena.autoplanner.presentation.utils.toTask
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val DeleteAllTasksUseCase: DeleteAllTasksUseCase
) : BaseViewModel<TaskIntent, TaskState>() {

    override fun createInitialState(): TaskState = TaskState(
        filters = TaskState.Filters(timeFrame = TimeFrame.TODAY, status = TaskStatus.ALL)
    )

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
            TaskIntent.ClearError -> clearError()
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            getTasksUseCase()
                .onStart { setState { copy(uiState = TaskState.UiState.Loading) } }
                .catch { e -> setState { copy(uiState = TaskState.UiState.Error(e.message)) } }
                .collect { tasks ->
                    setState {
                        copy(
                            tasks = tasks,
                            filteredTasks = applyFilters(tasks, filters),
                            uiState = TaskState.UiState.Idle
                        )
                    }
                }
        }
    }

    private fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            try {
                if (newTaskData.name.isBlank()) {
                    setState { copy(uiState = TaskState.UiState.Error("Task name cannot be empty")) }
                    return@launch
                }

                val task = newTaskData.toTask()
                val newTaskList = currentState.tasks.toMutableList().apply { add(task) }

                setState {
                    copy(
                        tasks = newTaskList,
                        filteredTasks = applyFilters(newTaskList, filters),
                        uiState = TaskState.UiState.Success("Task created")
                    )
                }

                addTaskUseCase(task)
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }


    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                setState { copy(uiState = TaskState.UiState.Loading) }
                updateTaskUseCase(task)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task.id) task else it },
                        filteredTasks = applyFilters(
                            currentState.tasks,
                            currentState.filters
                        ),
                        uiState = TaskState.UiState.Success("Task updated")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    private fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                val task = currentState.tasks.find { it.id == taskId }
                    ?: throw IllegalArgumentException("Task not found")
                setState { copy(uiState = TaskState.UiState.Loading) }
                deleteTaskUseCase(task)
                setState {
                    copy(
                        tasks = currentState.tasks - task,
                        filteredTasks = applyFilters(
                            currentState.tasks - task,
                            currentState.filters
                        ),
                        uiState = TaskState.UiState.Success("Task deleted")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    private fun toggleTaskCompletion(task: Task, checked: Boolean) {
        viewModelScope.launch {
            try {

                val updatedTask = task.copy(isCompleted = checked)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task.id) updatedTask else it },
                        filteredTasks = applyFilters(currentState.tasks, currentState.filters),
                        uiState = TaskState.UiState.Idle
                    )
                }

                // Then perform actual update
                updateTaskUseCase(updatedTask)
            } catch (e: Exception) {
                // Rollback UI state on error
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task.id) task else it },
                        uiState = TaskState.UiState.Error(e.message)
                    )
                }
            }
        }
    }

    fun addSubtask(task: Int, subtaskName: String) {
        viewModelScope.launch {
            try {
                if (subtaskName.isBlank()) {
                    setState { copy(uiState = TaskState.UiState.Error(R.string.taskError.toString())) }
                    return@launch
                }

                setState { copy(uiState = TaskState.UiState.Loading) }
                val updatedTask = addSubtaskUseCase(task, subtaskName)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task) updatedTask else it },
                        filteredTasks = applyFilters(currentState.tasks, currentState.filters),
                        uiState = TaskState.UiState.Success("Subtask added")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    fun toggleSubtask(taskId: Int, subtaskId: Int, checked: Boolean) {
        viewModelScope.launch {
            try {
                val updatedTask = toggleSubtaskUseCase(taskId, subtaskId, checked)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == taskId) updatedTask else it },
                        filteredTasks = applyFilters(currentState.tasks, currentState.filters),
                        uiState = TaskState.UiState.Idle
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    fun deleteSubtask(task: Int, subtask: Int) {
        viewModelScope.launch {
            try {
                setState { copy(uiState = TaskState.UiState.Loading) }
                val updatedTask = deleteSubtaskUseCase(task, subtask)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task) updatedTask else it },
                        filteredTasks = applyFilters(currentState.tasks, currentState.filters),
                        uiState = TaskState.UiState.Success("Subtask deleted")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    private fun updateStatusFilter(status: TaskStatus) {
        setState {
            val newFilters = filters.copy(status = status)
            copy(
                filters = newFilters,
                filteredTasks = applyFilters(tasks, newFilters)
            )
        }
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        setState {
            val newFilters = filters.copy(timeFrame = timeFrame)
            copy(
                filters = newFilters,
                filteredTasks = applyFilters(tasks, newFilters)
            )
        }
    }

    private fun applyFilters(tasks: List<Task>, filters: TaskState.Filters): List<Task> {
        val expiredTasks = tasks.filter { it.isExpired() }
        val nonExpiredTasks = tasks.filter { !it.isExpired() }

        val timeFilteredTasks = when (filters.timeFrame) {
            TimeFrame.TODAY -> nonExpiredTasks.filter { it.isDueToday() }
            TimeFrame.WEEK -> nonExpiredTasks.filter { it.isDueThisWeek() }
            TimeFrame.MONTH -> nonExpiredTasks.filter { it.isDueThisMonth() }
            TimeFrame.ALL -> nonExpiredTasks
            TimeFrame.EXPIRED -> expiredTasks
        }

        return (timeFilteredTasks + expiredTasks).filter { task ->
            when (filters.status) {
                TaskStatus.COMPLETED -> task.isCompleted
                TaskStatus.UNCOMPLETED -> !task.isCompleted
                TaskStatus.ALL -> true
            }
        }
    }


    private fun clearError() {
        setState { copy(uiState = TaskState.UiState.Idle) }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            try {
                setState { copy(uiState = TaskState.UiState.Loading) }

                DeleteAllTasksUseCase()

                setState {
                    copy(
                        tasks = emptyList(),
                        filteredTasks = emptyList(),
                        uiState = TaskState.UiState.Success("All tasks deleted")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
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

                // Adjust the time if it's MORNING, EVENING, or NIGHT
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
                            id = 0, // DB will auto-generate
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
        /*
         Format suggestions:
           "Task #3 (Priority=HIGH) [Start in +5 days, Period=EVENING, Duration=60min, EndDate=yes, Subtasks=2]"
           or
           "Task #1 (Priority=LOW) [Start in -2 days, Period=NONE, Duration=--, EndDate=no, Subtasks=0]"
        */

        // "start in +X days" or "start in -X days"
        val sign = if (dayOffset >= 0) "+" else ""
        val offsetDescriptor = "start in $sign$dayOffset days"

        // Day period, or "NONE"
        val periodDescriptor = "Period=${chosenPeriod.name}"

        // Duration, or none
        val durationDescriptor = durationMinutes?.let { "${it}min" } ?: "--"

        // End date
        val endDateDescriptor = if (hasEndDate) "yes" else "no"

        // Priority
        val priorityLabel = "Priority=${priority.name}"

        // Subtasks
        val subtaskDescriptor = subtaskCount.toString()

        return "Task #$index ($priorityLabel) [$offsetDescriptor, $periodDescriptor, Duration=$durationDescriptor, EndDate=$endDateDescriptor, Subtasks=$subtaskDescriptor]"
    }




}