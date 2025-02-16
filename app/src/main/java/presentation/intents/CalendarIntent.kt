package com.elena.autoplanner.presentation.intents


import java.time.LocalDate
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView

sealed class CalendarIntent : BaseIntent() {
    data class ChangeDate(val date: LocalDate) : CalendarIntent()
    data class ChangeView(val view: CalendarView) : CalendarIntent()
    data class ToggleDatePicker(val show: Boolean) : CalendarIntent()
}