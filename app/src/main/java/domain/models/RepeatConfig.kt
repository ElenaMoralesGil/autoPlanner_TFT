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
    val interval: Int? = null, // e.g., repeat every 2 weeks
    val intervalUnit: IntervalUnit? = null, // DAY, WEEK, MONTH, YEAR
    val selectedDays: Set<com.elena.autoplanner.domain.models.DayOfWeek> = emptySet(), // For WEEKLY repetition
    val repeatEndDate: LocalDate? = null, // Optional end date for the recurrence
    val repeatOccurrences: Int? = null, // Optional number of times to repeat
    // Advanced RRULE fields (optional, add if needed)
    val daysOfMonth: List<Int> = emptyList(), // e.g., [1, 15] for 1st and 15th of the month
    val monthsOfYear: List<Int> = emptyList(), // e.g., [1, 7] for Jan and July
    val ordinalsOfWeekdays: List<OrdinalWeekday> = emptyList(), // e.g., [OrdinalWeekday(1, DayOfWeek.SUNDAY)] for 1st Sunday
    val setPos: List<Int> = emptyList(), // e.g., [-1] for the last occurrence matching other rules in the period
) {
    init {
        require(interval == null || interval > 0) { "Interval must be positive" }
        require(repeatEndDate == null || repeatOccurrences == null) { "Cannot set both repeatEndDate and repeatOccurrences" }
        require(repeatOccurrences == null || repeatOccurrences > 0) { "Repeat occurrences must be positive" }
    }
}

enum class IntervalUnit { DAY, WEEK, MONTH, YEAR }
enum class DayOfWeek { MON, TUE, WED, THU, FRI,   SAT, SUN }
data class OrdinalWeekday(
    val ordinal: Int, // e.g., 1 for 1st, 2 for 2nd, -1 for last
    val dayOfWeek: com.elena.autoplanner.domain.models.DayOfWeek,
)