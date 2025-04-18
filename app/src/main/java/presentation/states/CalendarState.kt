package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import java.time.LocalDate

data class CalendarState(
    val currentDate: LocalDate = LocalDate.now(),
    val currentView: CalendarView = CalendarView.DAY,
    val showDatePicker: Boolean = false,
)
