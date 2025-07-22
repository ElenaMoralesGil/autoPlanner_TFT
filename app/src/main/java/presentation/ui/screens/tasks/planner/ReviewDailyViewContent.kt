package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import java.time.LocalDate

@Composable
fun ReviewDailyViewContent(
    date: LocalDate,
    items: List<ScheduledTaskItem>,
    conflicts: List<ConflictItem>,
    resolutions: Map<Int, ResolutionOption>,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val hourHeight = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    hourHeight * 24
    val timeLabelWidth: Dp = 48.dp
    val scrollState = rememberScrollState()
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val color1 = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

    val conflictedTasksToShow = remember(conflicts, resolutions, date) {
        conflicts.flatMap { conflict ->

            val conflictResolved = resolutions[conflict.hashCode()]?.let {
                it == ResolutionOption.MOVE_TO_TOMORROW
            } == true

            if (conflictResolved) {
                emptyList()
            } else {
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
    val allItemsToRender = remember(items, conflictedTasksToShow) {
        val scheduledTaskIds = items.map { it.task.id }.toSet()

        items + conflictedTasksToShow
            .filterNot { scheduledTaskIds.contains(it.id) }
            .map { task ->

                val startTime =
                    conflicts.find { c -> c.conflictingTasks.any { t -> t.id == task.id } }?.conflictTime?.toLocalTime()
                        ?: task.startTime
                val endTime = startTime.plusMinutes(task.effectiveDurationMinutes.toLong())
                ScheduledTaskItem(
                    task,
                    startTime,
                    endTime,
                    task.startDateConf.dateTime?.toLocalDate() ?: date
                )
            }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val labelWidthPx = with(density) { timeLabelWidth.toPx() }
            val gridColor = color
            val gridColorHalf = color1

            for (hour in 0..23) {
                val y = hour * hourHeightPx
                drawLine(
                    color = gridColor,
                    start = Offset(labelWidthPx, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )

                val halfY = y + hourHeightPx / 2
                drawLine(
                    color = gridColorHalf,
                    start = Offset(labelWidthPx, halfY),
                    end = Offset(size.width, halfY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            drawLine(
                color = gridColor,
                start = Offset(labelWidthPx, 0f),
                end = Offset(labelWidthPx, size.height),
                strokeWidth = 1f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(timeLabelWidth)
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
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = timeLabelWidth, end = 4.dp)
        ) {

            allItemsToRender.sortedBy { it.scheduledStartTime }.forEach { item ->
                val startMinutes =
                    item.scheduledStartTime.hour * 60 + item.scheduledStartTime.minute
                val endMinutes = item.scheduledEndTime.hour * 60 + item.scheduledEndTime.minute
                val durationMinutes = (endMinutes - startMinutes).coerceAtLeast(15)
                val topOffset = (startMinutes / 60f) * hourHeight
                val itemHeight = (durationMinutes / 60f) * hourHeight

                val isConflicted = conflictedTasksToShow.any { it.id == item.task.id }

                ReviewTaskCard(
                    task = item.task,
                    startTime = item.scheduledStartTime,
                    endTime = item.scheduledEndTime,
                    isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(item.task.id),
                    isConflicted = isConflicted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 0.5.dp)
                        .height(itemHeight.coerceAtLeast(24.dp))
                        .offset(y = topOffset),
                    onClick = { onTaskClick(item.task) }
                )
            }
        }
    }
}