package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect
import java.time.LocalDate

sealed class CalendarEffect : UiEffect {

    data class ShowSnackbar(val message: String) : CalendarEffect()
    data class NavigateTo(val date: LocalDate) : CalendarEffect()
    data class ShowLoading(val isLoading: Boolean) : CalendarEffect()
    data class Error(val message: String) : CalendarEffect()
    data class Success(val message: String) : CalendarEffect()
    data class SwitchView(val viewType: String) : CalendarEffect()
}