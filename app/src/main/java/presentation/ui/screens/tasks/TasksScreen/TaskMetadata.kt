package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.utils.getFormattedRepeat
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskMetadata(task: Task, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        val dateStringToDisplay = remember(task.startDateConf, task.endDateConf) {
            buildString {
                val startDateTime = task.startDateConf?.dateTime
                val endDateTime = task.endDateConf?.dateTime
                val startPeriod = task.startDateConf?.dayPeriod
                val endPeriod = task.endDateConf?.dayPeriod

                val dateFormatter = DateTimeFormatters.dateFormatter

                val startDateText = startDateTime?.format(dateFormatter)
                val endDateText = endDateTime?.format(dateFormatter)
                val startTimeText = startDateTime?.takeIf { it.toLocalTime() != LocalTime.MIDNIGHT }
                    ?.let { DateTimeFormatters.formatTime(it) }
                val endTimeText = endDateTime?.takeIf { it.toLocalTime() != LocalTime.MIDNIGHT }
                    ?.let { DateTimeFormatters.formatTime(it) }

                val startPeriodDisplay =
                    startPeriod?.takeIf { it != DayPeriod.NONE && it != DayPeriod.ALLDAY }?.let {
                        "(${
                            it.name.lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        })"
                    }
                val allDayDisplay = if (startPeriod == DayPeriod.ALLDAY) " (All day)" else null

                if (startDateText != null) {
                    append(startDateText)
                    if (startTimeText != null) append(" $startTimeText")
                    if (startPeriodDisplay != null) append(" $startPeriodDisplay")
                    if (allDayDisplay != null) append(allDayDisplay)

                    if (endDateText != null && endDateText != startDateText) {
                        append(" - $endDateText")
                        if (endTimeText != null) append(" $endTimeText")
                        val endPeriodDisplay =
                            endPeriod?.takeIf { it != DayPeriod.NONE && it != DayPeriod.ALLDAY && it != startPeriod }
                                ?.let {
                                    "(${
                                        it.name.lowercase().replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                        }
                                    })"
                                }
                        if (endPeriodDisplay != null) append(" $endPeriodDisplay")
                    } else if (endDateText != null && endTimeText != null && endTimeText != startTimeText) {
                        append(" - $endTimeText")
                        val endPeriodDisplay =
                            endPeriod?.takeIf { it != DayPeriod.NONE && it != DayPeriod.ALLDAY && it != startPeriod }
                                ?.let {
                                    "(${
                                        it.name.lowercase().replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                        }
                                    })"
                                }
                        if (endPeriodDisplay != null) append(" $endPeriodDisplay")
                    }
                } else if (endDateText != null) {
                    append("Due: $endDateText")
                    if (endTimeText != null) append(" $endTimeText")
                    val endPeriodDisplay =
                        endPeriod?.takeIf { it != DayPeriod.NONE && it != DayPeriod.ALLDAY }?.let {
                            "(${
                                it.name.lowercase()
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            })"
                        }
                    if (endPeriodDisplay != null) append(" $endPeriodDisplay")
                }
            }.trim()
        }

        if (dateStringToDisplay.isNotEmpty()) {
            TaskChip(
                icon = painterResource(R.drawable.ic_calendar),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = dateStringToDisplay
            )
        }

        task.listName?.let { listName ->
            TaskChip(
                icon = painterResource(R.drawable.ic_lists),
                iconTint = task.listColor ?: MaterialTheme.colorScheme.secondary,
                text = listName
            )
        }

        task.durationConf?.totalMinutes?.takeIf { it > 0 }?.let {
            TaskChip(
                icon = painterResource(R.drawable.ic_duration),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = DateTimeFormatters.formatDurationShort(task.durationConf)
            )
        }

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
                    Priority.NONE -> MaterialTheme.colorScheme.primary
                },
                text = task.priority.name.lowercase()
            )
        }

        if (task.isExpired() && !task.isCompleted) {
            TaskChip(
                icon = painterResource(R.drawable.expired),
                iconTint = MaterialTheme.colorScheme.error,
                text = "Expired"
            )
        }

        task.getFormattedRepeat()?.let { repeatText ->
            TaskChip(
                icon = painterResource(R.drawable.ic_repeat),
                iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                text = repeatText
            )
        }
    }
}