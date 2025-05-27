package com.elena.autoplanner.domain.models

import java.time.LocalDate


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
    val intervalUnit: IntervalUnit? = null,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val repeatEndDate: LocalDate? = null,
    val repeatOccurrences: Int? = null,

    val daysOfMonth: List<Int> = emptyList(),
    val monthsOfYear: List<Int> = emptyList(),
    val ordinalsOfWeekdays: List<OrdinalWeekday> = emptyList(),
    val setPos: List<Int> = emptyList(), 
) {
    init {
        require(interval == null || interval > 0) { "Interval must be positive" }
        require(repeatEndDate == null || repeatOccurrences == null) { "Cannot set both repeatEndDate and repeatOccurrences" }
        require(repeatOccurrences == null || repeatOccurrences > 0) { "Repeat occurrences must be positive" }
    }
}

enum class IntervalUnit { DAY, WEEK, MONTH, YEAR }
enum class DayOfWeek { MON, TUE, WED, THU, FRI, SAT, SUN }
data class OrdinalWeekday(
    val ordinal: Int, 
    val dayOfWeek: DayOfWeek,
)