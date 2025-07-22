package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteRepeatableTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.domain.usecases.tasks.CompleteRepeatableTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.RepeatableTaskGenerator
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.states.TaskDetailState
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    private val getTaskUseCase: GetTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val completeRepeatableTaskUseCase: CompleteRepeatableTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val deleteRepeatableTaskUseCase: DeleteRepeatableTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val repeatableTaskGenerator: RepeatableTaskGenerator,
    private val taskId: Int,
    private val instanceIdentifier: String? = null,
) : BaseTaskViewModel<TaskDetailIntent, TaskDetailState, TaskDetailEffect>() {

    override fun createInitialState(): TaskDetailState = TaskDetailState()

    override suspend fun handleIntent(intent: TaskDetailIntent) {
        when (intent) {
            is TaskDetailIntent.LoadTask -> loadTask(intent.taskId, instanceIdentifier)
            is TaskDetailIntent.ToggleCompletion -> toggleTaskCompletion(intent.completed)
            is TaskDetailIntent.DeleteTask -> deleteTask()
            is TaskDetailIntent.AddSubtask -> addSubtask(intent.name)
            is TaskDetailIntent.ToggleSubtask -> toggleSubtask(intent.subtaskId, intent.completed)
            is TaskDetailIntent.DeleteSubtask -> deleteSubtask(intent.subtaskId)
            is TaskDetailIntent.EditTask -> navigateToEdit()
        }
    }

    init {
        viewModelScope.launch {
            loadTask(taskId, instanceIdentifier)
        }
    }

    private fun loadTask(taskId: Int, instanceIdentifier: String? = null) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            executeTaskOperation(
                setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                operation = {
                    if (instanceIdentifier != null) {
                        getTaskUseCase.getTaskByInstanceIdentifier(instanceIdentifier)
                    } else {
                        getTaskUseCase(taskId)
                    }
                },
                onSuccess = { task ->
                    setState { copy(task = task, error = null) }
                },
                onError = { errorMessage ->
                    setState { copy(error = errorMessage) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error loading task: $errorMessage"))
                }
            )
        }
    }

    private fun toggleTaskCompletion(completed: Boolean) {
        viewModelScope.launch {
            val currentTask = currentState.task ?: return@launch

            // Si la tarea tiene repetición o es una instancia repetida y se está marcando como completada, usar CompleteRepeatableTaskUseCase
            if (completed && (currentTask.repeatPlan != null || currentTask.isRepeatedInstance)) {
                when (val result = completeRepeatableTaskUseCase.execute(currentTask)) {
                    is TaskResult.Success<*> -> {
                        setEffect(TaskDetailEffect.ShowSnackbar("Repeatable task completed and next occurrence created"))
                        setEffect(TaskDetailEffect.NavigateBack) // Navegar de vuelta ya que la tarea fue completada
                    }

                    is TaskResult.Error -> {
                        setState { copy(error = result.message) }
                        setEffect(TaskDetailEffect.ShowSnackbar("Error completing repeatable task: ${result.message}"))
                    }
                }
                return@launch
            }

            // Para tareas normales o desmarcar como completada, usar el método normal
            val updatedTask = Task.from(currentTask)
                .isCompleted(completed)
                .build()

            setState { copy(task = updatedTask) }

            executeTaskOperation(
                setLoadingState = { },
                operation = { toggleTaskCompletionUseCase(currentTask.id, completed) },
                onSuccess = {
                    val message = if (completed) "Task completed" else "Task marked as incomplete"
                    setEffect(TaskDetailEffect.ShowSnackbar(message))
                },
                onError = { errorMessage ->
                    setState { copy(task = currentTask, error = errorMessage) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error updating task: $errorMessage"))
                }
            )
        }
    }

    private fun deleteTask() {
        viewModelScope.launch {
            val taskId = currentState.task?.id ?: return@launch
            setState { copy(isLoading = true) }

            executeTaskOperation(
                setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                operation = { deleteTaskUseCase(taskId) },
                onSuccess = {
                    setEffect(TaskDetailEffect.NavigateBack)
                    setEffect(TaskDetailEffect.ShowSnackbar("Task deleted"))
                },
                onError = { errorMessage ->
                    setState { copy(error = errorMessage) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error deleting task: $errorMessage"))
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

            executeTaskOperation(
                setLoadingState = { },
                operation = { addSubtaskUseCase(taskId, name) },
                onSuccess = { updatedTask ->
                    setState { copy(task = updatedTask) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Subtask added"))
                },
                onError = { errorMessage ->
                    setState { copy(error = errorMessage) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error adding subtask: $errorMessage"))
                }
            )
        }
    }

    private fun toggleSubtask(subtaskId: Int, completed: Boolean) {
        viewModelScope.launch {
            val taskId = currentState.task?.id ?: return@launch

            executeTaskOperation(
                setLoadingState = { },
                operation = { toggleSubtaskUseCase(taskId, subtaskId, completed) },
                onSuccess = { updatedTask ->
                    setState { copy(task = updatedTask) }
                },
                onError = { errorMessage ->
                    setState { copy(error = errorMessage) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error updating subtask: $errorMessage"))
                }
            )
        }
    }

    private fun deleteSubtask(subtaskId: Int) {
        viewModelScope.launch {
            val taskId = currentState.task?.id ?: return@launch

            executeTaskOperation(
                setLoadingState = { },
                operation = { deleteSubtaskUseCase(taskId, subtaskId) },
                onSuccess = { updatedTask ->
                    setState { copy(task = updatedTask) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Subtask deleted"))
                },
                onError = { errorMessage ->
                    setState { copy(error = errorMessage) }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error deleting subtask: $errorMessage"))
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