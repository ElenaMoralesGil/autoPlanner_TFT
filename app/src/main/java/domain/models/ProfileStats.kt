package com.elena.autoplanner.domain.models

import java.time.LocalDate
import java.time.YearMonth



data class TimeSeriesStat<K : Comparable<K>>(

    val entries: Map<K, Float> = emptyMap(),
)

data class ProfileStats(

    val completedTasksDailyForWeek: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val successRateDailyForWeek: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val totalCompletedWeekly: Int = 0,
    val overallSuccessRateWeekly: Float = 0f,


    val completedTasksWeeklyForMonth: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val successRateWeeklyForMonth: TimeSeriesStat<LocalDate> = TimeSeriesStat(),
    val totalCompletedMonthly: Int = 0,
    val overallSuccessRateMonthly: Float = 0f,


    val completedTasksMonthlyForYear: TimeSeriesStat<YearMonth> = TimeSeriesStat(),
    val successRateMonthlyForYear: TimeSeriesStat<YearMonth> = TimeSeriesStat(),
    val totalCompletedYearly: Int = 0,
    val overallSuccessRateYearly: Float = 0f,
)