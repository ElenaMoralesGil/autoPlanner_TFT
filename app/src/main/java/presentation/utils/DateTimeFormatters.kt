package com.elena.autoplanner.presentation.utils

import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.TimePlanning
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeFormatters {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatDateTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")
        return dateTime.format(formatter)
    }

    fun formatTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return dateTime.format(formatter)
    }

    // In DateTimeFormatters.kt
    fun formatDateShort(dateTime: TimePlanning): String {
        return dateTime.dateTime?.format(dateFormatter) ?: ""
    }

    fun formatDurationShort(duration: DurationPlan?): String {
        return duration?.totalMinutes?.let { mins ->
            when {
                mins >= 1440 -> "${mins/1440}d"
                mins >= 60 -> "${mins/60}h ${mins%60}m"
                else -> "${mins}m"
            }
        } ?: ""
    }


    fun formatDateTimeWithPeriod(timePlanning: TimePlanning?): String {
        return timePlanning?.let {
            val period = when (it.dayPeriod) {
                DayPeriod.MORNING -> "Morning"
                DayPeriod.EVENING -> "Evening"
                DayPeriod.NIGHT -> "Night"
                DayPeriod.ALLDAY -> "All day"
                else -> null
            }

            val formattedDateTime = if (period != null) {
                it.dateTime?.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            } else {
                it.dateTime?.let { datetime -> formatDateTime(datetime) }
            }

            if (period != null) "$formattedDateTime • $period" else formattedDateTime
        } ?: "None"
    }

    fun formatDurationForDisplay(duration: DurationPlan?): String {
        return duration?.totalMinutes?.let { mins ->
            when {
                mins % 60 == 0 -> "${mins/60}H"
                else -> "${mins}min"
            }
        } ?: "None"
    }

    fun formatReminderForDisplay(reminder: ReminderPlan?): String {
        return when (reminder?.mode) {
            ReminderMode.PRESET_OFFSET -> reminder.offsetMinutes?.let {
                when {
                    it >= 1440 -> "${it/1440} day${if(it/1440 > 1) "s" else ""} before"
                    it >= 60 -> "${it/60} hour${if(it/60 > 1) "s" else ""} before"
                    else -> "$it min before"
                }
            } ?: "At start time"
            ReminderMode.EXACT -> "At ${reminder.exactDateTime?.let { formatTime(it) }}"
            ReminderMode.CUSTOM -> {
                val daysPart = if (reminder.customDayOffset != null) {
                    when (reminder.customDayOffset) {
                        0 -> "Same day"
                        else -> "${reminder.customDayOffset} day${if(reminder.customDayOffset > 1) "s" else ""} before"
                    }
                } else if (reminder.customWeekOffset != null) {
                    "${reminder.customWeekOffset} week${if(reminder.customWeekOffset > 1) "s" else ""} before"
                } else ""

                val timePart = reminder.customHour?.let { hour ->
                    reminder.customMinute?.let { minute ->
                        String.format(" at %02d:%02d", hour, minute)
                    }
                } ?: ""

                "$daysPart$timePart"
            }
            else -> "None"
        }
    }

    fun formatRepeatForDisplay(repeat: RepeatPlan?): String {
        return repeat?.let {
            when (it.frequencyType) {
                FrequencyType.DAILY -> "Daily"
                FrequencyType.WEEKLY -> "Weekly" +
                        if (it.selectedDays.isNotEmpty()) " on ${
                            it.selectedDays.joinToString(", ") { day ->
                                day.name.take(3)
                            }
                        }" else ""

                FrequencyType.MONTHLY -> "Monthly"
                FrequencyType.YEARLY -> "Yearly"
                FrequencyType.CUSTOM -> it.interval?.let { interval ->
                    "Every $interval ${it.intervalUnit?.name?.lowercase()}${if(interval > 1) "s" else ""}"
                } ?: "Custom"

                else -> "None"
            }
        } ?: "None"
    }
}
