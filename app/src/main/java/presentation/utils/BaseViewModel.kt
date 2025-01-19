package com.elena.autoplanner.presentation.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.presentation.intents.BaseIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


abstract class BaseViewModel<T : BaseIntent> : ViewModel() {

    private val _intentsFlow = MutableSharedFlow<T>(extraBufferCapacity = 64)
    val intentsFlow: SharedFlow<T> = _intentsFlow

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch {
            _intentsFlow.collect { intent ->
                onTriggerEvent(intent)
            }
        }
    }

    fun triggerEvent(intent: T) {
        viewModelScope.launch {
            _intentsFlow.emit(intent)
        }
    }

    protected abstract fun onTriggerEvent(intent: T)

    protected fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }
}