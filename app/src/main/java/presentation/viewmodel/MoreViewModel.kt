package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskListInfo
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.GetAllSectionsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetListsInfoUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveSectionUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.presentation.effects.MoreEffect
import com.elena.autoplanner.presentation.intents.MoreIntent
import com.elena.autoplanner.presentation.states.MoreState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoreViewModel(
    private val getListsInfoUseCase: GetListsInfoUseCase,
    private val saveListUseCase: SaveListUseCase,
    private val getAllSectionsUseCase: GetAllSectionsUseCase,
    private val saveSectionUseCase: SaveSectionUseCase,
    private val getTasksUseCase: GetTasksUseCase,
) : BaseViewModel<MoreIntent, MoreState, MoreEffect>() {

    override fun createInitialState(): MoreState = MoreState()

    init {
        loadLists()
        observeTotalTaskCount()
    }

    private fun observeTotalTaskCount() { // <-- New function
        viewModelScope.launch {
            getTasksUseCase()
                .catch { error ->
                    Log.e("MoreViewModel", "Error observing total tasks", error)
                }
                .collectLatest { tasks ->
                    setState { copy(totalTaskCount = tasks.size) }
                }
        }
    }

    private fun loadLists() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            getListsInfoUseCase().collectLatest { result ->
                when (result) {
                    is TaskResult.Success -> {
                        setState { copy(isLoading = false, lists = result.data, error = null) }
                    }
                    is TaskResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        setEffect(MoreEffect.ShowSnackbar("Error loading lists: ${result.message}"))
                    }
                }
            }
        }
    }

    override suspend fun handleIntent(intent: MoreIntent) {
        when (intent) {
            is MoreIntent.SelectList -> {
                // Effect is handled by UI callback now, but keep if needed
                Log.d("MoreViewModel", "Intent: SelectList ${intent.listId}")
                intent.listId?.let { setEffect(MoreEffect.NavigateToTasks(it)) }
            }
            is MoreIntent.CreateList -> saveList(intent.name, intent.colorHex)
            is MoreIntent.ToggleListExpansion -> handleToggleExpansion(intent.listId) // Refined this
            is MoreIntent.LoadSections -> loadSections(intent.listId) // Keep explicit load if needed elsewhere
            is MoreIntent.CreateSection -> createSection(intent.listId, intent.sectionName)
            MoreIntent.RequestCreateList -> setEffect(MoreEffect.ShowCreateListDialog)
        }
    }

    private suspend fun handleToggleExpansion(listId: Long) {
        val currentExpanded = currentState.expandedListIds
        val isCurrentlyExpanded = currentExpanded.contains(listId)
        val newExpanded = if (isCurrentlyExpanded) {
            currentExpanded - listId
        } else {
            currentExpanded + listId
        }
        setState { copy(expandedListIds = newExpanded) }

        // If expanding and sections not loaded yet for this list, trigger load
        if (!isCurrentlyExpanded && !currentState.sectionsByListId.containsKey(listId)) {
            loadSections(listId)
        }
    }

    private suspend fun loadSections(listId: Long) {
        if (currentState.isLoadingSectionsFor == listId) return // Don't load if already loading

        Log.d("MoreViewModel", "Loading sections for listId: $listId")
        setState {
            copy(
                isLoadingSectionsFor = listId,
                sectionError = null
            )
        } // Clear previous section error

        when (val result = getAllSectionsUseCase(listId)) {
            is TaskResult.Success -> {
                setState {
                    copy(
                        sectionsByListId = sectionsByListId + (listId to result.data),
                        isLoadingSectionsFor = null // Clear loading indicator
                    )
                }
                Log.d(
                    "MoreViewModel",
                    "Successfully loaded ${result.data.size} sections for list $listId"
                )
            }

            is TaskResult.Error -> {
                setState {
                    copy(
                        isLoadingSectionsFor = null, // Clear loading indicator on error
                        sectionError = "Could not load sections for list." // Set specific error
                    )
                }
                // Optionally show snackbar, but error message might be displayed in UI
                // setEffect(MoreEffect.ShowSnackbar("Error loading sections: ${result.message}"))
                Log.e("MoreViewModel", "Error loading sections for list $listId: ${result.message}")
            }
        }
    }

    private suspend fun saveList(name: String, colorHex: String) {
        setState { copy(isLoading = true) }
        val newList = TaskList(name = name, colorHex = colorHex)
        when (val result = saveListUseCase(newList)) {
            is TaskResult.Success -> {
                setEffect(MoreEffect.ShowSnackbar("List '$name' created"))
                // Flow collection in loadLists() should automatically refresh the list
            }
            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(MoreEffect.ShowSnackbar("Error creating list: ${result.message}"))
            }
        }

    }

    private suspend fun createSection(listId: Long, sectionName: String) {
        // Optionally add loading state specific to section creation if needed
        val newSection = TaskSection(listId = listId, name = sectionName)
        when (val result = saveSectionUseCase(newSection)) {
            is TaskResult.Success -> {
                setEffect(MoreEffect.ShowSnackbar("Section '$sectionName' created"))
                // Invalidate cached sections for this list and reload them
                setState { copy(sectionsByListId = sectionsByListId - listId) } // Remove old sections
                loadSections(listId) // Reload sections
            }

            is TaskResult.Error -> {
                setEffect(MoreEffect.ShowSnackbar("Error creating section: ${result.message}"))
            }
        }
    }
}