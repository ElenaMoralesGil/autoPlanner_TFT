package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.GetAllSectionsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetTasksByListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveSectionUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.presentation.effects.TaskListEffect
import com.elena.autoplanner.presentation.effects.TaskListEffect.*
import com.elena.autoplanner.presentation.intents.TaskListIntent
import com.elena.autoplanner.presentation.states.TaskListState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
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
    private val savedStateHandle: SavedStateHandle, // Keep injection
) : BaseViewModel<TaskListIntent, TaskListState, TaskListEffect>() {

    private var taskLoadingJob: Job? = null
    override fun createInitialState(): TaskListState = TaskListState(isLoading = true)

    init {
        Log.d("TaskListVM", "ViewModel Init Start")
        viewModelScope.launch {
            combine(
                savedStateHandle.getStateFlow<String?>("listId", null).map { it?.toLongOrNull() },
                savedStateHandle.getStateFlow<String?>("sectionId", null).map { it?.toLongOrNull() }
            ) { listId, sectionId ->
                Pair(listId, sectionId)
            }
                .distinctUntilChanged()
                .collectLatest { (listId, sectionId) ->
                    Log.d(
                        "TaskListVM",
                        "SavedStateHandle observed change: listId=$listId, sectionId=$sectionId. Current state IDs: list=${currentState.currentListId}, section=${currentState.currentSectionId}, isLoading=${currentState.isLoading}"
                    )

                    val listIdChanged = listId != currentState.currentListId
                    val sectionIdChanged = sectionId != currentState.currentSectionId
                    val isInitialLoadingState =
                        currentState.tasks.isEmpty() && currentState.isLoading
                    val shouldLoad = listIdChanged || sectionIdChanged || isInitialLoadingState
                    // Check the condition carefully

                    Log.d(
                        "TaskListVM",
                        "Change detection: listIdChanged=$listIdChanged, sectionIdChanged=$sectionIdChanged, needsInitialLoad=$shouldLoad"
                    )

                    if (shouldLoad) {
                        Log.d(
                            "TaskListVM",
                            "IDs changed or initial load needed. Setting state and calling loadTasks."
                        )

                        if (!currentState.isLoading) {
                            setState { copy(isLoading = true) }
                        }
                        setState {
                            copy(
                                currentListId = listId,
                                currentSectionId = sectionId,
                                isLoading = true // Set loading true when starting load
                            )
                        }
                        loadTasks(listId, sectionId)
                    } else {
                        Log.d(
                            "TaskListVM",
                            "Skipping load, IDs haven't changed or already loaded/loading."
                        )
                        // If it was loading but IDs didn't change, maybe stop loading?
                        if (currentState.isLoading) {
                            Log.d("TaskListVM", "Stopping potentially redundant loading state.")
                            // setState { copy(isLoading = false) } // Be careful with this, might cause flicker if data *is* coming
                        }
                    }
                }
        }
        Log.d("TaskListVM", "ViewModel Init End")
    }

    override suspend fun handleIntent(intent: TaskListIntent) {
        when (intent) {
            is TaskListIntent.UpdateStatusFilter -> updateStatusFilter(intent.status)
            is TaskListIntent.UpdateTimeFrameFilter -> updateTimeFrameFilter(intent.timeFrame)
            is TaskListIntent.ToggleTaskCompletion -> toggleTaskCompletion(
                intent.taskId,
                intent.completed
            )

            is TaskListIntent.SelectTask -> setEffect(NavigateToTaskDetail(intent.taskId))
            is TaskListIntent.UpdateTask -> handleUpdateTask(intent.task)
            is TaskListIntent.DeleteTask -> handleDeleteTask(intent.taskId)
            // Navigation Intents update SavedStateHandle
            is TaskListIntent.ViewList -> { // Navigating to a list clears the section
                savedStateHandle["listId"] = intent.listId.toString()
                savedStateHandle["sectionId"] = null
            }

            is TaskListIntent.ViewAllTasks -> { // Navigating to all tasks clears list and section
                savedStateHandle["listId"] = null
                savedStateHandle["sectionId"] = null
            }
            // Handle ViewSection intent
            is TaskListIntent.ViewSection -> {
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
                Log.d("TaskListVM", "LoadTasks Intent received. Reloading for current state IDs.")
                loadTasks(currentState.currentListId, currentState.currentSectionId)
            }

            is TaskListIntent.ArgumentsChanged -> {
                Log.d(
                    "TaskListVM",
                    "ArgumentsChanged Intent received: listId=${intent.listId}, sectionId=${intent.sectionId}"
                )
                // Only load if IDs actually changed from the current state to prevent redundant loads
                if (intent.listId != currentState.currentListId || intent.sectionId != currentState.currentSectionId || currentState.tasks.isEmpty()) {
                    Log.d("TaskListVM", "IDs changed or tasks empty, calling loadTasks.")
                    loadTasks(intent.listId, intent.sectionId)
                } else {
                    Log.d("TaskListVM", "IDs haven't changed, skipping loadTasks call.")
                    // If it was loading, stop it now as the trigger was potentially redundant
                    if (currentState.isLoading) {
                        setState { copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun loadTasks(listId: Long?, sectionId: Long?) {
        taskLoadingJob?.cancel()
        taskLoadingJob = viewModelScope.launch {
            Log.d(
                "TaskListVM",
                "loadTasks function started for listId: $listId, sectionId: $sectionId"
            )
            if (!currentState.isLoading) {
                setState {
                    copy(
                        isLoading = true,
                        error = null,
                        currentListId = listId,
                        currentSectionId = sectionId
                    )
                }
            }

            // Fetch tasks for the list
            getTasksByListUseCase(listId)
                .catch { error ->
                    Log.e("TaskListVM", "Error loading tasks for list $listId", error)
                    setState {
                        copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Unknown error"
                        )
                    }
                    setEffect(ShowSnackbar("Error loading tasks: ${error.localizedMessage}"))
                }
                .collect { (list, tasksForList) ->
                    Log.d(
                        "TaskListVM",
                        "Collected from use case: List Name='${list?.name ?: "All"}', Task Count=${tasksForList.size}"
                    )

                    // Filter by Section ID if provided
                    val tasksToShow = if (sectionId != null && listId != null) {
                        tasksForList.filter { it.sectionId == sectionId }
                    } else {
                        tasksForList
                    }

                    // --- Fetch Section Name if sectionId is not null ---
                    var sectionName: String? = null
                    if (sectionId != null && listId != null) {
                        when (val sectionsResult = getAllSectionsUseCase(listId)) {
                            is TaskResult.Success -> {
                                sectionName = sectionsResult.data.find { it.id == sectionId }?.name
                            }

                            is TaskResult.Error -> {
                                Log.w(
                                    "TaskListVM",
                                    "Could not fetch section name for $sectionId: ${sectionsResult.message}"
                                )
                                // Optionally show a snackbar or just display ID
                            }
                        }
                    }

                    Log.d(
                        "TaskListVM",
                        "Updating state: listId=${list?.id}, sectionId=$sectionId, listName=${list?.name}, sectionName=$sectionName, taskCount=${tasksToShow.size}"
                    )
                    setState {
                        copy(
                            currentListId = list?.id,
                            currentSectionId = sectionId,
                            currentListName = list?.name,
                            currentListColor = list?.colorHex,
                            currentSectionName = sectionName, // Store fetched section name
                            tasks = tasksToShow,
                            filteredTasks = filterTasksUseCase(
                                tasksToShow,
                                statusFilter,
                                timeFrameFilter
                            ),
                            isLoading = false,
                            error = null
                        )
                    }
                }
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
            updateTaskCompletionInState(taskId, completed) // Optimistic update
            when (val result = toggleTaskCompletionUseCase(taskId, completed)) {
                is TaskResult.Success -> {
                    val message = if (completed) "Task completed" else "Task marked incomplete"
                    setEffect(ShowSnackbar(message))
                }

                is TaskResult.Error -> {
                    updateTaskCompletionInState(taskId, !completed) // Revert
                    setState { copy(error = result.message) }
                    setEffect(ShowSnackbar("Error: ${result.message}"))
                }
            }
        }
    }

    private fun updateTaskCompletionInState(taskId: Int, completed: Boolean) {
        val updatedTasks = currentState.tasks.map {
            if (it.id == taskId) it.copy(isCompleted = completed) else it
        }
        val filtered = filterTasksUseCase(
            updatedTasks,
            currentState.statusFilter,
            currentState.timeFrameFilter
        )
        setState { copy(tasks = updatedTasks, filteredTasks = filtered) }
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

    private fun handleUpdateTask(task: Task) {
        viewModelScope.launch {
            when (val result = saveTaskUseCase(task)) {
                is TaskResult.Success -> setEffect(ShowSnackbar("Task updated"))
                is TaskResult.Error -> {
                    setState { copy(error = result.message) }
                    setEffect(ShowSnackbar("Error updating: ${result.message}"))
                }
            }
        }
    }

    // --- List/Section Save (remain the same) ---
    private suspend fun saveList(list: TaskList) {
        setState { copy(isLoading = true) }
        when (val result = saveListUseCase(list)) {
            is TaskResult.Success -> {
                loadTasks(currentState.currentListId, currentState.currentSectionId) // Reload
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
                loadTasks(currentState.currentListId, currentState.currentSectionId) // Reload
                setEffect(ShowSnackbar("Section saved"))
            }

            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(ShowSnackbar("Error saving section: ${result.message}"))
            }
        }
    }
}