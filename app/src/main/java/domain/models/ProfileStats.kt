package com.elena.autoplanner.domain.models

import java.time.LocalDate
import java.time.YearMonth


// Holds time-series data for charts
data class TimeSeriesStat<K : Comparable<K>>(
    // K is the time key (LocalDate, YearMonth, etc.)
    val entries: Map<K, Float> = emptyMap(),
)

data class ProfileStats(
    // Weekly View Data (Daily granularity)
    val completedTasksDailyForWeek: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val successRateDailyForWeek: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val totalCompletedWeekly: Int = 0, // Keep total for easy display if needed
    val overallSuccessRateWeekly: Float = 0f, // Keep total for easy display if needed

    // Monthly View Data (Weekly granularity - Key is the start date of the week)
    val completedTasksWeeklyForMonth: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val successRateWeeklyForMonth: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val totalCompletedMonthly: Int = 0,
    val overallSuccessRateMonthly: Float = 0f,

    // Yearly View Data (Monthly granularity)
    val completedTasksMonthlyForYear: TimeSeriesStat<YearMonth> = TimeSeriesStat(),
    val successRateMonthlyForYear: TimeSeriesStat<YearMonth> = TimeSeriesStat(),
    val totalCompletedYearly: Int = 0,
    val overallSuccessRateYearly: Float = 0f,
)