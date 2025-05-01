package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.GetAllListsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetAllSectionsUseCase
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
        }
    }

    private fun loadTask(taskId: Int) {
        if (taskId == 0) {
            setState {
                createInitialState().copy(
                    isNewTask = true,
                    isLoading = false,
                    //startDateConf = com.elena.autoplanner.domain.models.TimePlanning(dateTime = LocalDateTime.now())
                )
            }
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
                            listId = task.listId,
                            sectionId = task.sectionId,
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

            val newStartDateTime = state.startDateConf?.dateTime
            val newDurationMinutes = state.durationConf?.totalMinutes?.toLong()

            // Calculate the new scheduled end time IF start and duration exist
            val newScheduledEndDateTime =
                if (newStartDateTime != null && newDurationMinutes != null) {
                    newStartDateTime.plusMinutes(newDurationMinutes)
                } else {
                    // If start or duration is cleared, clear the scheduled end time too
                    null
                }

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
                .scheduledStartDateTime(null)
                .scheduledEndDateTime(null)
                .listId(state.listId)
                .sectionId(state.sectionId)
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
        setState {
            copy(
                listId = listId,
                // Reset section if list is changed or removed
                sectionId = if (listId == null || listId != this.listId) null else this.sectionId
            )
        }
    }

    private fun assignSection(sectionId: Long?) {
        // Only allow assigning section if a list is selected
        if (currentState.listId != null) {
            setState { copy(sectionId = sectionId) }
        }
    }

    private fun loadListsForSelection() {
        viewModelScope.launch {
            setState { copy(isLoadingSelection = true) } // Add isLoadingSelection to state
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
            setState {
                copy(
                    isLoadingSelection = true,
                    availableSections = emptyList()
                )
            } // Clear previous sections
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
}