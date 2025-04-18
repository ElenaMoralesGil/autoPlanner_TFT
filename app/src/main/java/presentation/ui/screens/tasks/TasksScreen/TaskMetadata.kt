package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import java.time.LocalTime
import java.util.Locale

@Composable
fun TaskMetadata(task: Task) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        if (task.subtasks.isNotEmpty()) {
            TaskChip(
                icon = painterResource(R.drawable.ic_subtasks),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size}"
            )
        }

        if (task.priority != Priority.NONE) {
            TaskChip(
                icon = painterResource(R.drawable.priority),
                iconTint = when (task.priority) {
                    Priority.HIGH -> MaterialTheme.colorScheme.error
                    Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    Priority.LOW -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                },
                text = task.priority.name.lowercase()
            )
        }

        task.durationConf?.let {
            TaskChip(
                icon = painterResource(R.drawable.ic_duration),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = DateTimeFormatters.formatDurationShort(it)
            )
        }


        if (task.isExpired()) {
            TaskChip(
                icon = painterResource(R.drawable.expired),
                iconTint = MaterialTheme.colorScheme.error,
                text = "Expired"
            )
        }

        task.startDateConf?.let { startTimePlanning ->
            val dateText = buildString {

                append(DateTimeFormatters.formatDateShort(startTimePlanning))

                startTimePlanning.dateTime?.let { dateTime ->
                    if (dateTime.toLocalTime() != LocalTime.MIDNIGHT) {
                        append(" ${DateTimeFormatters.formatTime(dateTime)}")
                    }
                }
                startTimePlanning.dayPeriod?.takeIf { it != DayPeriod.NONE && it != DayPeriod.ALLDAY }
                    ?.let { period ->
                        append(
                            " (${
                            period.name.lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        })"
                        )
                    }

                if (startTimePlanning.dayPeriod == DayPeriod.ALLDAY) {
                    append(" (All day)")
                }

                task.endDateConf?.let { endTimePlanning ->
                    append(" - ")

                    append(DateTimeFormatters.formatDateShort(endTimePlanning))

                    endTimePlanning.dateTime?.let { dateTime ->
                        if (dateTime.toLocalTime() != LocalTime.MIDNIGHT) {
                            append(" ${DateTimeFormatters.formatTime(dateTime)}")
                        }
                    }

                    endTimePlanning.dayPeriod?.takeIf {
                        it != DayPeriod.NONE && it != DayPeriod.ALLDAY && it != startTimePlanning.dayPeriod
                    }?.let { period ->
                        append(
                            " (${
                            period.name.lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        })"
                        )
                    }
                }
            }
            TaskChip(
                icon = painterResource(R.drawable.ic_calendar),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = dateText
            )
        }
    }
}
