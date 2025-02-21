package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
    println("Current time: $currentTime")
    println("All Tasks: $tasks")
    val tasks = tasks.filter { it.isDueOn(selectedDate) }
    val allDayTasks = tasks.filter { it.isAllDay() }
    val scheduledTasks = tasks.filter { !it.hasPeriod }
    println("Scheduled tasks: $scheduledTasks")

    val periodTasks = tasks.filter { it.hasPeriod && !it.isAllDay() }


    val morningTasks = periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.MORNING }
    println("Morning tasks: $morningTasks")
    val eveningTasks = periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.EVENING }
    println("Evening tasks: $eveningTasks")
    val nightTasks = periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.NIGHT }
    println("Night tasks: $nightTasks")


    LaunchedEffect(selectedDate) {
        if (selectedDate.isToday()) {
            val scrollToPosition = (currentMinutes / 60f * hourHeightPx).toInt() - 200
            scrollState.animateScrollTo(scrollToPosition.coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WeekHeader(
            selectedDate = selectedDate,
            onDateSelected = { calendarViewModel.processIntent(CalendarIntent.ChangeDate(it)) },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
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

    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Time blocks for display
    val timeBlocks = listOf(
        TimeBlock("Late Night", 0, 6, null),
        TimeBlock("Morning", 6, 12, morningTasks),
        TimeBlock("Evening", 12, 18, eveningTasks),
        TimeBlock("Night", 18, 24, nightTasks)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            timeBlocks.forEach { block ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!block.periodTasks.isNullOrEmpty()) {
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
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Time labels column
                    TimeLabelsForBlock(
                        startHour = block.startHour,
                        endHour = block.endHour,
                        hourHeightDp = hourHeightDp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(hourHeightDp * (block.endHour - block.startHour))
                    ) {
                        var colorScheme = MaterialTheme.colorScheme.surface
                        // Draw background grid for the block
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                color = colorScheme,
                                size = size
                            )

                            for (hour in block.startHour until block.endHour) {
                                val yPos = (hour - block.startHour) * hourHeightPx
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.2f),
                                    start = Offset(0f, yPos),
                                    end = Offset(size.width, yPos),
                                    strokeWidth = 1f
                                )
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

                        // Filter tasks that belong to this block based on their start time
                        val tasksInThisBlock = scheduledTasks.filter { task ->
                            // This will filter tasks based on the hour in their startDateConf
                            val taskHour = task.startTime.hour
                            taskHour in block.startHour until block.endHour
                        }

                        // Use column positioning to avoid overlaps (if desired)
                        val blockTaskPositions = positionTasks(tasksInThisBlock)

                        blockTaskPositions.forEach { (task, position) ->

                            // Calculate vertical offset: task appears at its start time
                            val startMinutes = task.startTime.toMinutes()
                            val blockStartMinutes = block.startHour * 60
                            val relativePosition = startMinutes - blockStartMinutes
                            // Use duration from durationConf or default to 30 minutes
                            val duration = task.durationConf?.totalMinutes ?: 30
                            val taskHeight = (duration / 60f) * hourHeightPx
                            val isDragging = task.id == draggedTaskId?.toInt()
                            println("Drawing task '${task.name}' at offset = $relativePosition, block = ${block.name} with a height of $taskHeight")
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
                                        // Show start time and duration (ignore end time)
                                        Text(
                                            text = buildString {
                                                append(
                                                    task.startTime.format(
                                                        DateTimeFormatter.ofPattern(
                                                            "HH:mm"
                                                        )
                                                    )
                                                )
                                                append(" (${task.durationConf?.totalMinutes ?: 30}min)")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (isToday && currentTime.hour >= block.startHour && currentTime.hour < block.endHour) {
                            val blockStartMinutes = block.startHour * 60
                            val relativeCurrentMinutes = currentMinutes - blockStartMinutes
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = (relativeCurrentMinutes / 60f * hourHeightPx).dp)
                                    .height(2.dp)
                                    .background(Color.Yellow.copy(alpha = 0.4f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!task.isAllDay() && task.startTime != LocalTime.MIDNIGHT) {
                Text(
                    text = "${task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} (${task.durationConf?.totalMinutes ?: 30}min)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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

private fun positionTasks(tasks: List<Task>): Map<Task, TaskPosition> {
    val sortedTasks = tasks.sortedBy { it.startTime }
    val result = mutableMapOf<Task, TaskPosition>()
    val columns = mutableListOf<MutableList<Task>>()

    sortedTasks.forEach { task ->
        var placed = false
        for (column in columns) {
            val lastTask = column.last()
            // Calculate last task’s end time using its start time plus its duration (default 30 min)
            val lastTaskEndTime = lastTask.startTime.plusMinutes(
                ((lastTask.durationConf?.totalMinutes ?: 30).toLong())
            )
            if (task.startTime >= lastTaskEndTime) {
                column.add(task)
                placed = true
                break
            }
        }
        if (!placed) {
            columns.add(mutableListOf(task))
        }
    }

    // Calculate horizontal position based on the column index
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

