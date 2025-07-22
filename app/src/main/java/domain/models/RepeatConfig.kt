package com.elena.autoplanner.domain.models

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

// Enums existentes en el sistema (compatibles con RepeatConfigEntity)
enum class FrequencyType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

enum class IntervalUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

enum class DayOfWeek {
    MON, TUE, WED, THU, FRI, SAT, SUN
}

// Nuevos enums para funcionalidad extendida
enum class RepeatFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class MonthlyRepeatType {
    BY_DAY, // Día específico del mes (ej: día 15)
    BY_WEEKDAY // Día específico de la semana (ej: segundo martes)
}

enum class WeekdayOrdinal {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    LAST
}

data class OrdinalWeekday(
    val ordinal: Int,
    val dayOfWeek: DayOfWeek,
)

// RepeatPlan híbrido compatible con el sistema existente
data class RepeatPlan(
    // Compatibilidad con sistema existente (RepeatConfigEntity)
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

    // Sistema NUEVO (para tareas repetibles con instancias pre-generadas)
    val isEnabled: Boolean = false,
    val frequency: RepeatFrequency = RepeatFrequency.DAILY,
    val intervalNew: Int = 1,
    val monthlyRepeatType: MonthlyRepeatType = MonthlyRepeatType.BY_DAY,
    val dayOfMonth: Int? = null,
    val weekdayOrdinal: WeekdayOrdinal? = null,
    val dayOfWeekNew: DayOfWeek? = null,
    val monthOfYear: Int? = null,
    val endDate: LocalDateTime? = null,
    val maxOccurrences: Int? = null,
    val skipWeekends: Boolean = false,
    val skipHolidays: Boolean = false,
) {
    init {
        require(intervalNew > 0) { "Interval must be positive" }
        require(interval == null || interval > 0) { "Legacy interval must be positive" }
        require(repeatOccurrences == null || repeatOccurrences > 0) { "Repeat occurrences must be positive" }
        require(maxOccurrences == null || maxOccurrences > 0) { "Max occurrences must be positive" }
        require(dayOfMonth == null || dayOfMonth in 1..31) { "Day of month must be between 1 and 31" }
        require(monthOfYear == null || monthOfYear in 1..12) { "Month of year must be between 1 and 12" }

        // No permitir configuraciones conflictivas
        require(
            (repeatEndDate == null && repeatOccurrences == null) ||
                    (endDate == null && maxOccurrences == null)
        ) { "Cannot mix old and new end date/occurrence configurations" }
    }

    // Funciones de conveniencia para compatibilidad
    fun isRepeatEnabled(): Boolean = isEnabled || frequencyType != FrequencyType.NONE

    fun getEffectiveFrequency(): RepeatFrequency {
        return when {
            isEnabled -> frequency
            frequencyType != FrequencyType.NONE -> when (frequencyType) {
                FrequencyType.DAILY -> RepeatFrequency.DAILY
                FrequencyType.WEEKLY -> RepeatFrequency.WEEKLY
                FrequencyType.MONTHLY -> RepeatFrequency.MONTHLY
                FrequencyType.YEARLY -> RepeatFrequency.YEARLY
                else -> RepeatFrequency.DAILY
            }

            else -> RepeatFrequency.DAILY
        }
    }
}
