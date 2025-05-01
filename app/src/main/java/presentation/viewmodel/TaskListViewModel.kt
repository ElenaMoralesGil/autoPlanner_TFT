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

// Inherit from a simpler BaseViewModel or directly from ViewModel
class TaskListViewModel(
    // Remove GetTasksUseCase, only GetTasksByListUseCase is needed
    private val getTasksByListUseCase: GetTasksByListUseCase,
    private val filterTasksUseCase: FilterTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase, // Keep for potential updates like drag/drop
    private val saveListUseCase: SaveListUseCase, // For editing list name/color
    private val saveSectionUseCase: SaveSectionUseCase, // For editing sections
    private val savedStateHandle: SavedStateHandle, // Inject SavedStateHandle
) : BaseViewModel<TaskListIntent, TaskListState, TaskListEffect>() { // Use BaseViewModel

    // Read listId as String? from SavedStateHandle and map to Long?
    private val _currentListId: StateFlow<Long?> =
        savedStateHandle.getStateFlow<String?>("listId", null)
            .map { it?.toLongOrNull() } // Convert String? from nav args to Long?
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            ) // Convert to StateFlow

    private var taskLoadingJob: Job? = null

    override fun createInitialState(): TaskListState = TaskListState(
        currentListId = _currentListId.value // Initialize from StateFlow's current value
    )

    init {
        // Observe changes in listId StateFlow and reload tasks
        viewModelScope.launch {
            _currentListId.collectLatest { listId ->
                // Reset list details when ID changes, then load
                setState {
                    copy(
                        currentListId = listId,
                        currentListName = null, // Reset name
                        currentListColor = null // Reset color
                    )
                }
                loadTasks(listId)
            }
        }
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
                // Update SavedStateHandle (as String) to trigger navigation and _currentListId flow
                savedStateHandle["listId"] = intent.listId.toString()
            }

            is TaskListIntent.ViewAllTasks -> {
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
            is TaskListIntent.LoadTasks -> TODO()
        }
    }

    // Renamed from loadTasks(listId) to avoid conflict if BaseTaskViewModel was used
    private fun loadTasks(listId: Long?) {
        taskLoadingJob?.cancel() // Cancel previous loading job
        taskLoadingJob = viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            getTasksByListUseCase(listId)
                .catch { error -> // Catch errors during flow processing
                    setState {
                        copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Unknown error loading tasks"
                        )
                    }
                    setEffect(TaskListEffect.ShowSnackbar("Error loading tasks: ${error.localizedMessage}"))
                }
                .collect { (list, tasks) ->
                    setState {
                        copy(
                            currentListName = list?.name,

                            currentListColor = list?.colorHex
                        )
                    }
                    applyFilters(tasks) // Apply filters to the fetched tasks
                }
        }
    }

    // Removed the other loadTasks() method that used GetTasksUseCase

    private fun applyFilters(tasks: List<Task>) {
        val filteredTasks = filterTasksUseCase(
            tasks,
            currentState.statusFilter,
            currentState.timeFrameFilter
        )
        setState {
            copy(
                isLoading = false, // Loading finished
                tasks = tasks, // Store the *unfiltered* tasks for the current list view
                filteredTasks = filteredTasks, // Store the filtered tasks for display
                error = null
            )
        }
    }

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
            // Optimistic UI update
            updateTaskCompletionInState(taskId, completed)

            // Perform operation
            when (val result = toggleTaskCompletionUseCase(taskId, completed)) {
                is TaskResult.Success -> {
                    // Flow should ideally update the list automatically.
                    // Explicit reload removed for now, rely on flow.
                    // If issues arise, uncomment: loadTasks(currentState.currentListId)
                    val message = if (completed) "Task completed" else "Task marked incomplete"
                    setEffect(TaskListEffect.ShowSnackbar(message))
                }

                is TaskResult.Error -> {
                    // Revert optimistic update
                    updateTaskCompletionInState(taskId, !completed)
                    setState { copy(error = result.message) }
                    setEffect(TaskListEffect.ShowSnackbar("Error: ${result.message}"))
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
        applyFilters(updatedTasks)
    }

    private fun handleDeleteTask(taskId: Int) {
        viewModelScope.launch {
            // Consider adding a specific isDeleting flag to state if needed for UI
            // setState { copy(isDeleting = true) }
            when (val result = deleteTaskUseCase(taskId)) {
                is TaskResult.Success -> {
                    // setState { copy(isDeleting = false) }
                    // Task flow should update automatically. Explicit reload removed.
                    // If issues arise, uncomment: loadTasks(currentState.currentListId)
                    setEffect(TaskListEffect.ShowSnackbar("Task deleted"))
                }

                is TaskResult.Error -> {
                    // setState { copy(isDeleting = false, error = result.message) }
                    setState { copy(error = result.message) } // Keep it simple
                    setEffect(TaskListEffect.ShowSnackbar("Error deleting: ${result.message}"))
                }
            }
        }
    }

    // Used for drag/drop reordering etc.
    private fun handleUpdateTask(task: Task) {
        viewModelScope.launch {
            // Consider adding a specific isUpdating flag
            when (val result = saveTaskUseCase(task)) {
                is TaskResult.Success -> {
                    // Task flow should update. Explicit reload removed.
                    // If issues arise, uncomment: loadTasks(currentState.currentListId)
                    setEffect(TaskListEffect.ShowSnackbar("Task updated"))
                }

                is TaskResult.Error -> {
                    setState { copy(error = result.message) }
                    setEffect(TaskListEffect.ShowSnackbar("Error updating: ${result.message}"))
                }
            }
        }
    }

    // --- Methods for list/section editing invoked from this screen's menu ---
    private suspend fun saveList(list: TaskList) {
        setState { copy(isLoading = true) } // Use general loading flag
        when (val result = saveListUseCase(list)) {
            is TaskResult.Success -> {
                // Reload tasks to ensure list name/color is updated in the header
                // This is necessary because list details aren't part of the main task flow
                loadTasks(currentState.currentListId)
                setEffect(TaskListEffect.ShowSnackbar("List updated"))
            }

            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(TaskListEffect.ShowSnackbar("Error updating list: ${result.message}"))
            }
        }
        // isLoading will be set to false by loadTasks finishing
    }

    private suspend fun saveSection(section: TaskSection) {
        setState { copy(isLoading = true) }
        when (val result = saveSectionUseCase(section)) {
            is TaskResult.Success -> {
                // Sections aren't directly shown here, but reload might be needed
                // if future UI groups tasks by section within this screen.
                loadTasks(currentState.currentListId) // Reload for now
                setEffect(TaskListEffect.ShowSnackbar("Section saved"))
            }

            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(TaskListEffect.ShowSnackbar("Error saving section: ${result.message}"))
            }
        }
        // isLoading will be set to false by loadTasks finishing
    }
}