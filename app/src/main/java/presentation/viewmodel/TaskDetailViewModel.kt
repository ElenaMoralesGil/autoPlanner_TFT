package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.RepeatableTaskInstance
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
import com.elena.autoplanner.domain.usecases.tasks.RepeatTaskDeleteOption
import com.elena.autoplanner.domain.usecases.tasks.RepeatableTaskInstanceManager
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.intents.RepeatableDeleteType
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.states.TaskDetailState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskDetailViewModel(
    private val getTaskUseCase: GetTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val completeRepeatableTaskUseCase: CompleteRepeatableTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val deleteRepeatableTaskUseCase: DeleteRepeatableTaskUseCase,
    private val addSubtaskUseCase: AddSubtaskUseCase,
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase,
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase,
    private val repeatableTaskInstanceManager: RepeatableTaskInstanceManager,
    private val taskId: Int,
    private val instanceIdentifier: String? = null,
) : BaseTaskViewModel<TaskDetailIntent, TaskDetailState, TaskDetailEffect>() {

    // Lista de fechas de instancias generadas eliminadas (soft delete)
    private val deletedGeneratedInstanceDates = mutableSetOf<String>()

    // Instancias repetidas reales
    var repeatableInstances: List<RepeatableTaskInstance> = emptyList()
        private set

    override fun createInitialState(): TaskDetailState = TaskDetailState()

    override suspend fun handleIntent(intent: TaskDetailIntent) {
        when (intent) {
            is TaskDetailIntent.LoadTask -> loadTask(intent.taskId, intent.instanceIdentifier)
            is TaskDetailIntent.ToggleCompletion -> toggleTaskCompletion(intent.completed)
            is TaskDetailIntent.DeleteTask -> deleteTask()
            is TaskDetailIntent.AddSubtask -> addSubtask(intent.name)
            is TaskDetailIntent.ToggleSubtask -> toggleSubtask(intent.subtaskId, intent.completed)
            is TaskDetailIntent.DeleteSubtask -> deleteSubtask(intent.subtaskId)
            is TaskDetailIntent.EditTask -> navigateToEdit()
            is TaskDetailIntent.DeleteRepeatableTask -> deleteRepeatableTask(
                intent.instanceIdentifier,
                intent.deleteType
            )
            is TaskDetailIntent.EditRepeatableTask -> editRepeatableTask(
                intent.newTask,
                intent.newRepeatConfig,
                intent.fromDate
            )

            is TaskDetailIntent.UpdateRepeatableTaskInstances -> updateRepeatableTaskInstances(
                intent.newRepeatConfig,
                intent.fromDate
            )
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

    private suspend fun instanceExists(instanceIdentifier: String): Boolean {
        // Verifica en la base de datos
        val dbInstances = repeatableTaskInstanceManager.getInstancesByIdentifier(instanceIdentifier)
        if (dbInstances.isNotEmpty()) return true
        // Verifica en las instancias generadas en memoria
        if (repeatableInstances.any { it.instanceIdentifier == instanceIdentifier }) return true
        return false
    }

    private fun deleteTask() {
        viewModelScope.launch {
            val currentTask = currentState.task ?: return@launch
            setState { copy(isLoading = true) }

            if (currentTask.isRepeatedInstance || instanceIdentifier != null) {
                val identifier = instanceIdentifier ?: currentTask.instanceIdentifier!!
                val exists = withContext(Dispatchers.IO) { instanceExists(identifier) }
                if (!exists) {
                    setState { copy(error = "Instance not found") }
                    setEffect(TaskDetailEffect.ShowSnackbar("Error deleting repeated instance: instance not found"))
                    return@launch
                }
                // Borrar solo la instancia repetida
                executeTaskOperation<Unit>(
                    setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                    operation = {
                        deleteRepeatableTaskUseCase.deleteInstance(identifier)
                    },
                    onSuccess = {
                        setEffect(TaskDetailEffect.NavigateBack)
                        setEffect(TaskDetailEffect.ShowSnackbar("Repeated task instance deleted"))
                    },
                    onError = { errorMessage ->
                        setState { copy(error = errorMessage) }
                        setEffect(TaskDetailEffect.ShowSnackbar("Error deleting repeated instance: $errorMessage"))
                    }
                )
            } else {
                // Borrar tarea normal
                executeTaskOperation<Unit>(
                    setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                    operation = {
                        deleteTaskUseCase(currentTask.id)
                    },
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

    private fun deleteRepeatableTask(instanceIdentifier: String, deleteType: RepeatableDeleteType) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            when (deleteType) {
                RepeatableDeleteType.INSTANCE -> {
                    executeTaskOperation<Unit>(
                        setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                        operation = {
                            deleteRepeatableTaskUseCase.deleteInstance(instanceIdentifier)
                        },
                        onSuccess = {
                            setEffect(TaskDetailEffect.NavigateBack)
                            setEffect(TaskDetailEffect.ShowSnackbar("Instance deleted"))
                        },
                        onError = { errorMessage ->
                            setState { copy(error = errorMessage) }
                            setEffect(TaskDetailEffect.ShowSnackbar("Error deleting instance: $errorMessage"))
                        }
                    )
                }

                RepeatableDeleteType.FUTURE -> {
                    executeTaskOperation<Unit>(
                        setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                        operation = {
                            deleteRepeatableTaskUseCase.deleteFutureInstances(instanceIdentifier)
                        },
                        onSuccess = {
                            setEffect(TaskDetailEffect.NavigateBack)
                            setEffect(TaskDetailEffect.ShowSnackbar("Instance and future deleted"))
                        },
                        onError = { errorMessage ->
                            setState { copy(error = errorMessage) }
                            setEffect(TaskDetailEffect.ShowSnackbar("Error deleting future instances: $errorMessage"))
                        }
                    )
                }

                RepeatableDeleteType.ALL -> {
                    executeTaskOperation<Unit>(
                        setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                        operation = {
                            deleteRepeatableTaskUseCase.deleteAllInstances(instanceIdentifier)
                        },
                        onSuccess = {
                            setEffect(TaskDetailEffect.NavigateBack)
                            setEffect(TaskDetailEffect.ShowSnackbar("All instances deleted"))
                        },
                        onError = { errorMessage ->
                            setState { copy(error = errorMessage) }
                            setEffect(TaskDetailEffect.ShowSnackbar("Error deleting all instances: $errorMessage"))
                        }
                    )
                }
            }
        }
    }

    fun deleteGeneratedInstanceByDate(date: String) {
        deletedGeneratedInstanceDates.add(date)
        // Fuerza la actualización del estado para re-renderizar la UI
        setState { copy() }
        setEffect(TaskDetailEffect.ShowSnackbar("Instancia generada eliminada"))
    }

    fun isInstanceDateDeleted(date: String): Boolean = deletedGeneratedInstanceDates.contains(date)

    fun loadRepeatableInstances(instanceIdentifier: String? = null) {
        val id = instanceIdentifier ?: this.instanceIdentifier
        if (id != null) {
            viewModelScope.launch {
                val allInstances = repeatableTaskInstanceManager.getInstancesForIdentifier(id)
                repeatableInstances = allInstances.filter { !it.isDeleted }
                setState { copy(repeatableInstances = repeatableInstances) }
            }
        }
    }

    fun deleteRepeatableInstance(instanceIdentifier: String) {
        viewModelScope.launch {
            deleteRepeatableTaskUseCase.deleteInstance(instanceIdentifier)
            loadRepeatableInstances(instanceIdentifier)
            setEffect(TaskDetailEffect.ShowSnackbar("Instancia eliminada"))
        }
    }

    /**
     * Llama a esta función después de modificar la configuración de repetición de una tarea repetida.
     * Actualiza las instancias futuras según la nueva configuración.
     */
    fun updateRepeatableTaskInstances(
        newRepeatConfig: Any,
        fromDate: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    ) {
        val currentTask = currentState.task ?: return
        if (currentTask.repeatPlan != null) {
            viewModelScope.launch {
                repeatableTaskInstanceManager.updateFutureInstances(
                    currentTask.id,
                    fromDate
                )
                if (currentTask.instanceIdentifier != null) {
                    loadRepeatableInstances(currentTask.instanceIdentifier)
                }
                setEffect(TaskDetailEffect.ShowSnackbar("Instancias futuras eliminadas"))
            }
        }
    }

    /**
     * Edita una tarea repetida y actualiza sus instancias futuras si cambia la configuración de repetición.
     */
    fun editRepeatableTask(
        newTask: Task,
        newRepeatConfig: Any?,
        fromDate: java.time.LocalDateTime? = null,
    ) {
        viewModelScope.launch {
            if (newRepeatConfig != null && fromDate != null) {
                updateRepeatableTaskInstances(newRepeatConfig, fromDate)
            }
            loadTask(newTask.id)
            setEffect(TaskDetailEffect.ShowSnackbar("Tarea repetida editada y futuras instancias actualizadas"))
        }
    }
}