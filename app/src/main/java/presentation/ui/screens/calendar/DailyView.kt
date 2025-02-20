package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.isToday
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.ui.utils.WeekHeader
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DailyView(
    selectedDate: LocalDate,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    calendarViewModel: CalendarViewModel,
    taskViewModel: TaskViewModel,
) {
    val hourHeightDp = 60.dp
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }
    val scrollState = rememberScrollState()
    val currentTime = LocalTime.now()
    val currentMinutes = currentTime.hour * 60 + currentTime.minute

    // Sort tasks into categories
    val allDayTasks = tasks.filter { it.isAllDay() }
    val scheduledTasks = tasks.filter { !it.isAllDay() && !it.hasPeriod }
    val periodTasks = tasks.filter { it.hasPeriod && !it.isAllDay() }

    // Separate period tasks by their period type
    val morningTasks = periodTasks.filter { it.getDayPeriod() == DayPeriod.MORNING }
    val eveningTasks = periodTasks.filter { it.getDayPeriod() == DayPeriod.EVENING }
    val nightTasks = periodTasks.filter { it.getDayPeriod() == DayPeriod.NIGHT }

    // Non-scheduled tasks - tasks without specific time or period
    val nonScheduledTasks = tasks.filter {
        !it.isAllDay() && !it.hasPeriod &&
                (it.startTime == LocalTime.MIDNIGHT || it.startTime == null)
    }

    LaunchedEffect(selectedDate) {
        if (selectedDate.isToday()) {
            // Scroll to current time with offset for better visibility
            val scrollToPosition = (currentMinutes / 60f * hourHeightPx).toInt() - 200
            scrollState.animateScrollTo(scrollToPosition.coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week day selector
        WeekHeader(
            selectedDate = selectedDate,
            onDateSelected = { calendarViewModel.processIntent(CalendarIntent.ChangeDate(it)) },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Main content area with tasks
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // All-day section with card style
            if (allDayTasks.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "All Day",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        allDayTasks.forEach { task ->
                            TaskItem(
                                task = task,
                                onTaskSelected = onTaskSelected,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Non-scheduled tasks section (tasks without time that can be dragged to schedule)
            if (nonScheduledTasks.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Unscheduled Tasks",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(nonScheduledTasks) { task ->
                                DraggableTaskItem(
                                    task = task,
                                    onTaskSelected = onTaskSelected,
                                    onTaskScheduled = { updatedTask, time ->
                                        val taskWithTime = updatedTask.copy(
                                            startDateConf = TimePlanning(
                                                dateTime = LocalDateTime.of(selectedDate, time),
                                                dayPeriod = updatedTask.startDateConf?.dayPeriod
                                            )
                                        )
                                        taskViewModel.updateTask(taskWithTime)
                                    },
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Time schedule with integrated period sections
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                EnhancedTimeSchedule(
                    scheduledTasks = scheduledTasks,
                    morningTasks = morningTasks,
                    eveningTasks = eveningTasks,
                    nightTasks = nightTasks,
                    hourHeightDp = hourHeightDp,
                    onTaskSelected = onTaskSelected,
                    onTaskTimeChanged = { task, newStartTime ->
                        val updatedTask = task.copy(
                            startDateConf = TimePlanning(
                                dateTime = LocalDateTime.of(selectedDate, newStartTime),
                                dayPeriod = task.startDateConf?.dayPeriod
                            )
                        )
                        taskViewModel.updateTask(updatedTask)
                    },
                    isToday = selectedDate.isToday(),
                    currentTime = currentTime,
                    scrollState = scrollState,
                    selectedDate = selectedDate
                )
            }
        }
    }
}

@Composable
private fun EnhancedTimeSchedule(
    scheduledTasks: List<Task>,
    morningTasks: List<Task>,
    eveningTasks: List<Task>,
    nightTasks: List<Task>,
    hourHeightDp: Dp,
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (Task, LocalTime) -> Unit,
    isToday: Boolean,
    currentTime: LocalTime,
    scrollState: ScrollState,
    selectedDate: LocalDate
) {
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }
    val currentMinutes = currentTime.hour * 60 + currentTime.minute
    val density = LocalDensity.current

    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Corrected time blocks with 0-6 coverage
    val timeBlocks = listOf(
        TimeBlock("Late Night", 0, 6, null),
        TimeBlock("Morning Hours", 6, 12, morningTasks),
        TimeBlock("Afternoon Hours", 12, 18, eveningTasks),
        TimeBlock("Night Hours", 18, 24, nightTasks)
    )


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {


            // Time blocks with integrated sections
            timeBlocks.forEach { block ->
                // Time block header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Render period-specific tasks if they exist for this block
                if (block.periodTasks != null && block.periodTasks.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "${block.name} Tasks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            block.periodTasks.forEach { task ->
                                TaskItem(
                                    task = task,
                                    onTaskSelected = onTaskSelected,
                                    color = when {
                                        block.name.contains("Morning") -> getPeriodColor(DayPeriod.MORNING)
                                        block.name.contains("Evening") -> getPeriodColor(DayPeriod.EVENING)
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Hourly schedule for this block
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Time labels column
                    TimeLabelsForBlock(
                        startHour = block.startHour,
                        endHour = block.endHour,
                        hourHeightDp = hourHeightDp
                    )

                    // Schedule content for this block
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(hourHeightDp * (block.endHour - block.startHour))
                    ) {
                        // Draw hour grid lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw background for this time block
                            drawRect(
                                color = when {
                                    block.name.contains("Early Morning") -> Color(0xFFF5F5F5)
                                    block.name.contains("Morning") -> Color(0xFFE3F2FD).copy(alpha = 0.3f)
                                    block.name.contains("Afternoon") -> Color(0xFFFFF8E1).copy(alpha = 0.3f)
                                    block.name.contains("Evening") -> Color(0xFFE8EAF6).copy(alpha = 0.3f)
                                    else -> Color.White
                                },
                                size = size
                            )

                            // Draw hour grid lines
                            for (hour in block.startHour until block.endHour) {
                                val yPos = (hour - block.startHour) * hourHeightPx
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.2f),
                                    start = Offset(0f, yPos),
                                    end = Offset(size.width, yPos),
                                    strokeWidth = 1f
                                )

                                // Add half-hour lines
                                val halfHourYPos = yPos + (hourHeightPx / 2)
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.1f),
                                    start = Offset(0f, halfHourYPos),
                                    end = Offset(size.width, halfHourYPos),
                                    strokeWidth = 0.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                                )
                            }
                        }

                        // Filter and position tasks that belong to this time block
                        val tasksInThisBlock = scheduledTasks.filter { task ->
                            val taskHour = task.startTime.hour
                            taskHour >= block.startHour && taskHour < block.endHour
                        }

                        val blockTaskPositions = positionTasks(tasksInThisBlock)

                        // Render scheduled tasks for this block
                        blockTaskPositions.forEach { (task, position) ->
                            val startMinutes = task.startTime.toMinutes()
                            val blockStartMinutes = block.startHour * 60
                            val relativePosition = startMinutes - blockStartMinutes
                            val duration = task.durationConf?.totalMinutes ?: 60
                            val taskHeight = (duration / 60f) * hourHeightPx
                            val isDragging = task.id == draggedTaskId?.toInt()

                            val taskModifier = if (isDragging) {
                                Modifier
                                    .fillMaxWidth(position.width)
                                    .offset {
                                        IntOffset(
                                            x = (position.x.dp.toPx()).toInt(),
                                            y = (relativePosition / 60f * hourHeightPx + dragOffsetY).toInt()
                                        )
                                    }
                            } else {
                                Modifier
                                    .fillMaxWidth(position.width)
                                    .offset(
                                        x = position.x.dp,
                                        y = (relativePosition / 60f * hourHeightPx).dp
                                    )
                            }

                            Box(
                                modifier = taskModifier
                                    .height(taskHeight.dp.coerceAtMost(hourHeightDp * (block.endHour - block.startHour)))
                                    .padding(end = 2.dp, top = 1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isDragging) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.secondaryContainer
                                    )
                                    .pointerInput(task.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedTaskId = task.id.toString()
                                            },
                                            onDragEnd = {
                                                if (draggedTaskId != null) {
                                                    val newPositionY =
                                                        relativePosition / 60f * hourHeightPx + dragOffsetY
                                                    val newBlockRelativeHour =
                                                        (newPositionY / hourHeightPx).toInt()
                                                    val newHour =
                                                        (block.startHour + newBlockRelativeHour).coerceIn(
                                                            0,
                                                            23
                                                        )
                                                    val newMinute =
                                                        ((newPositionY % hourHeightPx) / hourHeightPx * 60)
                                                            .toInt()
                                                            .coerceIn(0, 59)

                                                    // Snap to 15-minute intervals
                                                    val snappedMinute = (newMinute / 15) * 15
                                                    val snappedTime =
                                                        LocalTime.of(newHour, snappedMinute)

                                                    onTaskTimeChanged(task, snappedTime)
                                                    draggedTaskId = null
                                                    dragOffsetY = 0f
                                                }
                                            },
                                            onDragCancel = {
                                                draggedTaskId = null
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                            }
                                        )
                                    }
                                    .clickable { onTaskSelected(task) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (task.priority != Priority.NONE) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(getPriorityColor(task.priority))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        Text(
                                            text = task.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (task.isCompleted) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_completed),
                                                contentDescription = "Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    if (taskHeight > 40) {
                                        Text(
                                            text = buildString {
                                                append(
                                                    task.startTime.format(
                                                        DateTimeFormatter.ofPattern(
                                                            "HH:mm"
                                                        )
                                                    )
                                                )
                                                if (task.durationConf != null) {
                                                    append(" (${task.durationConf.totalMinutes}min)")
                                                } else if (task.endTime != LocalTime.MIDNIGHT) {
                                                    append(
                                                        " - ${
                                                            task.endTime.format(
                                                                DateTimeFormatter.ofPattern("HH:mm")
                                                            )
                                                        }"
                                                    )
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Current time indicator (only if today and time is within this block)
                        if (isToday && currentTime.hour >= block.startHour && currentTime.hour < block.endHour) {
                            val blockStartMinutes = block.startHour * 60
                            val relativeCurrentMinutes = currentMinutes - blockStartMinutes

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = (relativeCurrentMinutes / 60f * hourHeightPx).dp)
                                    .height(2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Time dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )

                                    // Time line
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(2.dp)
                                            .background(MaterialTheme.colorScheme.primary)
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

// New data class for time blocks
data class TimeBlock(
    val name: String,
    val startHour: Int,
    val endHour: Int,
    val periodTasks: List<Task>?
)

@Composable
private fun TimeLabelsForBlock(startHour: Int, endHour: Int, hourHeightDp: Dp) {
    Column(modifier = Modifier.width(48.dp)) {
        for (hour in startHour until endHour) {
            Box(
                modifier = Modifier
                    .height(hourHeightDp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = when (hour) {
                        0 -> "12 AM"
                        in 1..11 -> "$hour AM"
                        12 -> "12 PM"
                        else -> "${hour - 12} PM"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DraggableTaskItem(
    task: Task,
    onTaskSelected: (Task) -> Unit,
    onTaskScheduled: (Task, LocalTime) -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // Handle dragging state
                    isDragging = true
                },
                onDragStarted = {
                    isDragging = true
                },
                onDragStopped = { velocity ->
                    isDragging = false
                    // Calculate drop position when dragging ends
                    // This is simplified - you'll need to calculate the actual time based on y position
                }
            )
            .clickable { onTaskSelected(task) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            if (task.priority != Priority.NONE) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getPriorityColor(task.priority))
                        .align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.durationConf != null) {
                Text(
                    text = "${task.durationConf.totalMinutes}min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.isCompleted) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_completed),
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.End)
                )
            }

            if (isDragging) {
                Text(
                    text = "Drag to schedule",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PeriodSection(
    title: String,
    tasks: List<Task>,
    position: Dp,
    color: Color,
    onTaskSelected: (Task) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = position)
            .padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.3f))
                .padding(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            tasks.forEach { task ->
                TaskItem(
                    task = task,
                    onTaskSelected = onTaskSelected,
                    color = color.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}


@Composable
private fun TaskItem(
    task: Task,
    onTaskSelected: (Task) -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .clickable { onTaskSelected(task) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority indicator
        if (task.priority != Priority.NONE) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(getPriorityColor(task.priority))
                    .padding(end = 8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Task content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Show time for tasks with specific times
            if (!task.isAllDay() && task.startTime != LocalTime.MIDNIGHT) {
                Text(
                    text = buildString {
                        append(task.startTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                        if (task.durationConf != null) {
                            append(" (${task.durationConf.totalMinutes}min)")
                        } else if (task.endTime != LocalTime.MIDNIGHT) {
                            append(" - ${task.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Completion indicator
        if (task.isCompleted) {
            Icon(
                painter = painterResource(id = R.drawable.ic_completed),
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TimeLabels(hourHeightDp: Dp) {
    Column(modifier = Modifier.width(48.dp)) {
        (0..23).forEach { hour ->
            Box(
                modifier = Modifier
                    .height(hourHeightDp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = when (hour) {
                        0 -> "12 AM"
                        in 1..11 -> "$hour AM"
                        12 -> "12 PM"
                        else -> "${hour - 12} PM"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

// Helper functions

// Efficiently position tasks to avoid overlaps
private fun positionTasks(tasks: List<Task>): Map<Task, TaskPosition> {
    val sortedTasks = tasks.sortedBy { it.startTime }
    val result = mutableMapOf<Task, TaskPosition>()
    val columns = mutableListOf<MutableList<Task>>()

    sortedTasks.forEach { task ->
        // Find a column where this task doesn't overlap with existing tasks
        var placed = false
        for (column in columns) {
            val lastTask = column.last()
            val lastTaskEndTime = if (lastTask.durationConf != null) {
                val minutes =
                    lastTask.startTime.toMinutes() + (lastTask.durationConf.totalMinutes ?: 60)
                LocalTime.of(minutes / 60, minutes % 60)
            } else {
                lastTask.endTime
            }

            if (task.startTime.isAfter(lastTaskEndTime) || task.startTime == lastTaskEndTime) {
                column.add(task)
                placed = true
                break
            }
        }

        // If no suitable column found, create a new one
        if (!placed) {
            columns.add(mutableListOf(task))
        }
    }

    // Calculate positions based on columns
    val columnWidth = 1f / columns.size.coerceAtLeast(1)
    columns.forEachIndexed { colIndex, column ->
        column.forEach { task ->
            result[task] = TaskPosition(
                x = colIndex * columnWidth * 100,
                width = columnWidth
            )
        }
    }

    return result
}

data class TaskPosition(val x: Float, val width: Float)

// Helper function to get color for priority indicators
private fun getPriorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> Color.Red
    Priority.MEDIUM -> Color(0xFFFFA500) // Orange
    Priority.LOW -> Color(0xFF4CAF50) // Green
    Priority.NONE -> Color.Gray
}

// Helper function to get color for different periods
private fun getPeriodColor(period: DayPeriod): Color = when (period) {
    DayPeriod.MORNING -> Color(0xFFE3F2FD) // Light blue
    DayPeriod.EVENING -> Color(0xFFFFF9C4) // Light yellow
    DayPeriod.NIGHT -> Color(0xFFE1BEE7) // Light purple
    DayPeriod.ALLDAY -> Color(0xFFE8F5E9) // Light green
    DayPeriod.NONE -> Color.White
}

private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}
