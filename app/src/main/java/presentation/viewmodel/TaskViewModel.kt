package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.*
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
import kotlinx.coroutines.flow.*
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
                            filteredTasks = applyFilters(tasks),
                            uiState = TaskState.UiState.Idle
                        )
                    }
                }
        }
    }

    private fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            try {
                setState { copy(uiState = TaskState.UiState.Loading) }
                val task = newTaskData.toTask()
                addTaskUseCase(task)
                setState {
                    copy(
                        tasks = currentState.tasks + task,
                        filteredTasks = applyFilters(currentState.tasks + task),
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
                        filteredTasks = applyFilters(currentState.tasks),
                        uiState = TaskState.UiState.Success("Task updated")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    private fun deleteTask(task: Int) {
        viewModelScope.launch {
            try {
                val task = currentState.tasks.find { it.id == task } ?: throw IllegalArgumentException("Task not found")
                setState { copy(uiState = TaskState.UiState.Loading) }
                deleteTaskUseCase(task)
                setState {
                    copy(
                        tasks = currentState.tasks - task,
                        filteredTasks = applyFilters(currentState.tasks - task),
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
                        filteredTasks = applyFilters(currentState.tasks),
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
                setState { copy(uiState = TaskState.UiState.Loading) }
                val updatedTask = addSubtaskUseCase(task, subtaskName)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task) updatedTask else it },
                        filteredTasks = applyFilters(currentState.tasks),
                        uiState = TaskState.UiState.Success("Subtask added")
                    )
                }
            } catch (e: Exception) {
                setState { copy(uiState = TaskState.UiState.Error(e.message)) }
            }
        }
    }

    fun toggleSubtask(task: Int, subtask: Int, checked: Boolean) {
        viewModelScope.launch {
            try {
                val updatedTask = toggleSubtaskUseCase(task, subtask, checked)
                setState {
                    copy(
                        tasks = currentState.tasks.map { if (it.id == task) updatedTask else it },
                        filteredTasks = applyFilters(currentState.tasks),
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
                        filteredTasks = applyFilters(currentState.tasks),
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
            copy(
                filters = filters.copy(status = status),
                filteredTasks = applyFilters(tasks)
            )
        }
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        setState {
            copy(
                filters = filters.copy(timeFrame = timeFrame),
                filteredTasks = applyFilters(tasks)
            )
        }
    }

    private fun applyFilters(tasks: List<Task>): List<Task> = tasks
        .filter { task ->
            when (currentState.filters.status) {
                TaskStatus.COMPLETED -> task.isCompleted
                TaskStatus.UNCOMPLETED -> !task.isCompleted
                TaskStatus.ALL -> true
            }
        }
        .filter { task ->
            when (currentState.filters.timeFrame) {
                TimeFrame.TODAY -> task.isDueToday()
                TimeFrame.WEEK -> task.isDueThisWeek()
                TimeFrame.MONTH -> task.isDueThisMonth()
                TimeFrame.ALL -> true
            }
        }

    private fun clearError() {
        setState { copy(uiState = TaskState.UiState.Idle) }
    }
}