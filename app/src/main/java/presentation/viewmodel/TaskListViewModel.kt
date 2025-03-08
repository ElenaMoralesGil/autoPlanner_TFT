package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.presentation.effects.TaskListEffect
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.states.TaskListState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


class TaskListViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val filterTasksUseCase: FilterTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase
) : BaseTaskViewModel<TaskListIntent, TaskListState, TaskListEffect>() {

    override fun createInitialState(): TaskListState = TaskListState()

    override suspend fun handleIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.LoadTasks -> loadTasks()
            is TaskListIntent.UpdateStatusFilter -> updateStatusFilter(intent.status)
            is TaskListIntent.UpdateTimeFrameFilter -> updateTimeFrameFilter(intent.timeFrame)
            is TaskListIntent.ToggleTaskCompletion -> toggleTaskCompletion(
                intent.taskId,
                intent.completed
            )
            is TaskListIntent.SelectTask -> setEffect(TaskListEffect.NavigateToTaskDetail(intent.taskId))
            is TaskListIntent.DeleteTask -> handleDeleteTask(intent.taskId)
            is TaskListIntent.UpdateTask -> handleUpdateTask(intent.taskId)
        }
    }

    private fun loadTasks() {
        super.loadTasks(
            getTasksUseCase = getTasksUseCase,
            setLoadingState = { isLoading ->
                setState { copy(isLoading = isLoading) }
            },
            processResult = { tasks ->
                applyFilters(tasks)
            },
            handleError = { error ->
                setState { copy(isLoading = false, error = error.message) }
                setEffect(TaskListEffect.ShowSnackbar("Error loading tasks: ${error.message}"))
            }
        )
    }

    private fun applyFilters(tasks: List<Task>) {
        val filteredTasks = filterTasksUseCase(
            tasks,
            currentState.statusFilter,
            currentState.timeFrameFilter
        )

        setState {
            copy(
                isLoading = false,
                tasks = tasks,
                filteredTasks = filteredTasks,
                error = null
            )
        }
    }

    private fun updateStatusFilter(status: TaskStatus) {
        viewModelScope.launch {
            val filteredTasks = filterTasksUseCase(
                currentState.tasks,
                status,
                currentState.timeFrameFilter
            )

            setState {
                copy(
                    statusFilter = status,
                    filteredTasks = filteredTasks
                )
            }
        }
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        viewModelScope.launch {
            val filteredTasks = filterTasksUseCase(
                currentState.tasks,
                currentState.statusFilter,
                timeFrame
            )

            setState {
                copy(
                    timeFrameFilter = timeFrame,
                    filteredTasks = filteredTasks
                )
            }
        }
    }

    private fun toggleTaskCompletion(taskId: Int, completed: Boolean) {
        viewModelScope.launch {
            updateTaskCompletionInState(taskId, completed)

            executeTaskOperation(
                setLoadingState = { },
                operation = { toggleTaskCompletionUseCase(taskId, completed) },
                onSuccess = {
                    if (completed) {
                        setEffect(TaskListEffect.ShowSnackbar("Task completed"))
                    } else {
                        setEffect(TaskListEffect.ShowSnackbar("Task marked as incomplete"))
                    }
                },
                onError = { errorMessage ->
                    updateTaskCompletionInState(taskId, !completed)
                    setState { copy(error = errorMessage) }
                    setEffect(TaskListEffect.ShowSnackbar(errorMessage))
                }
            )
        }
    }

    private fun updateTaskCompletionInState(taskId: Int, completed: Boolean) {
        val currentTasks = currentState.tasks
        val updatedTasksList = currentTasks.map {
            if (it.id == taskId) it.copy(isCompleted = completed) else it
        }

        val filteredTasks = filterTasksUseCase(
            updatedTasksList,
            currentState.statusFilter,
            currentState.timeFrameFilter
        )

        setState {
            copy(
                tasks = updatedTasksList,
                filteredTasks = filteredTasks
            )
        }
    }

    private fun handleDeleteTask(taskId: Int) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }

            val taskToDelete = currentState.tasks.find { it.id == taskId }
            val updatedTasks = currentState.tasks.filter { it.id != taskId }

            setState {
                copy(
                    tasks = updatedTasks,
                    filteredTasks = filterTasksUseCase(
                        updatedTasks,
                        statusFilter,
                        timeFrameFilter
                    )
                )
            }

            executeTaskOperation(
                setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                operation = { deleteTaskUseCase(taskId) },
                onSuccess = {
                    setEffect(TaskListEffect.ShowSnackbar("Task deleted successfully"))
                },
                onError = { errorMessage ->
                    taskToDelete?.let {
                        val restoredTasks = currentState.tasks + it
                        setState {
                            copy(
                                tasks = restoredTasks,
                                filteredTasks = filterTasksUseCase(
                                    restoredTasks,
                                    statusFilter,
                                    timeFrameFilter
                                ),
                                error = errorMessage
                            )
                        }
                    }
                    setEffect(TaskListEffect.ShowSnackbar("Error deleting task: $errorMessage"))
                }
            )
        }
    }

    private fun handleUpdateTask(task: Task) {
        viewModelScope.launch {
            val updatedTasks = currentState.tasks.map {
                if (it.id == task.id) task else it
            }

            setState {
                copy(
                    tasks = updatedTasks,
                    filteredTasks = filterTasksUseCase(
                        updatedTasks,
                        statusFilter,
                        timeFrameFilter
                    )
                )
            }

            executeTaskOperation(
                setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                operation = { saveTaskUseCase(task) },
                onSuccess = { _ ->
                    setEffect(TaskListEffect.ShowSnackbar("Task updated successfully"))
                },
                onError = { errorMessage ->
                    val originalTask = currentState.tasks.find { it.id == task.id }
                    originalTask?.let {
                        val restoredTasks = currentState.tasks.map { t ->
                            if (t.id == task.id) originalTask else t
                        }
                        setState {
                            copy(
                                tasks = restoredTasks,
                                filteredTasks = filterTasksUseCase(
                                    restoredTasks,
                                    statusFilter,
                                    timeFrameFilter
                                ),
                                error = errorMessage
                            )
                        }
                    }
                    setEffect(TaskListEffect.ShowSnackbar("Error updating task: $errorMessage"))
                }
            )
        }
    }
}