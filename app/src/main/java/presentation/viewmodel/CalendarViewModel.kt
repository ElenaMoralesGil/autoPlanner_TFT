package com.elena.autoplanner.presentation.viewmodel


import com.elena.autoplanner.presentation.effects.CalendarEffect
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.states.CalendarState
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.ui.utils.getWeekDays
import com.elena.autoplanner.presentation.utils.BaseViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

class CalendarViewModel : BaseViewModel<CalendarIntent, CalendarState, CalendarEffect>() {

    override fun createInitialState(): CalendarState = CalendarState()

    override suspend fun handleIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.ChangeDate -> {
                val newDate = intent.date
                val currentView = currentState.currentView

                val adjustedDate = when (currentView) {
                    CalendarView.WEEK -> newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    else -> newDate
                }

                setState {
                    copy(
                        currentDate = adjustedDate,
                        showDatePicker = if (intent.dismiss) false else showDatePicker
                    )
                }
            }

            is CalendarIntent.ChangeView -> setState { copy(currentView = intent.view) }
            is CalendarIntent.ToggleDatePicker -> setState { copy(showDatePicker = intent.show) }
        }
    }

    fun getWeekDays(date: LocalDate): List<LocalDate> {
        return date.getWeekDays()
    }

    fun getCalendarGrid(date: LocalDate): List<CalendarCell> {
        val calendarGrid = mutableListOf<CalendarCell>()
        val firstDayOfMonth = date.withDayOfMonth(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
        val daysInMonth = date.lengthOfMonth()

        val previousMonth = firstDayOfMonth.minusDays((firstDayOfWeek - 1).toLong())
        repeat(firstDayOfWeek - 1) {
            calendarGrid.add(CalendarCell(previousMonth.plusDays(it.toLong()), false))
        }

        repeat(daysInMonth) {
            calendarGrid.add(CalendarCell(firstDayOfMonth.plusDays(it.toLong()), true))
        }
        val remainingDays = 42 - calendarGrid.size
        repeat(remainingDays) {
            calendarGrid.add(
                CalendarCell(firstDayOfMonth.plusDays(daysInMonth + it.toLong()), false)
            )
        }
        return calendarGrid
    }

    fun generateTimeSlots(): List<LocalTime> {
        return (0..23).map { LocalTime.of(it, 0) } // 12 AM to 11 PM
    }
}

data class CalendarCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)