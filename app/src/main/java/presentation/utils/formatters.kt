package com.elena.autoplanner.presentation.utils

import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.Task
import java.time.format.DateTimeFormatter

fun Task.getFormattedReminder(): String? {
    return reminderPlan?.let { reminder ->
        when (reminder.mode) {
            ReminderMode.NONE -> null
            ReminderMode.PRESET_OFFSET -> reminder.offsetMinutes?.let {
                when {
                    it == 0 -> "At time of event"
                    it < 60 -> "$it minutes before"
                    it == 60 -> "1 hour before"
                    it % 60 == 0 -> "${it / 60} hours before"
                    else -> "${it / 60}h ${it % 60}m before"
                }
            }

            ReminderMode.EXACT -> reminder.exactDateTime?.let {
                "At ${DateTimeFormatter.ofPattern("h:mm a, MMM d").format(it)}"
            }

            ReminderMode.CUSTOM -> "Custom reminder"
        }
    }
}

fun Task.getFormattedRepeat(): String? {
    return repeatPlan?.let { repeat ->
        when (repeat.frequencyType) {
            FrequencyType.NONE -> null
            FrequencyType.DAILY -> "Daily"
            FrequencyType.WEEKLY -> {
                if (repeat.selectedDays.isEmpty()) "Weekly"
                else "Weekly on " + repeat.selectedDays.joinToString(", ") {
                    it.name.substring(0, 3)
                }
            }

            FrequencyType.MONTHLY -> "Monthly"
            FrequencyType.YEARLY -> "Yearly"
            FrequencyType.CUSTOM -> repeat.interval?.let { interval ->
                "Every $interval ${repeat.intervalUnit?.name?.lowercase()}${if (interval > 1) "s" else ""}"
            } ?: "Custom"
        }
    }
}