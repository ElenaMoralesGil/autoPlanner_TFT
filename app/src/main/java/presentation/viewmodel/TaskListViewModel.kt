package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.results.TaskResult
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
import android.util.Log

class TaskListViewModel(
    private val getTasksByListUseCase: GetTasksByListUseCase,
    private val filterTasksUseCase: FilterTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val saveListUseCase: SaveListUseCase,
    private val saveSectionUseCase: SaveSectionUseCase,
    private val savedStateHandle: SavedStateHandle, // Keep injection
) : BaseViewModel<TaskListIntent, TaskListState, TaskListEffect>() {

    private var taskLoadingJob: Job? = null

    // --- CORRECTED createInitialState ---
    // It MUST NOT access savedStateHandle here.
    override fun createInitialState(): TaskListState = TaskListState(
        // Initialize with default values only
        currentListId = null, // Start with null, will be updated in init
        currentListName = null,
        currentListColor = null,
        isLoading = true // Start in loading state until init finishes
    )

    init {
        Log.d("TaskListVM", "ViewModel Init Start")
        // Now savedStateHandle is guaranteed to be non-null here

        // Get the initial listId *safely* after construction
        val initialListId = savedStateHandle.get<String?>("listId")?.toLongOrNull()
        Log.d("TaskListVM", "Initial listId from handle in init: $initialListId")

        // Set the initial listId in the state *once*
        // Note: BaseViewModel's init already sets the state using createInitialState.
        // We update it here with the value from SavedStateHandle.
        setState { copy(currentListId = initialListId, isLoading = true) } // Ensure loading is true

        // Observe the SavedStateHandle for subsequent navigation changes
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String?>("listId", initialListId?.toString())
                .map { it?.toLongOrNull() } // Convert String? from nav args to Long?
                .distinctUntilChanged() // Only react to actual changes
                .collectLatest { listId ->
                    Log.d("TaskListVM", "Observed listId change in collectLatest: $listId")
                    // Update state and trigger load
                    // Check if it's different from current state to avoid redundant loads on init
                    if (listId != currentState.currentListId || currentState.tasks.isEmpty()) {
                        setState {
                            copy(
                                currentListId = listId,
                                currentListName = null, // Reset name/color on ID change
                                currentListColor = null,
                                isLoading = true // Set loading true before fetching
                            )
                        }
                        loadTasks(listId)
                    } else {
                        // If listId hasn't actually changed (e.g., during initial setup),
                        // ensure loading is false if it was set true initially
                        if (currentState.isLoading) {
                            setState { copy(isLoading = false) }
                        }
                        Log.d(
                            "TaskListVM",
                            "Skipping load, listId ($listId) hasn't changed or tasks already loaded."
                        )
                    }
                }
        }
        Log.d("TaskListVM", "ViewModel Init End")
    }


    override suspend fun handleIntent(intent: TaskListIntent) {
        when (intent) {
            // Filtering Intents
            is TaskListIntent.UpdateStatusFilter -> updateStatusFilter(intent.status)
            is TaskListIntent.UpdateTimeFrameFilter -> updateTimeFrameFilter(intent.timeFrame)

            // Task Modification Intents
            is TaskListIntent.ToggleTaskCompletion -> toggleTaskCompletion(
                intent.taskId,
                intent.completed
            )
            is TaskListIntent.DeleteTask -> handleDeleteTask(intent.taskId)
            is TaskListIntent.UpdateTask -> handleUpdateTask(intent.task) // For drag-drop, etc.

            // Navigation Intents
            is TaskListIntent.SelectTask -> setEffect(NavigateToTaskDetail(intent.taskId))
            is TaskListIntent.ViewList -> {
                Log.d("TaskListVM", "Intent: ViewList ${intent.listId}")
                // Update SavedStateHandle (as String) which triggers the flow collection
                savedStateHandle["listId"] = intent.listId.toString()
            }
            is TaskListIntent.ViewAllTasks -> {
                Log.d("TaskListVM", "Intent: ViewAllTasks")
                // Set listId to null (as String) in SavedStateHandle
                savedStateHandle["listId"] = null
            }

            // List/Section Editing Intents (triggered from this screen's menu)
            is TaskListIntent.RequestEditList -> currentState.currentListId?.let {
                setEffect(ShowEditListDialog(it))
            }
            is TaskListIntent.RequestEditSections -> currentState.currentListId?.let {
                setEffect(ShowEditSectionsDialog(it))
            }
            is TaskListIntent.SaveList -> saveList(intent.list)
            is TaskListIntent.SaveSection -> saveSection(intent.section)
            is TaskListIntent.LoadTasks -> { // Keep this case, but call the correct load function
                Log.w(
                    "TaskListVM",
                    "Received LoadTasks intent. Reloading for current listId: ${currentState.currentListId}"
                )
                loadTasks(currentState.currentListId) // Reload for the current list
            }
        }
    }

    private fun loadTasks(listId: Long?) {
        taskLoadingJob?.cancel()
        taskLoadingJob = viewModelScope.launch {
            Log.d("TaskListVM", "loadTasks triggered for listId: $listId")
            // Ensure loading state is set if not already
            if (!currentState.isLoading) {
                setState { copy(isLoading = true, error = null) }
            }
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
                .collect { (list, tasks) ->
                    Log.d(
                        "TaskListVM",
                        "Loaded ${tasks.size} tasks for list: ${list?.name ?: "All Tasks"}"
                    )
                    // Update list details along with tasks
                    setState {
                        copy(
                            currentListName = list?.name,
                            currentListColor = list?.colorHex,
                            // Apply filters immediately after getting tasks
                            tasks = tasks,
                            filteredTasks = filterTasksUseCase(
                                tasks,
                                currentState.statusFilter,
                                currentState.timeFrameFilter
                            ),
                            isLoading = false, // Set loading false here
                            error = null
                        )
                    }
                    // applyFilters(tasks) // applyFilters is now integrated into the setState above
                }
        }
    }

    // applyFilters is now integrated into the loadTasks collect block
    // private fun applyFilters(tasks: List<Task>) { ... }

    private fun updateStatusFilter(status: TaskStatus) {
        // Re-apply filter to the currently loaded tasks
        val filteredTasks = filterTasksUseCase(
            currentState.tasks, // Use the unfiltered list
            status,
            currentState.timeFrameFilter
        )
        setState { copy(statusFilter = status, filteredTasks = filteredTasks) }
    }

    private fun updateTimeFrameFilter(timeFrame: TimeFrame) {
        // Re-apply filter to the currently loaded tasks
        val filteredTasks = filterTasksUseCase(
            currentState.tasks, // Use the unfiltered list
            currentState.statusFilter,
            timeFrame
        )
        setState { copy(timeFrameFilter = timeFrame, filteredTasks = filteredTasks) }
    }

    private fun toggleTaskCompletion(taskId: Int, completed: Boolean) {
        viewModelScope.launch {
            updateTaskCompletionInState(taskId, completed) // Optimistic update

            when (val result = toggleTaskCompletionUseCase(taskId, completed)) {
                is TaskResult.Success -> {
                    // Flow should update list automatically if data source changes.
                    // If not, uncommenting loadTasks might be needed, but prefer relying on flow.
                    // loadTasks(currentState.currentListId)
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

    // Helper for optimistic updates
    private fun updateTaskCompletionInState(taskId: Int, completed: Boolean) {
        val updatedTasks = currentState.tasks.map {
            if (it.id == taskId) it.copy(isCompleted = completed) else it
        }
        // Re-apply filters immediately with the optimistically updated list
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
                is TaskResult.Success -> {
                    // Flow should update list automatically
                    setEffect(ShowSnackbar("Task deleted"))
                }
                is TaskResult.Error -> {
                    setState { copy(error = result.message) }
                    setEffect(ShowSnackbar("Error deleting: ${result.message}"))
                }
            }
        }
    }

    // Used for drag/drop reordering etc.
    private fun handleUpdateTask(task: Task) {
        viewModelScope.launch {
            when (val result = saveTaskUseCase(task)) {
                is TaskResult.Success -> {
                    // Flow should update list automatically
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
                // Reload tasks to ensure list name/color is updated in the header
                loadTasks(currentState.currentListId)
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
                // Reload tasks if section affects grouping/filtering in the future.
                loadTasks(currentState.currentListId)
                setEffect(ShowSnackbar("Section saved"))
            }
            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(ShowSnackbar("Error saving section: ${result.message}"))
            }
        }
    }
}