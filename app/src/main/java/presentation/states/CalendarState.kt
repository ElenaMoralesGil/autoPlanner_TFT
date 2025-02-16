package com.elena.autoplanner.presentation.states

import java.time.LocalDate
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView

data class CalendarState(
    val currentDate: LocalDate = LocalDate.now(),
    val currentView: CalendarView = CalendarView.DAY,
    val showDatePicker: Boolean = false
)
