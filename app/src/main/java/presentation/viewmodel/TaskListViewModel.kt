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
    private val savedStateHandle: SavedStateHandle,
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

            is TaskListIntent.SelectTask -> setEffect(NavigateToTaskDetail(intent.taskId))
            is TaskListIntent.UpdateTask -> handleUpdateTask(intent.task)
            is TaskListIntent.DeleteTask -> handleDeleteTask(intent.taskId)

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
        }
    }



    private fun loadTasks(listId: Long?, sectionId: Long?) {
        taskLoadingJob?.cancel()
        taskLoadingJob = viewModelScope.launch {
            Log.d("TaskListVM", "loadTasks ENTERED for listId: $listId, sectionId: $sectionId.")



            setState { copy(isLoading = true, isNavigating = false, error = null) }
            Log.d("TaskListVM", "loadTasks: Set isLoading=true, isNavigating=false. State: $currentState")

            getTasksByListUseCase(listId)
                .catch { error ->
                    Log.e("TaskListVM", "Error in getTasksByListUseCase for list $listId", error)
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
                }
                .collect { (listData, tasksForList) ->
                    Log.d("TaskListVM", "Collected from use case: List Name='${listData?.name ?: "All"}', Task Count=${tasksForList.size} for requested listId=$listId, sectionId=$sectionId")

                    val tasksToShow = if (sectionId != null && listId != null) {
                        tasksForList.filter { it.sectionId == sectionId }
                    } else {
                        tasksForList
                    }

                    var fetchedSectionName: String? = null
                    if (sectionId != null && listId != null) {
                        when (val sectionsResult = getAllSectionsUseCase(listId)) {
                            is TaskResult.Success -> fetchedSectionName = sectionsResult.data.find { it.id == sectionId }?.name
                            is TaskResult.Error -> Log.w("TaskListVM", "Could not fetch section name for $sectionId: ${sectionsResult.message}")
                        }
                    }

                    Log.d("TaskListVM", "Updating state post-load: listId=${listData?.id}, sectionId=$sectionId, listName=${listData?.name}, sectionName=$fetchedSectionName, taskCount=${tasksToShow.size}")
                    setState {
                        copy(
                            currentListId = listData?.id,
                            currentSectionId = sectionId, 
                            currentListName = listData?.name,
                            currentListColor = listData?.colorHex,
                            currentSectionName = fetchedSectionName,
                            tasks = tasksToShow,
                            filteredTasks = filterTasksUseCase(tasksToShow, statusFilter, timeFrameFilter),
                            isLoading = false,
                            error = null,

                            requestedListId = listData?.id,
                            requestedSectionId = sectionId
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
            updateTaskCompletionInState(taskId, completed) 
            when (val result = toggleTaskCompletionUseCase(taskId, completed)) {
                is TaskResult.Success -> {
                    val message = if (completed) "Task completed" else "Task marked incomplete"
                    setEffect(ShowSnackbar(message))
                }

                is TaskResult.Error -> {
                    updateTaskCompletionInState(taskId, !completed) 
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