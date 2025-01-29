package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.utils.BaseViewModel

import com.elena.autoplanner.domain.usecases.GetTasksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.AddTaskUseCase
import com.elena.autoplanner.domain.usecases.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.UpdateTaskUseCase
import com.elena.autoplanner.presentation.intents.TaskFilter
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.utils.NewTaskData
import com.elena.autoplanner.presentation.utils.isDueThisMonth
import com.elena.autoplanner.presentation.utils.isDueThisWeek
import com.elena.autoplanner.presentation.utils.isDueToday
import com.elena.autoplanner.presentation.utils.toTask
import kotlinx.coroutines.flow.update



class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addTaskUseCase: AddTaskUseCase
) : BaseViewModel<TaskIntent>() {

    private val _state = MutableStateFlow(TaskState(
        selectedTimeFrame = TimeFrame.TODAY
    ))
    val state: StateFlow<TaskState> = _state

    // Derived state properties
    private val currentTasks: List<Task>
        get() = _state.value.allTasks

    override fun onTriggerEvent(intent: TaskIntent) {
        when (intent) {
            is TaskIntent.LoadTasks -> loadTasks()
            is TaskIntent.CreateTask -> createTask(intent.newTaskData)
            is TaskIntent.UpdateTask -> updateTask(intent.task)
            is TaskIntent.DeleteTask -> deleteTask(intent.task)
            is TaskIntent.ToggleTaskCompletion -> toggleCompletion(intent.task, intent.checked)
            is TaskIntent.UpdateStatusFilter -> updateStatusFilter(intent.status)
            is TaskIntent.UpdateTimeFrameFilter -> updateTimeFrameFilter(intent.timeFrame)
            is TaskIntent.AddSubtask -> addSubtask(intent.task, intent.subtaskName)
            is TaskIntent.ToggleSubtask -> toggleSubtask(intent.task, intent.subtask, intent.checked)
            is TaskIntent.ClearError -> clearError()
        }
    }

    private fun updateStatusFilter(status: TaskStatus) {
        _state.update { it.copy(selectedStatus = status) }
        applyFilters(currentTasks)
    }

    private fun addSubtask(task: Task, subtaskName: String) {
        val newSubtask = Subtask(
            id = task.subtasks.size + 1,
            name = subtaskName
        )
        val updatedTask = task.copy(subtasks = task.subtasks + newSubtask)
        updateTask(updatedTask)
    }

    private fun toggleSubtask(task: Task, subtask: Subtask, checked: Boolean) {
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtask.id) it.copy(isCompleted = checked) else it
        }
        val updatedTask = task.copy(subtasks = updatedSubtasks)
        updateTask(updatedTask)
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        _state.update { it.copy(selectedTimeFrame = timeFrame) }
        applyFilters(currentTasks)
    }

    private fun applyFilters(tasks: List<Task>) {
        val filtered = tasks
            .filter { task ->
                when  (state.value.selectedStatus) {
                    TaskStatus.COMPLETED -> task.isCompleted
                    TaskStatus.UNCOMPLETED -> !task.isCompleted
                    TaskStatus.ALL -> true
                }
            }
            .filter { task ->
                when (state.value.selectedTimeFrame) {
                    TimeFrame.TODAY -> task.isDueToday()
                    TimeFrame.WEEK -> task.isDueThisWeek()
                    TimeFrame.MONTH -> task.isDueThisMonth()
                    TimeFrame.ALL -> true
                }
            }

        _state.update { it.copy(filteredTasks = filtered) }
    }

    private fun toggleCompletion(task: Task, checked: Boolean) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(isCompleted = checked)
                updateTaskUseCase(updatedTask)
                updateLocalTask(updatedTask)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                getTasksUseCase().collect { tasks ->
                    _state.update {
                        it.copy(
                            allTasks = tasks,
                            isLoading = false,
                            error = null
                        )
                    }
                    applyFilters(tasks)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                val task = newTaskData.toTask()
                addTaskUseCase(task)
                _state.update {
                    it.copy(
                        allTasks = currentTasks + task,
                        isLoading = false
                    )
                }
                applyFilters(currentTasks)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                updateTaskUseCase(task)
                updateLocalTask(task)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                deleteTaskUseCase(task)
                _state.update {
                    it.copy(
                        allTasks = currentTasks - task,
                        isLoading = false
                    )
                }
                applyFilters(currentTasks)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun updateLocalTask(updatedTask: Task) {
        _state.update { state ->
            state.copy(
                allTasks = state.allTasks.map { task ->
                    if (task.id == updatedTask.id) updatedTask else task
                }
            )
        }
        applyFilters(currentTasks)
    }

    private fun handleError(e: Exception) {
        _state.update {
            it.copy(
                error = e.message ?: "An unexpected error occurred",
                isLoading = false
            )
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
