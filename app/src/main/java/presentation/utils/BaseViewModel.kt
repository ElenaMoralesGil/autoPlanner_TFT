package com.elena.autoplanner.presentation.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.presentation.intents.BaseIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


abstract class BaseViewModel<Intent : BaseIntent, State : Any> : ViewModel() {
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    val currentState: State
        get() = _state.value ?: createInitialState()

    abstract fun createInitialState(): State
    abstract suspend fun handleIntent(intent: Intent)

    protected fun setState(reducer: State.() -> State) {
        _state.update { (it ?: createInitialState()).reducer() }
    }

    fun sendIntent(intent: Intent) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }
}