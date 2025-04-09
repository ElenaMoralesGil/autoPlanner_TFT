package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


interface Intent

interface UiEffect

abstract class BaseViewModel<I : Intent, S, E : UiEffect> : ViewModel() {

    private val _state = MutableStateFlow<S?>(null)
    val state: StateFlow<S?> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<E>()
    val effect: SharedFlow<E> = _effect.asSharedFlow()

    private val _intent = MutableSharedFlow<I>()

    val currentState: S
        get() = _state.value ?: createInitialState()

    init {
        viewModelScope.launch {
            _state.value = createInitialState()

            _intent.collect { intent ->
                handleIntent(intent)
            }
        }
    }
    fun sendIntent(intent: I) {
        viewModelScope.launch { _intent.emit(intent) }
    }

    protected fun setState(reducer: S.() -> S) {
        viewModelScope.launch {
            _state.update { (it ?: createInitialState()).reducer() }
        }
    }


    fun setEffect(effect: E) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    abstract fun createInitialState(): S
    abstract suspend fun handleIntent(intent: I)
}
