package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
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

    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state

    override fun onTriggerEvent(intent: TaskIntent) {
        when (intent) {
            is TaskIntent.LoadTasks -> loadTasks()
            is TaskIntent.CreateTask -> createTask(intent.newTaskData)
            is TaskIntent.UpdateTask -> updateTask(intent.task)
            is TaskIntent.DeleteTask -> deleteTask(intent.task)
            is TaskIntent.ToggleTaskCompletion -> toggleCompletion(intent.task, intent.checked)
            is TaskIntent.UpdateFilter -> updateFilter(intent.filter)
        }
    }

    private fun updateFilter(filter: TaskFilter) {
        _state.update { it.copy(currentFilter = filter) }
        applyFilters( _state.value.allTasks)
    }

    private fun applyFilters(tasks: List<Task>): List<Task> {
        val filtered = when (_state.value.currentFilter) {
            TaskFilter.TODAY -> tasks.filter { it.isDueToday() }
            TaskFilter.WEEK -> tasks.filter { it.isDueThisWeek() }
            TaskFilter.MONTH -> tasks.filter { it.isDueThisMonth() }
            TaskFilter.ALL -> tasks
        }
        _state.update { it.copy(filteredTasks = filtered) }
        return filtered
    }


    private fun toggleCompletion(task: Task, checked: Boolean) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = checked)
            updateTaskUseCase(updatedTask)
            loadTasks()
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                getTasksUseCase().collect { tasks ->
                    _state.update {
                        it.copy(
                            allTasks = tasks,
                            filteredTasks = applyFilters(tasks),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }



    private fun createTask(newTaskData: NewTaskData) {
        viewModelScope.launch {
            try {
                val task = newTaskData.toTask()
                addTaskUseCase(task)
                loadTasks()
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message, isLoading = false)
                }
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task)
                loadTasks()
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message)
                }
            }
        }
    }

    private fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(task)
                loadTasks()
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message)
                }
            }
        }
    }


}
