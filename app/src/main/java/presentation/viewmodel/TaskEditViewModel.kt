package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.presentation.effects.TaskEditEffect
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.states.TaskEditState
import com.elena.autoplanner.presentation.utils.BaseViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskEditViewModel(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase
) : BaseViewModel<TaskEditIntent, TaskEditState, TaskEditEffect>() {

    override fun createInitialState(): TaskEditState = TaskEditState()

    override suspend fun handleIntent(intent: TaskEditIntent) {
        when (intent) {
            is TaskEditIntent.LoadTask -> loadTask(intent.taskId)
            is TaskEditIntent.UpdateName -> setState { copy(name = intent.name) }
            is TaskEditIntent.UpdatePriority -> setState { copy(priority = intent.priority) }
            is TaskEditIntent.UpdateStartDateConf -> setState { copy(startDateConf = intent.timePlanning) }
            is TaskEditIntent.UpdateEndDateConf -> setState { copy(endDateConf = intent.timePlanning) }
            is TaskEditIntent.UpdateDuration -> setState { copy(durationConf = intent.duration) }
            is TaskEditIntent.UpdateReminder -> setState { copy(reminderPlan = intent.reminder) }
            is TaskEditIntent.UpdateRepeat -> setState { copy(repeatPlan = intent.repeat) }
            is TaskEditIntent.AddSubtask -> addSubtask(intent.name)
            is TaskEditIntent.UpdateSubtask -> updateSubtask(intent.subtask)
            is TaskEditIntent.RemoveSubtask -> removeSubtask(intent.subtaskId)
            is TaskEditIntent.SaveTask -> saveTask()
            is TaskEditIntent.Cancel -> setEffect(TaskEditEffect.NavigateBack)
        }
    }

    private fun loadTask(taskId: Int) {
        if (taskId == 0) {
            // New task
            setState {
                copy(
                    isNewTask = true,
                    startDateConf = TimePlanning(dateTime = LocalDateTime.now())
                )
            }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }

            getTaskUseCase(taskId).fold(
                onSuccess = { task ->
                    setState {
                        copy(
                            isLoading = false,
                            isNewTask = false,
                            taskId = task.id,
                            name = task.name,
                            priority = task.priority,
                            startDateConf = task.startDateConf,
                            endDateConf = task.endDateConf,
                            durationConf = task.durationConf,
                            reminderPlan = task.reminderPlan,
                            repeatPlan = task.repeatPlan,
                            subtasks = task.subtasks,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    setState { copy(isLoading = false, error = error.message) }
                    setEffect(TaskEditEffect.ShowSnackbar("Error loading task: ${error.message}"))
                }
            )
        }
    }

    private fun addSubtask(name: String) {
        if (name.isBlank()) {
            setEffect(TaskEditEffect.ShowSnackbar("Subtask name cannot be empty"))
            return
        }

        val nextId = if (currentState.subtasks.isEmpty()) 1
        else currentState.subtasks.maxOf { it.id } + 1

        val newSubtask = Subtask(id = nextId, name = name)
        setState { copy(subtasks = subtasks + newSubtask) }
    }

    private fun updateSubtask(subtask: Subtask) {
        setState {
            copy(
                subtasks = subtasks.map {
                    if (it.id == subtask.id) subtask else it
                }
            )
        }
    }

    private fun removeSubtask(subtaskId: Int) {
        setState { copy(subtasks = subtasks.filter { it.id != subtaskId }) }
    }

    private fun saveTask() {
        viewModelScope.launch {
            val state = currentState

            if (state.name.isBlank()) {
                setEffect(TaskEditEffect.ShowSnackbar("Task name cannot be empty"))
                return@launch
            }

            setState { copy(isLoading = true) }

            val task = Task(
                id = state.taskId,
                name = state.name,
                priority = state.priority,
                startDateConf = state.startDateConf,
                endDateConf = state.endDateConf,
                durationConf = state.durationConf,
                reminderPlan = state.reminderPlan,
                repeatPlan = state.repeatPlan,
                subtasks = state.subtasks
            )

            saveTaskUseCase(task).fold(
                onSuccess = { _ ->
                    setEffect(TaskEditEffect.NavigateBack)
                    setEffect(
                        TaskEditEffect.ShowSnackbar(
                            if (state.isNewTask) "Task created" else "Task updated"
                        )
                    )
                },
                onFailure = { error ->
                    setState { copy(isLoading = false, error = error.message) }
                    setEffect(TaskEditEffect.ShowSnackbar("Error saving task: ${error.message}"))
                }
            )
        }
    }
}