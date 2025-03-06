package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.presentation.effects.TaskListEffect
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.states.TaskListState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.utils.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


class TaskListViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val filterTasksUseCase: FilterTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
) : BaseViewModel<TaskListIntent, TaskListState, TaskListEffect>() {

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
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            getTasksUseCase()
                .catch { error ->
                    setState { copy(isLoading = false, error = error.message) }
                    setEffect(TaskListEffect.ShowSnackbar("Error loading tasks: ${error.message}"))
                }
                .collect { tasks ->
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
            toggleTaskCompletionUseCase(taskId, completed).fold(
                onSuccess = { updatedTask ->
                    // Update local state immediately
                    val updatedTasksList = currentState.tasks.map {
                        if (it.id == taskId) updatedTask else it
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

                    setEffect(
                        TaskListEffect.ShowSnackbar(
                            if (completed) "Task marked as completed"
                            else "Task marked as not completed"
                        )
                    )
                },
                onFailure = { error ->
                    setState { copy(error = error.message) }
                    setEffect(TaskListEffect.ShowSnackbar("Error updating task: ${error.message}"))
                }
            )
        }
    }

    private fun handleDeleteTask(taskId: Int) {
        setEffect(TaskListEffect.ShowSnackbar("Delete task functionality not implemented yet"))
        loadTasks()
    }

    private fun handleUpdateTask(task: Task) {

        setEffect(TaskListEffect.ShowSnackbar("Update task functionality not implemented yet"))
        loadTasks()
    }
}