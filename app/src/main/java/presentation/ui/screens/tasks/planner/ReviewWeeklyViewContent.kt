package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun ReviewWeeklyViewContent(
    startDate: LocalDate,
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    conflicts: List<ConflictItem>,
    resolutions: Map<Int, ResolutionOption>,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val weekDays = remember(startDate) {

        val monday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0..6).map { monday.plusDays(it.toLong()) }
    }
    val hourHeight: Dp = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = remember(hourHeight, density) { with(density) { hourHeight.toPx() } }
    val timeLabelWidth: Dp = 48.dp
    val totalGridHeight = remember(hourHeight) { hourHeight * 24 }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    (screenWidthDp - timeLabelWidth)
    val dayWidth: Dp = 47.dp
    val conflictedTasksToShowMap = remember(conflicts, resolutions, weekDays) {
        weekDays.associateWith { date ->
            conflicts.flatMap { conflict ->
                val conflictResolved = resolutions[conflict.hashCode()]?.let {
                    it == ResolutionOption.MOVE_TO_TOMORROW
                } == true
                if (conflictResolved) emptyList()
                else {
                    conflict.conflictingTasks.filter { task ->
                        val taskResolution = resolutions[task.id]
                        val showTask = taskResolution == null ||
                                taskResolution == ResolutionOption.LEAVE_IT_LIKE_THAT ||
                                taskResolution == ResolutionOption.MANUALLY_SCHEDULE
                        showTask && (conflict.conflictTime?.toLocalDate() == date || task.startDateConf.dateTime?.toLocalDate() == date)
                    }
                }
            }.distinctBy { it.id }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {

            Spacer(modifier = Modifier.width(timeLabelWidth))

            Row(modifier = Modifier.weight(1f)) {
                weekDays.forEach { day ->
                    Column(
                        modifier = Modifier
                            .width(dayWidth)
                            .padding(
                                vertical = 6.dp,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isToday = day == LocalDate.now()
                        Text(
                            day.format(DateTimeFormatter.ofPattern("E")),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Box(
            modifier = Modifier
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .height(totalGridHeight)
                    .fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .width(timeLabelWidth)
                        .fillMaxHeight()
                        .padding(start = 6.dp)
                ) {
                    for (hour in 0..23) {
                        val timeString = when (hour) {
                            0 -> "12AM"; 12 -> "12PM"; in 1..11 -> "${hour}AM"; else -> "${hour - 12}PM"
                        }
                        Box(
                            modifier = Modifier.height(hourHeight),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = timeString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val color1 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 4.dp)
                ) {

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dayWidthPx = with(density) { dayWidth.toPx() }
                        val gridColor = color
                        val gridColorHalf = color1

                        for (hour in 0..23) {
                            val y = hour * hourHeightPx
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )

                            val halfY = y + hourHeightPx / 2
                            drawLine(
                                color = gridColorHalf,
                                start = Offset(0f, halfY),
                                end = Offset(size.width, halfY),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                            )
                        }

                        for (i in 1 until weekDays.size) {
                            val x = i * dayWidthPx
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        weekDays.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .width(dayWidth)
                                    .fillMaxHeight()
                            ) {

                                val scheduledItems = plan[date] ?: emptyList()
                                val conflictedTasks = conflictedTasksToShowMap[date] ?: emptyList()
                                val scheduledTaskIds = scheduledItems.map { it.task.id }.toSet()
                                val allItemsToRender = scheduledItems + conflictedTasks
                                    .filterNot { scheduledTaskIds.contains(it.id) }
                                    .map { task -> 
                                        val startTime =
                                            conflicts.find { c -> c.conflictingTasks.any { t -> t.id == task.id } }?.conflictTime?.toLocalTime()
                                                ?: task.startTime
                                        val endTime =
                                            startTime.plusMinutes(task.effectiveDurationMinutes.toLong())
                                        ScheduledTaskItem(
                                            task,
                                            startTime,
                                            endTime,
                                            task.startDateConf.dateTime?.toLocalDate() ?: date
                                        )
                                    }

                                allItemsToRender.sortedBy { it.scheduledStartTime }
                                    .forEach { item ->

                                        val startMinutes =
                                            item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                                        val endMinutes =
                                            item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                                        val durationMinutes =
                                            (endMinutes - startMinutes).coerceAtLeast(15)
                                        val topOffset = (startMinutes / 60f) * hourHeight
                                        val itemHeight = (durationMinutes / 60f) * hourHeight
                                        val isConflicted =
                                            conflictedTasks.any { it.id == item.task.id }

                                        ReviewTaskCard(
                                            task = item.task,
                                            startTime = item.scheduledStartTime,
                                            endTime = item.scheduledEndTime,
                                            isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(
                                                item.task.id
                                            ),
                                            isConflicted = isConflicted,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 1.dp)
                                                .height(itemHeight.coerceAtLeast(24.dp))
                                                .offset(y = topOffset),
                                            onClick = { onTaskClick(item.task) }
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}