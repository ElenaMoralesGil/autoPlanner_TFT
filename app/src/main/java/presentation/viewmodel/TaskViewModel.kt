package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteAllTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.DeleteSubtaskUseCase
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

/**
 * Central ViewModel that serves as a data provider across the app
 * Handles global task operations that don't belong to specific screens
 */
class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val deleteAllTasksUseCase: DeleteAllTasksUseCase,
    private val filterTasksUseCase: FilterTasksUseCase
) : BaseViewModel<TaskIntent, TaskState, TaskState.UiState>() {

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
            setState { copy(uiState = TaskState.UiState.Loading) }

            getTasksUseCase()
                .onStart { setState { copy(uiState = TaskState.UiState.Loading) } }
                .catch { error ->
                    setState {
                        copy(
                            uiState = TaskState.UiState.Error(error.message),
                            tasks = emptyList(),
                            filteredTasks = emptyList()
                        )
                    }
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
                }
        }
    }

    private fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            if (newTaskData.name.isBlank()) {
                setState { copy(uiState = TaskState.UiState.Error("Task name cannot be empty")) }
                return@launch
            }

            setState { copy(uiState = TaskState.UiState.Loading) }

            try {
                val task = toTask(newTaskData)
                saveTaskUseCase(task).fold(
                    onSuccess = { taskId ->
                        // Reload tasks to get the newly created task with its ID
                        loadTasks()
                        setState { copy(uiState = TaskState.UiState.Success("Task created")) }
                    },
                    onFailure = { error ->
                        setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                    }
                )
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            setState { copy(uiState = TaskState.UiState.Loading) }

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
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                }
            )
        }
    }

    private fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            setState { copy(uiState = TaskState.UiState.Loading) }

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
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                }
            )
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
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
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
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                }
            )
        }
    }

    private fun clearError() {
        setState { copy(uiState = TaskState.UiState.Idle) }
    }

    // Method for development testing only
    fun seedTasks(count: Int = 25) {
        viewModelScope.launch {
            deleteAllTasks()
            // Rest of the seeding logic remains the same...
        }
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
                },
                onFailure = { error ->
                    setState { copy(uiState = TaskState.UiState.Error(error.message)) }
                }
            )
        }
    }
}