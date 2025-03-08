package com.elena.autoplanner.presentation.viewmodel


import com.elena.autoplanner.presentation.effects.CalendarEffect
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.states.CalendarState
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import java.time.DayOfWeek
import java.time.LocalDate

import java.time.temporal.TemporalAdjusters

class CalendarViewModel : BaseViewModel<CalendarIntent, CalendarState, CalendarEffect>() {

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
    }

}