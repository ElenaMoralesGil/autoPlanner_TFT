package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.states.TaskDetailState
import com.elena.autoplanner.presentation.utils.BaseViewModel
import kotlinx.coroutines.launch

/**
 * ViewModel for the task detail screen
 * Handles detailed task viewing and modifications
 */
class TaskDetailViewModel(
    private val getTaskUseCase: GetTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val taskId: Int // Inject the taskId parameter
) : BaseViewModel<TaskDetailIntent, TaskDetailState, TaskDetailEffect>() {

    override fun createInitialState(): TaskDetailState = TaskDetailState()

    override suspend fun handleIntent(intent: TaskDetailIntent) {
        when (intent) {
            is TaskDetailIntent.LoadTask -> loadTask(intent.taskId)
            is TaskDetailIntent.ToggleCompletion -> toggleTaskCompletion(intent.completed)
            is TaskDetailIntent.DeleteTask -> deleteTask()
            is TaskDetailIntent.AddSubtask -> addSubtask(intent.name)
            is TaskDetailIntent.ToggleSubtask -> toggleSubtask(intent.subtaskId, intent.completed)
            is TaskDetailIntent.DeleteSubtask -> deleteSubtask(intent.subtaskId)
            is TaskDetailIntent.EditTask -> navigateToEdit()
        }
    }

    // Automatically load task on initialization
    init {
        viewModelScope.launch {
            loadTask(taskId)
        }
    }

    private fun loadTask(taskId: Int) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            getTaskUseCase(taskId).fold(
                onSuccess = { task ->
                    setState { copy(isLoading = false, task = task, error = null) }
                },
                onFailure = { error ->
                    setState { copy(isLoading = false, error = error.message) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error loading task: ${error.message}"))
                }
            )
        }
    }

    private fun toggleTaskCompletion(completed: Boolean) {
        viewModelScope.launch {

            val currentTask = currentState.task ?: return@launch

            setState {
                copy(task = currentTask.copy(isCompleted = completed))
            }

            toggleTaskCompletionUseCase(currentTask.id, completed).fold(
                onSuccess = {
                    setState { copy(task = it) }
                },
                onFailure = { error ->
                    setState {
                        copy(
                            task = currentTask,
                            error = error.message
                        )
                    }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error updating task: ${error.message}"))
                }
            )
        }
    }

    private fun deleteTask() {
        viewModelScope.launch {
            val taskId = currentState.task?.id ?: return@launch
            setState { copy(isLoading = true) }

            deleteTaskUseCase(taskId).fold(
                onSuccess = {
                    setEffect(TaskDetailEffect.NavigateBack)
                    setEffect(TaskDetailEffect.ShowSnackbar("Task deleted"))
                },
                onFailure = { error ->
                    setState { copy(isLoading = false, error = error.message) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error deleting task: ${error.message}"))
                }
            )
        }
    }

    private fun addSubtask(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                setEffect(TaskDetailEffect.ShowSnackbar("Subtask name cannot be empty"))
                return@launch
            }

            val taskId = currentState.task?.id ?: return@launch

            addSubtaskUseCase(taskId, name).fold(
                onSuccess = { updatedTask ->
                    setState { copy(task = updatedTask) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Subtask added"))
                },
                onFailure = { error ->
                    setState { copy(error = error.message) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error adding subtask: ${error.message}"))
                }
            )
        }
    }

    private fun toggleSubtask(subtaskId: Int, completed: Boolean) {
        viewModelScope.launch {
            val taskId = currentState.task?.id ?: return@launch

            toggleSubtaskUseCase(taskId, subtaskId, completed).fold(
                onSuccess = { updatedTask ->
                    setState { copy(task = updatedTask) }
                },
                onFailure = { error ->
                    setState { copy(error = error.message) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error updating subtask: ${error.message}"))
                }
            )
        }
    }

    private fun deleteSubtask(subtaskId: Int) {
        viewModelScope.launch {
            val taskId = currentState.task?.id ?: return@launch

            deleteSubtaskUseCase(taskId, subtaskId).fold(
                onSuccess = { updatedTask ->
                    setState { copy(task = updatedTask) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Subtask deleted"))
                },
                onFailure = { error ->
                    setState { copy(error = error.message) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error deleting subtask: ${error.message}"))
                }
            )
        }
    }


    private fun navigateToEdit() {
        currentState.task?.let {
            setEffect(TaskDetailEffect.NavigateToEdit(it.id))
        }
    }
}