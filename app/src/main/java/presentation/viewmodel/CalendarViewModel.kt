package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetExpandedTasksUseCase
import com.elena.autoplanner.presentation.effects.CalendarEffect
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.states.CalendarState
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class CalendarViewModel(
    private val getExpandedTasksUseCase: GetExpandedTasksUseCase,
) : BaseViewModel<CalendarIntent, CalendarState, CalendarEffect>() {

    var loadedTasks: List<Task> = emptyList()

    override fun createInitialState(): CalendarState = CalendarState()

    override suspend fun handleIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.ChangeDate -> handleDateChange(intent.date, intent.dismiss)
            is CalendarIntent.ChangeView -> setState { copy(currentView = intent.view) }
            is CalendarIntent.ToggleDatePicker -> setState { copy(showDatePicker = intent.show) }
        }
    }

    private fun handleDateChange(newDate: LocalDate, dismiss: Boolean) {
        val adjustedDate = when (currentState.currentView) {
            CalendarView.WEEK -> newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            else -> newDate
        }

        setState {
            copy(
                currentDate = adjustedDate,
                showDatePicker = if (dismiss) false else showDatePicker
            )
        }
        // NUEVO: Recargar tareas para el mes de la nueva fecha
        loadTasksForMonth(adjustedDate)
    }

    // NUEVO: Cargar tareas para el mes de la fecha dada
    fun loadTasksForMonth(date: LocalDate) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            getExpandedTasksUseCase.invoke(
                startDate = date.withDayOfMonth(1),
                endDate = date.withDayOfMonth(date.lengthOfMonth()),
                limit = currentState.limit,
                offset = currentState.offset
            ).collect { result ->
                when (result) {
                    is TaskResult.Success -> {
                        loadedTasks = result.data
                        setState { copy(isLoading = false) }
                    }

                    is TaskResult.Error -> {
                        setState { copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun loadTasksForCurrentMonth() {
        val state = currentState
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            getExpandedTasksUseCase.invoke(
                startDate = state.currentDate.withDayOfMonth(1),
                endDate = state.currentDate.withDayOfMonth(state.currentDate.lengthOfMonth()),
                limit = state.limit,
                offset = state.offset
            ).collect { result ->
                when (result) {
                    is TaskResult.Success -> {
                        loadedTasks = result.data
                        setState { copy(isLoading = false) }
                    }

                    is TaskResult.Error -> {
                        setState { copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun loadMoreTasks() {
        val state = currentState
        setState { copy(isLoading = true, offset = state.offset + state.limit) }
        viewModelScope.launch {
            getExpandedTasksUseCase.invoke(
                startDate = state.currentDate.withDayOfMonth(1),
                endDate = state.currentDate.withDayOfMonth(state.currentDate.lengthOfMonth()),
                limit = state.limit,
                offset = state.offset + state.limit
            ).collect { result ->
                when (result) {
                    is TaskResult.Success -> {
                        loadedTasks = loadedTasks + result.data
                        setState { copy(isLoading = false) }
                    }

                    is TaskResult.Error -> {
                        setState { copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun reloadTasks() {
        loadTasksForCurrentMonth()
    }

    fun onTaskDeletedOrCompleted() {
        reloadTasks()
    }
}