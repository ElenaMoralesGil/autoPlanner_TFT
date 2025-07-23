package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.GetAllSectionsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetAllListsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetTasksByListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveSectionUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetExpandedTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.CompleteRepeatableTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteRepeatableTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.RepeatTaskDeleteOption
import com.elena.autoplanner.domain.usecases.tasks.RepeatableTaskGenerator
import com.elena.autoplanner.presentation.effects.TaskListEffect
import com.elena.autoplanner.presentation.effects.TaskListEffect.*
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.states.TaskListState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.domain.repositories.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val getTasksByListUseCase: GetTasksByListUseCase,
    private val filterTasksUseCase: FilterTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val saveListUseCase: SaveListUseCase,
    private val saveSectionUseCase: SaveSectionUseCase,
    private val getAllSectionsUseCase: GetAllSectionsUseCase,
    private val getAllListsUseCase: GetAllListsUseCase,
    private val getExpandedTasksUseCase: GetExpandedTasksUseCase,
    private val completeRepeatableTaskUseCase: CompleteRepeatableTaskUseCase,
    private val deleteRepeatableTaskUseCase: DeleteRepeatableTaskUseCase,
    private val repeatableTaskGenerator: RepeatableTaskGenerator,
    private val savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
) : BaseViewModel<TaskListIntent, TaskListState, TaskListEffect>() {

    private var taskLoadingJob: Job? = null
    private val initialListId: Long? = savedStateHandle.get<String>("listId")?.toLongOrNull()
    private val initialSectionId: Long? = savedStateHandle.get<String>("sectionId")?.toLongOrNull()
    override fun createInitialState(): TaskListState = TaskListState(isLoading = false)

    init {
        Log.d("TaskListVM", "ViewModel Init Start - Instance: $this")
        viewModelScope.launch {
            combine(

                savedStateHandle.getStateFlow<String?>("listId", initialListId?.toString()),
                savedStateHandle.getStateFlow<String?>("sectionId", initialSectionId?.toString())
            ) { listIdStr, sectionIdStr ->
                Pair(listIdStr?.toLongOrNull(), sectionIdStr?.toLongOrNull())
            }
                .distinctUntilChanged()
                .onEach { (listId, sectionId) ->
                    Log.d("TaskListVM", "SavedStateHandle Changed (onEach): listId=$listId, sectionId=$sectionId. Current isNavigating: ${currentState.isNavigating}")

                    setState { copy(requestedListId = listId, requestedSectionId = sectionId) }
                }
                .collectLatest { (listId, sectionId) ->
                    Log.d("TaskListVM", "Collecting new nav args: listId=$listId, sectionId=$sectionId. Current state: currentListId=${currentState.currentListId}, currentSectionId=${currentState.currentSectionId}, isLoading=${currentState.isLoading}, tasksEmpty=${currentState.tasks.isEmpty()}")

                    val idsChanged = currentState.currentListId != listId || currentState.currentSectionId != sectionId

                    val initialOrEmptyAndNotLoading = currentState.tasks.isEmpty() && !currentState.isLoading

                    if (idsChanged || initialOrEmptyAndNotLoading) {
                        Log.d("TaskListVM", "Data load triggered for listId=$listId, sectionId=$sectionId. IDsChanged: $idsChanged, InitialOrEmptyAndNotLoading: $initialOrEmptyAndNotLoading")
                        loadTasks(listId, sectionId) 
                    } else {
                        Log.d("TaskListVM", "Data load skipped for listId=$listId, sectionId=$sectionId.")

                        if (currentState.isLoading || currentState.isNavigating) {
                            setState { copy(isLoading = false, isNavigating = false) }
                        }
                    }
                }
        }
        Log.d("TaskListVM", "ViewModel Init End - Instance: $this")
    }

    override suspend fun handleIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.UpdateStatusFilter -> updateStatusFilter(intent.status)
            is TaskListIntent.UpdateTimeFrameFilter -> updateTimeFrameFilter(intent.timeFrame)
            is TaskListIntent.ToggleTaskCompletion -> toggleTaskCompletion(
                intent.taskId,
                intent.completed
            )

            is TaskListIntent.SelectTask -> setEffect(
                NavigateToTaskDetail(
                    intent.taskId,
                    intent.instanceIdentifier
                )
            )
            is TaskListIntent.UpdateTask -> handleUpdateTask(intent.task)
            is TaskListIntent.DeleteTask -> handleDeleteTask(intent.taskId)
            is TaskListIntent.DeleteRepeatableTask -> handleDeleteRepeatableTask(intent.task)
            is TaskListIntent.ConfirmRepeatableTaskDeletion -> handleConfirmRepeatableTaskDeletion(
                intent.task,
                intent.option
            )

            is TaskListIntent.ViewList -> {
                Log.d("TaskListVM", "ViewList intent received for listId=${intent.listId}")
                setState { copy(isNavigating = true) } 
                savedStateHandle["listId"] = intent.listId?.toString()
                savedStateHandle["sectionId"] = null
            }

            is TaskListIntent.ViewAllTasks -> {
                Log.d("TaskListVM", "ViewAllTasks intent received")
                setState { copy(isNavigating = true )}
                savedStateHandle["listId"] = null
                savedStateHandle["sectionId"] = null
            }
            is TaskListIntent.ViewSection -> {
                Log.d("TaskListVM", "ViewSection intent received for listId=${intent.listId}, sectionId=${intent.sectionId}")
                setState { copy(isNavigating = true) } 
                savedStateHandle["listId"] = intent.listId.toString()
                savedStateHandle["sectionId"] = intent.sectionId.toString()
            }

            is TaskListIntent.RequestEditList -> currentState.currentListId?.let {
                setEffect(
                    ShowEditListDialog(it)
                )
            }

            is TaskListIntent.RequestEditSections -> currentState.currentListId?.let {
                setEffect(
                    ShowEditSectionsDialog(it)
                )
            }

            is TaskListIntent.SaveList -> saveList(intent.list)
            is TaskListIntent.SaveSection -> saveSection(intent.section)
            is TaskListIntent.LoadTasks -> { 
                Log.d(
                    "TaskListVM",
                    "LoadTasks Intent received. Reloading for listId: ${intent.listId} (current requested: ${currentState.requestedListId})"
                )
                if (currentState.requestedListId != intent.listId || currentState.currentListId != intent.listId) {
                    setState {
                        copy(
                            requestedListId = intent.listId,
                            requestedSectionId = null
                        )
                    } 
                }
                loadTasks(currentState.requestedListId, currentState.requestedSectionId)
            }
            is TaskListIntent.LoadTasksForDateRange -> { // Nuevo intent para calendario
                Log.d(
                    "TaskListVM",
                    "LoadTasksForDateRange Intent received for range: ${intent.startDate} to ${intent.endDate}"
                )
                loadTasksForDateRange(intent.startDate, intent.endDate)
            }

            is TaskListIntent.LoadMoreTasks -> {
                // Lógica para cargar más tareas (paginación)
                loadMoreTasks()
            }
        }
    }

    private fun loadTasks(listId: Long?, sectionId: Long?) {
        taskLoadingJob?.cancel()
        taskLoadingJob = viewModelScope.launch {
            Log.d("TaskListVM", "loadTasks ENTERED for listId: $listId, sectionId: $sectionId.")

            setState { copy(isLoading = true, isNavigating = false, error = null) }
            Log.d("TaskListVM", "loadTasks: Set isLoading=true, isNavigating=false. State: $currentState")

            getExpandedTasksUseCase()
                .catch { error ->
                    Log.e("TaskListVM", "Error in getExpandedTasksUseCase", error)
                    setState {
                        copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Unknown error loading tasks",
                            currentListId = listId,
                            currentSectionId = sectionId,
                            requestedListId = listId,
                            requestedSectionId = sectionId
                        )
                    }
                    setEffect(ShowSnackbar("Error loading tasks: ${error.localizedMessage}"))

                    // CORREGIR: Auto-limpiar el error después de 3 segundos para recuperación automática
                    launch {
                        kotlinx.coroutines.delay(3000)
                        if (currentState.error != null) {
                            Log.d("TaskListVM", "Auto-clearing error after timeout")
                            setState { copy(error = null) }
                        }
                    }
                }
                .collect { result ->
                    when (result) {
                        is TaskResult.Success -> {
                            val expandedTasks = result.data
                            Log.d(
                                "TaskListVM",
                                "Collected expanded tasks: Count=${expandedTasks.size} for requested listId=$listId, sectionId=$sectionId"
                            )

                            val tasksToShow = when {
                                listId != null -> {
                                    expandedTasks.filter { task -> task.listId == listId }
                                }

                                else -> expandedTasks
                            }

                            // Aplicar filtros inmediatamente después de cargar las tareas
                            val filteredTasks = filterTasksUseCase(
                                tasksToShow,
                                currentState.statusFilter,
                                currentState.timeFrameFilter
                            )

                            // Obtener información de la lista si se está viendo una lista específica
                            if (listId != null) {
                                when (val listsResult = getAllListsUseCase()) {
                                    is TaskResult.Success -> {
                                        val currentList = listsResult.data.find { it.id == listId }
                                        setState {
                                            copy(
                                                tasks = tasksToShow,
                                                filteredTasks = filteredTasks,
                                                isLoading = false,
                                                currentListId = listId,
                                                currentListName = currentList?.name,
                                                currentListColor = currentList?.colorHex,
                                                currentSectionId = sectionId,
                                                requestedListId = listId,
                                                requestedSectionId = sectionId
                                            )
                                        }
                                    }

                                    is TaskResult.Error -> {
                                        Log.e(
                                            "TaskListVM",
                                            "Error loading list info: ${listsResult.message}"
                                        )
                                        setState {
                                            copy(
                                                tasks = tasksToShow,
                                                filteredTasks = filteredTasks,
                                                isLoading = false,
                                                currentListId = listId,
                                                currentListName = null,
                                                currentListColor = null,
                                                currentSectionId = sectionId,
                                                requestedListId = listId,
                                                requestedSectionId = sectionId
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Si no hay lista específica (All Tasks), limpiar info de lista
                                setState {
                                    copy(
                                        tasks = tasksToShow,
                                        filteredTasks = filteredTasks,
                                        isLoading = false,
                                        currentListId = null,
                                        currentListName = null,
                                        currentListColor = null,
                                        currentSectionId = sectionId,
                                        requestedListId = listId,
                                        requestedSectionId = sectionId
                                    )
                                }
                            }
                        }

                        is TaskResult.Error -> {
                            Log.e("TaskListVM", "Error loading expanded tasks: ${result.message}")
                            setState {
                                copy(
                                    tasks = emptyList(),
                                    isLoading = false,
                                    error = result.message,
                                    currentListId = listId,
                                    currentSectionId = sectionId,
                                    requestedListId = listId,
                                    requestedSectionId = sectionId
                                )
                            }
                        }
                    }
                }
        }
    }

    // Nuevo método para cargar tareas con rango de fechas específico (para calendario)
    private fun loadTasksForDateRange(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ) {
        taskLoadingJob?.cancel()
        taskLoadingJob = viewModelScope.launch {
            Log.d("TaskListVM", "loadTasksForDateRange ENTERED for range: $startDate to $endDate")

            setState { copy(isLoading = true, error = null) }

            getExpandedTasksUseCase(startDate, endDate)
                .catch { error ->
                    Log.e("TaskListVM", "Error in getExpandedTasksUseCase for date range", error)
                    setState {
                        copy(
                            isLoading = false,
                            error = error.localizedMessage
                                ?: "Unknown error loading tasks for date range"
                        )
                    }
                    setEffect(ShowSnackbar("Error loading tasks: ${error.localizedMessage}"))
                }
                .collect { result ->
                    when (result) {
                        is TaskResult.Success -> {
                            val expandedTasks = result.data
                            Log.d(
                                "TaskListVM",
                                "Loaded ${expandedTasks.size} tasks for date range $startDate to $endDate"
                            )

                            // Aplicar filtros
                            val filteredTasks = filterTasksUseCase(
                                expandedTasks,
                                currentState.statusFilter,
                                currentState.timeFrameFilter
                            )

                            setState {
                                copy(
                                    tasks = expandedTasks,
                                    filteredTasks = filteredTasks,
                                    isLoading = false,
                                    // Mantener los IDs actuales para el calendario
                                    currentListId = null, // Calendario no filtra por lista
                                    currentSectionId = null
                                )
                            }
                        }

                        is TaskResult.Error -> {
                            Log.e(
                                "TaskListVM",
                                "Error loading tasks for date range: ${result.message}"
                            )
                            setState {
                                copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                            setEffect(ShowSnackbar("Error loading tasks: ${result.message}"))
                        }
                    }
                }
        }
    }

    // Agrega la función para cargar más tareas
    private fun loadMoreTasks() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            // Aquí deberías implementar la lógica real de paginación, por ejemplo:
            // val moreTasks = ...
            // setState { copy(tasks = tasks + moreTasks, isLoading = false) }
            setState { copy(isLoading = false) } // Placeholder
        }
    }

    private fun updateStatusFilter(status: TaskStatus) {
        val filteredTasks =
            filterTasksUseCase(currentState.tasks, status, currentState.timeFrameFilter)
        setState { copy(statusFilter = status, filteredTasks = filteredTasks) }
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        val filteredTasks =
            filterTasksUseCase(currentState.tasks, currentState.statusFilter, timeFrame)
        setState { copy(timeFrameFilter = timeFrame, filteredTasks = filteredTasks) }
    }

    private fun toggleTaskCompletion(taskId: Int, completed: Boolean) {
        viewModelScope.launch {
            Log.d("TaskListVM", "toggleTaskCompletion called: taskId=$taskId, completed=$completed")

            // Buscar la tarea en el estado actual para verificar si tiene repetición
            val task = currentState.tasks.find { it.id == taskId }
            if (task == null) {
                Log.e(
                    "TaskListVM",
                    "toggleTaskCompletion ERROR: Task $taskId not found in current state"
                )
                setEffect(ShowSnackbar("Error: Task not found"))
                setState { copy(error = "Task not found") }
                return@launch
            }

            // Si la tarea tiene repetición o es una instancia repetida y se está marcando como completada, usar CompleteRepeatableTaskUseCase
            if (completed && (task.repeatPlan != null || task.isRepeatedInstance)) {
                Log.d(
                    "TaskListVM",
                    "Task $taskId has repetition or is repeated instance, using CompleteRepeatableTaskUseCase"
                )
                when (val result = completeRepeatableTaskUseCase.execute(task)) {
                    is TaskResult.Success<*> -> {
                        // Para tareas repetibles, actualizamos el estado DESPUÉS del éxito
                        // Esto evita conflictos entre la tarea completada y la nueva instancia
                        updateTaskCompletionInState(taskId, completed)
                        setEffect(ShowSnackbar("Repeatable task completed and next occurrence created"))
                        Log.d("TaskListVM", "CompleteRepeatableTaskUseCase SUCCESS: taskId=$taskId")
                    }

                    is TaskResult.Error -> {
                        Log.e(
                            "TaskListVM",
                            "CompleteRepeatableTaskUseCase ERROR: taskId=$taskId, error=${result.message}"
                        )
                        setState { copy(error = result.message) }
                        setEffect(ShowSnackbar("Error completing repeatable task: ${result.message}"))
                    }
                }
                return@launch
            }

            // Para tareas normales o desmarcar como completada, usar el método normal
            updateTaskCompletionInState(taskId, completed)
            when (val result = toggleTaskCompletionUseCase(taskId, completed)) {
                is TaskResult.Success<*> -> {
                    val message = if (completed) "Task completed" else "Task marked incomplete"
                    setEffect(ShowSnackbar(message))
                    Log.d("TaskListVM", "toggleTaskCompletion SUCCESS: taskId=$taskId")
                }

                is TaskResult.Error -> {
                    Log.e(
                        "TaskListVM",
                        "toggleTaskCompletion ERROR: taskId=$taskId, error=${result.message}"
                    )
                    updateTaskCompletionInState(taskId, !completed)
                    setState { copy(error = result.message) }
                    setEffect(ShowSnackbar("Error: ${result.message}"))
                }
            }
        }
    }

    private fun updateTaskCompletionInState(taskId: Int, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.getTasks().collect { result ->
                if (result is TaskResult.Success) {
                    val refreshedTasks = result.data
                    val filtered = filterTasksUseCase(
                        refreshedTasks,
                        currentState.statusFilter,
                        currentState.timeFrameFilter
                    )
                    setState { copy(tasks = refreshedTasks, filteredTasks = filtered) }
                }
            }
        }
    }

    private fun handleDeleteTask(taskId: Int) {
        viewModelScope.launch {
            when (val result = deleteTaskUseCase(taskId)) {
                is TaskResult.Success -> setEffect(ShowSnackbar("Task deleted"))
                is TaskResult.Error -> {
                    setState { copy(error = result.message) }
                    setEffect(ShowSnackbar("Error deleting: ${result.message}"))
                }
            }
        }
    }

    private fun handleDeleteRepeatableTask(task: Task) {
        if (deleteRepeatableTaskUseCase.needsDeleteOptions(task)) {

            setEffect(ShowRepeatTaskDeleteDialog(task))
        } else {
            viewModelScope.launch {
                // Log para depuración del instanceIdentifier
                android.util.Log.d(
                    "TaskDeleteDebug",
                    "Deleting instance with identifier: ${task.instanceIdentifier}"
                )
                if (task.instanceIdentifier != null) {
                    when (val result =
                        deleteRepeatableTaskUseCase.deleteInstance(task.instanceIdentifier)) {
                        is TaskResult.Success -> setEffect(ShowSnackbar("Instance deleted"))
                        is TaskResult.Error -> {
                            setState { copy(error = result.message) }
                            setEffect(ShowSnackbar("Error deleting instance: ${result.message}"))
                        }
                    }
                } else {
                    when (val result = deleteTaskUseCase(task.id)) {
                        is TaskResult.Success -> setEffect(ShowSnackbar("Task deleted"))
                        is TaskResult.Error -> {
                            setState { copy(error = result.message) }
                            setEffect(ShowSnackbar("Error deleting: ${result.message}"))
                        }
                    }
                }
            }
        }
    }

    private fun handleConfirmRepeatableTaskDeletion(task: Task, option: RepeatTaskDeleteOption) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val instanceIdentifier = task.instanceIdentifier ?: return@launch
            when (option) {
                RepeatTaskDeleteOption.THIS_INSTANCE_ONLY -> {
                    when (val result =
                        deleteRepeatableTaskUseCase.deleteInstance(instanceIdentifier)) {
                        is TaskResult.Success<*> -> {
                            setEffect(TaskListEffect.ShowSnackbar("Instance deleted"))
                            loadTasks(currentState.currentListId, currentState.currentSectionId)
                        }

                        is TaskResult.Error -> {
                            setEffect(TaskListEffect.ShowSnackbar("Error deleting instance: ${result.message}"))
                        }
                    }
                }

                RepeatTaskDeleteOption.THIS_AND_FUTURE -> {
                    when (val result =
                        deleteRepeatableTaskUseCase.deleteFutureInstances(instanceIdentifier)) {
                        is TaskResult.Success<*> -> {
                            setEffect(TaskListEffect.ShowSnackbar("Instance and future deleted"))
                            loadTasks(currentState.currentListId, currentState.currentSectionId)
                        }

                        is TaskResult.Error -> {
                            setEffect(TaskListEffect.ShowSnackbar("Error deleting future instances: ${result.message}"))
                        }
                    }
                }

                RepeatTaskDeleteOption.ALL_INSTANCES -> {
                    when (val result =
                        deleteRepeatableTaskUseCase.deleteAllInstances(instanceIdentifier)) {
                        is TaskResult.Success<*> -> {
                            setEffect(TaskListEffect.ShowSnackbar("All instances deleted"))
                            loadTasks(currentState.currentListId, currentState.currentSectionId)
                        }

                        is TaskResult.Error -> {
                            setEffect(TaskListEffect.ShowSnackbar("Error deleting all instances: ${result.message}"))
                        }
                    }
                }
            }
        }
    }

    private fun handleUpdateTask(task: Task) {
        viewModelScope.launch {
            when (val result = saveTaskUseCase(task)) {
                is TaskResult.Success -> {
                    loadTasks(currentState.currentListId, currentState.currentSectionId)
                    setEffect(ShowSnackbar("Task updated"))
                }
                is TaskResult.Error -> {
                    setState { copy(error = result.message) }
                    setEffect(ShowSnackbar("Error updating: ${result.message}"))
                }
            }
        }
    }

    private suspend fun saveList(list: TaskList) {
        setState { copy(isLoading = true) }
        when (val result = saveListUseCase(list)) {
            is TaskResult.Success -> {
                loadTasks(currentState.currentListId, currentState.currentSectionId) 
                setEffect(ShowSnackbar("List updated"))
            }

            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(ShowSnackbar("Error updating list: ${result.message}"))
            }
        }
    }

    private suspend fun saveSection(section: TaskSection) {
        setState { copy(isLoading = true) }
        when (val result = saveSectionUseCase(section)) {
            is TaskResult.Success -> {
                loadTasks(currentState.currentListId, currentState.currentSectionId) 
                setEffect(ShowSnackbar("Section saved"))
            }

            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(ShowSnackbar("Error saving section: ${result.message}"))
            }
        }
    }
}