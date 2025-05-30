package com.elena.autoplanner.presentation.intents


import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.viewmodel.Intent
import java.time.LocalDate

sealed class CalendarIntent : Intent {
    data class ChangeDate(val date: LocalDate, val dismiss: Boolean = false) : CalendarIntent()
    data class ChangeView(val view: CalendarView) : CalendarIntent()
    data class ToggleDatePicker(val show: Boolean) : CalendarIntent()
}