package com.elena.autoplanner.domain.models

data class ProfileStats(
    val completedTasksWeekly: Int = 0,
    val completedTasksMonthly: Int = 0,
    val completedTasksYearly: Int = 0,
    val successRateWeekly: Float = 0f,
    val successRateMonthly: Float = 0f,
    val successRateYearly: Float = 0f,

    )
