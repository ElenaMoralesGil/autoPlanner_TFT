package com.elena.autoplanner.domain.models


enum class FrequencyType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM
}
data class RepeatPlan(
    val frequencyType: FrequencyType,
    val interval: Int? = null,
    val selectedWeekdays: List<Int>? = null,
    val dayOfMonth: Int? = null,
    val weekOfMonth: Int? = null,
    val monthOfYear: Int? = null
)