package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.GetAllListsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetAllSectionsUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveSectionUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.presentation.effects.TaskEditEffect
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.states.TaskEditState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskEditViewModel(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val getAllListsUseCase: GetAllListsUseCase,
    private val getAllSectionsUseCase: GetAllSectionsUseCase,
    private val saveListUseCase: SaveListUseCase,         
    private val saveSectionUseCase: SaveSectionUseCase,
) : BaseTaskViewModel<TaskEditIntent, TaskEditState, TaskEditEffect>() {

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
            is TaskEditIntent.AssignList -> assignList(intent.listId)
            is TaskEditIntent.AssignSection -> assignSection(intent.sectionId)
            is TaskEditIntent.LoadListsForSelection -> loadListsForSelection()
            is TaskEditIntent.LoadSectionsForSelection -> intent.listId?.let {
                loadSectionsForSelection(
                    it
                )
            }

            is TaskEditIntent.CreateAndAssignList -> createAndAssignList(
                intent.name,
                intent.colorHex
            )

            is TaskEditIntent.CreateAndAssignSection -> createAndAssignSection(
                intent.name,
                intent.listId
            )
            is TaskEditIntent.UpdateSplitting -> setState { copy(allowSplitting = intent.allowSplitting) }
        }
    }

    private fun loadTask(taskId: Int) {
        if (taskId == 0) {
            setState { createInitialState().copy(isNewTask = true, isLoading = false) }
            return
        }
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            executeTaskOperation(
                setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                operation = { getTaskUseCase(taskId) },
                onSuccess = { task ->
                    setState {
                        copy(
                            isLoading = false, isNewTask = false, taskId = task.id,
                            name = task.name, priority = task.priority,
                            startDateConf = task.startDateConf, endDateConf = task.endDateConf,
                            durationConf = task.durationConf, reminderPlan = task.reminderPlan,
                            repeatPlan = task.repeatPlan, subtasks = task.subtasks,
                            listId = task.listId,
                            sectionId = task.sectionId,
                            allowSplitting = task.allowSplitting,
                            error = null
                        )
                    }


                },
                onError = { errorMessage ->
                    setState { copy(isLoading = false, error = errorMessage) }
                    setEffect(TaskEditEffect.ShowSnackbar("Error loading task: $errorMessage"))
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

            if (state.endDateConf != null && state.startDateConf?.dateTime != null &&
                state.endDateConf.dateTime?.isBefore(state.startDateConf.dateTime) == true
            ) {
                setEffect(TaskEditEffect.ShowSnackbar("End date cannot be before start date"))
                return@launch
            }

            setState { copy(isLoading = true) }

            val taskToSave = Task.Builder()
                .id(state.taskId)
                .name(state.name)
                .priority(state.priority)
                .startDateConf(state.startDateConf)
                .endDateConf(state.endDateConf)
                .durationConf(state.durationConf)
                .reminderPlan(state.reminderPlan)
                .repeatPlan(state.repeatPlan)
                .subtasks(state.subtasks)
                .listId(state.listId)
                .sectionId(state.sectionId)
                .allowSplitting(currentState.allowSplitting)
                .build()

            executeTaskOperation(
                setLoadingState = { isLoading -> setState { copy(isLoading = isLoading) } },
                operation = { saveTaskUseCase(taskToSave) },
                onSuccess = { _ ->
                    val message = if (state.isNewTask) "Task created" else "Task updated"
                    setEffect(TaskEditEffect.NavigateBack)
                    setEffect(TaskEditEffect.ShowSnackbar(message))
                },
                onError = { errorMessage ->
                    setState { copy(isLoading = false, error = errorMessage) }
                    setEffect(TaskEditEffect.ShowSnackbar("Error saving task: $errorMessage"))
                }
            )
        }
    }

    private fun assignList(listId: Long?) {
        Log.d("TaskEditVM", "Assigning List ID: $listId")
        val oldListId = currentState.listId
        setState {
            copy(
                listId = listId,

                sectionId = if (listId == null || listId != oldListId) null else this.sectionId,

                availableSections = if (listId == null || listId != oldListId) emptyList() else this.availableSections
            )
        }

        if (listId != null && listId != oldListId) {
            loadSectionsForSelection(listId)
        }
    }

    private fun assignSection(sectionId: Long?) {
        Log.d("TaskEditVM", "Assigning Section ID: $sectionId")
        if (currentState.listId != null) {
            setState { copy(sectionId = sectionId) }
        } else {
            Log.w("TaskEditVM", "Attempted to assign section without a list selected.")
            setEffect(TaskEditEffect.ShowSnackbar("Please select a list first."))
        }
    }

    private fun loadListsForSelection() {
        viewModelScope.launch {
            setState { copy(isLoadingSelection = true) }
            when (val result = getAllListsUseCase()) {
                is TaskResult.Success -> setState {
                    copy(
                        availableLists = result.data,
                        isLoadingSelection = false
                    )
                }
                is TaskResult.Error -> {
                    setState { copy(isLoadingSelection = false) }
                    setEffect(TaskEditEffect.ShowSnackbar("Error loading lists: ${result.message}"))
                }
            }
        }
    }

    private fun loadSectionsForSelection(listId: Long) {
        viewModelScope.launch {
            setState { copy(isLoadingSelection = true, availableSections = emptyList()) }
            when (val result = getAllSectionsUseCase(listId)) {
                is TaskResult.Success -> setState {
                    copy(
                        availableSections = result.data,
                        isLoadingSelection = false
                    )
                }
                is TaskResult.Error -> {
                    setState { copy(isLoadingSelection = false) }
                    setEffect(TaskEditEffect.ShowSnackbar("Error loading sections: ${result.message}"))
                }
            }
        }
    }

    private suspend fun createAndAssignList(name: String, colorHex: String) {
        setState { copy(isLoadingSelection = true) }
        val newList = TaskList(name = name, colorHex = colorHex)
        when (val saveResult = saveListUseCase(newList)) {
            is TaskResult.Success -> {
                val newListId = saveResult.data
                Log.d("TaskEditVM", "Created new list with ID: $newListId. Assigning to task.")
                assignList(newListId)
                loadListsForSelection() 
                setState { copy(isLoadingSelection = false) }
            }

            is TaskResult.Error -> {
                setState { copy(isLoadingSelection = false) }
                setEffect(TaskEditEffect.ShowSnackbar("Error creating list: ${saveResult.message}"))
            }
        }
    }

    private suspend fun createAndAssignSection(name: String, listId: Long) {
        setState { copy(isLoadingSelection = true) }
        val newSection = TaskSection(listId = listId, name = name)
        when (val saveResult = saveSectionUseCase(newSection)) {
            is TaskResult.Success -> {
                val newSectionId = saveResult.data
                Log.d(
                    "TaskEditVM",
                    "Created new section with ID: $newSectionId. Assigning to task."
                )
                assignSection(newSectionId)
                loadSectionsForSelection(listId) 
                setState { copy(isLoadingSelection = false) }
            }

            is TaskResult.Error -> {
                setState { copy(isLoadingSelection = false) }
                setEffect(TaskEditEffect.ShowSnackbar("Error creating section: ${saveResult.message}"))
            }
        }
    }
}