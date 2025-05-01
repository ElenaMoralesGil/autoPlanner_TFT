package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskListInfo
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.GetListsInfoUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveListUseCase
import com.elena.autoplanner.presentation.effects.MoreEffect
import com.elena.autoplanner.presentation.intents.MoreIntent
import com.elena.autoplanner.presentation.states.MoreState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoreViewModel(
    private val getListsInfoUseCase: GetListsInfoUseCase,
    private val saveListUseCase: SaveListUseCase,
    // Add DeleteListUseCase etc. later if needed
) : BaseViewModel<MoreIntent, MoreState, MoreEffect>() {

    override fun createInitialState(): MoreState = MoreState()

    init {
        loadLists()
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
            is MoreIntent.SelectList -> setEffect(MoreEffect.NavigateToTasks(intent.listId))
            is MoreIntent.CreateList -> saveList(intent.name, intent.colorHex)
            is MoreIntent.ToggleListExpansion -> {
                val currentExpanded = currentState.expandedListIds
                val newExpanded = if (currentExpanded.contains(intent.listId)) {
                    currentExpanded - intent.listId
                } else {
                    currentExpanded + intent.listId
                }
                setState { copy(expandedListIds = newExpanded) }
            }

            MoreIntent.RequestCreateList -> setEffect(MoreEffect.ShowCreateListDialog)
        }
    }

    private suspend fun saveList(name: String, colorHex: String) {
        setState { copy(isLoading = true) } // Indicate saving
        val newList = TaskList(name = name, colorHex = colorHex)
        when (val result = saveListUseCase(newList)) {
            is TaskResult.Success -> {
                // No need to manually update state, flow should trigger reload
                setEffect(MoreEffect.ShowSnackbar("List '$name' created"))
            }

            is TaskResult.Error -> {
                setState { copy(isLoading = false, error = result.message) }
                setEffect(MoreEffect.ShowSnackbar("Error creating list: ${result.message}"))
            }
        }
        // isLoading will be reset by the flow collection
    }
}