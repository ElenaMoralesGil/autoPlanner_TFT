package com.elena.autoplanner.domain.models




enum class FrequencyType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

data class RepeatPlan(
    val frequencyType: FrequencyType = FrequencyType.NONE,
    val interval: Int? = null,
    val intervalUnit: IntervalUnit? = null, // Add this enum
    val selectedDays: Set<DayOfWeek> = emptySet() // Add proper DayOfWeek enum
)
enum class IntervalUnit { DAY, WEEK, MONTH }
enum class DayOfWeek { MON, TUE, WED, THU, FRI,   SAT, SUN }