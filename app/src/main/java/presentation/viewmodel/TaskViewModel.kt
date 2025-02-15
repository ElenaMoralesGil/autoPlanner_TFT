package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.AddTaskUseCase
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
import com.elena.autoplanner.presentation.utils.isExpired
import com.elena.autoplanner.presentation.utils.toTask
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase
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
            is TaskIntent.ToggleSubtask -> toggleSubtask(intent.task, intent.subtask, intent.checked)
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

                setState { copy(uiState = TaskState.UiState.Loading) }
                val task = newTaskData.toTask()
                addTaskUseCase(task)
                setState {
                    copy(
                        tasks = currentState.tasks + task,
                        filteredTasks = applyFilters(
                            currentState.tasks + task,
                            currentState.filters
                        ), // Add filters
                        uiState = TaskState.UiState.Success("Task created")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }


    private fun updateTask(task: Task) {
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
                        ), // Add filters
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
                val task = currentState.tasks.find { it.id == taskId } ?: throw IllegalArgumentException("Task not found")
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
                updateTaskUseCase(updatedTask)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task.id) updatedTask else it },
                        filteredTasks = applyFilters(currentState.tasks, currentState.filters),
                        uiState = TaskState.UiState.Idle
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    private fun addSubtask(task: Int, subtaskName: String) {
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

    private fun toggleSubtask(taskId: Int, subtaskId: Int, checked: Boolean) {
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

    private fun deleteSubtask(task: Int, subtask: Int) {
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

    private fun applyFilters(tasks: List<Task>, filters: TaskState.Filters): List<Task> = tasks
        .filter { task ->
            when (filters.status) {
                TaskStatus.COMPLETED -> task.isCompleted
                TaskStatus.UNCOMPLETED -> !task.isCompleted
                TaskStatus.ALL -> true
            }
        }
        .filter { task ->
            when (filters.timeFrame) {
                TimeFrame.TODAY -> task.isDueToday()
                TimeFrame.WEEK -> task.isDueThisWeek()
                TimeFrame.MONTH -> task.isDueThisMonth()
                TimeFrame.EXPIRED -> task.isExpired()
                TimeFrame.ALL -> true
            }
        }

    private fun clearError() {
        setState { copy(uiState = TaskState.UiState.Idle) }
    }
}