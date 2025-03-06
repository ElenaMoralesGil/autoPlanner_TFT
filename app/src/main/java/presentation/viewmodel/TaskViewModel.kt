package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteAllTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.presentation.utils.NewTaskData
import com.elena.autoplanner.presentation.utils.toTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * TaskViewModel serves as a data provider for task-related data across the app.
 * While specialized ViewModels (TaskListViewModel, TaskDetailViewModel, TaskEditViewModel)
 * handle specific screens, this ViewModel provides data access for components that
 * don't have a dedicated ViewModel or need broad access to task data.
 */
class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val deleteAllTasksUseCase: DeleteAllTasksUseCase
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            getTasksUseCase()
                .onStart { _isLoading.value = true }
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { fetchedTasks ->
                    _tasks.value = fetchedTasks
                    _isLoading.value = false
                }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                saveTaskUseCase(task).fold(
                    onSuccess = { _ ->
                        // Update local tasks list
                        _tasks.value = _tasks.value.map {
                            if (it.id == task.id) task else it
                        }
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            try {
                if (newTaskData.name.isBlank()) {
                    _error.value = "Task name cannot be empty"
                    return@launch
                }

                _isLoading.value = true
                val task = newTaskData.toTask()

                saveTaskUseCase(task).fold(
                    onSuccess = { taskId ->
                        val savedTask = task.copy(id = taskId)
                        _tasks.value = _tasks.value + savedTask
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                deleteTaskUseCase(taskId).fold(
                    onSuccess = {
                        _tasks.value = _tasks.value.filter { it.id != taskId }
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubtask(taskId: Int, subtaskName: String) {
        viewModelScope.launch {
            try {
                if (subtaskName.isBlank()) {
                    _error.value = "Subtask name cannot be empty"
                    return@launch
                }

                _isLoading.value = true

                addSubtaskUseCase(taskId, subtaskName).fold(
                    onSuccess = { updatedTask ->
                        _tasks.value = _tasks.value.map {
                            if (it.id == taskId) updatedTask else it
                        }
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSubtask(taskId: Int, subtaskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                toggleSubtaskUseCase(taskId, subtaskId, isCompleted).fold(
                    onSuccess = { updatedTask ->
                        _tasks.value = _tasks.value.map {
                            if (it.id == taskId) updatedTask else it
                        }
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSubtask(taskId: Int, subtaskId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                deleteSubtaskUseCase(taskId, subtaskId).fold(
                    onSuccess = { updatedTask ->
                        _tasks.value = _tasks.value.map {
                            if (it.id == taskId) updatedTask else it
                        }
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                deleteAllTasksUseCase().fold(
                    onSuccess = {
                        _tasks.value = emptyList()
                    },
                    onFailure = { e ->
                        _error.value = e.message
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Development utility function to generate sample tasks
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
        // Format: "Task #3 (Priority=HIGH) [Start in +5 days, Period=EVENING, Duration=60min, EndDate=yes, Subtasks=2]"

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