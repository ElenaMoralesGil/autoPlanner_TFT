package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import kotlin.math.roundToInt

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

    val filteredTasks = tasks.filter { it.isDueOn(selectedDate) }
    val allDayTasks = filteredTasks.filter { it.isAllDay() }
    val scheduledTasks = filteredTasks.filter { !it.hasPeriod }
    val periodTasks = filteredTasks.filter { it.hasPeriod && !it.isAllDay() }

    val morningTasks = periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.MORNING }
    val eveningTasks = periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.EVENING }
    val nightTasks = periodTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.NIGHT }

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

        Column(modifier = Modifier.weight(1f)) {
            if (allDayTasks.isNotEmpty()) {
                AllDayTasksSection(allDayTasks, onTaskSelected)
            }

            Box(modifier = Modifier.weight(1f)) {
                EnhancedTimeSchedule(
                    scheduledTasks = scheduledTasks,
                    morningTasks = morningTasks,
                    eveningTasks = eveningTasks,
                    nightTasks = nightTasks,
                    hourHeightDp = hourHeightDp,
                    onTaskSelected = onTaskSelected,
                    onTaskTimeChanged = { task, newTime ->
                        taskViewModel.updateTask(
                            task.copy(
                                startDateConf = TimePlanning(
                                    dateTime = LocalDateTime.of(selectedDate, newTime),
                                    dayPeriod = task.startDateConf?.dayPeriod
                                )
                            )
                        )
                    },
                    isToday = selectedDate.isToday(),
                    currentTime = currentTime,
                    scrollState = scrollState
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
    scrollState: ScrollState
) {
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }
    val currentMinutes = currentTime.hour * 60 + currentTime.minute

    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val timeBlocks = listOf(
        TimeBlock("Late Night", 0, 6, null),
        TimeBlock("Morning", 6, 12, morningTasks),
        TimeBlock("Afternoon", 12, 18, eveningTasks),
        TimeBlock("Night", 18, 24, nightTasks)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            timeBlocks.forEach { block ->
                TimeBlockSection(
                    block = block,
                    hourHeightDp = hourHeightDp,
                    scheduledTasks = scheduledTasks,
                    isToday = isToday,
                    currentMinutes = currentMinutes,
                    hourHeightPx = hourHeightPx,
                    draggedTaskId = draggedTaskId,
                    dragOffsetY = dragOffsetY,
                    onTaskSelected = onTaskSelected,
                    onTaskTimeChanged = onTaskTimeChanged,
                    onDragStart = { draggedTaskId = it },
                    onDragEnd = { draggedTaskId = null; dragOffsetY = 0f },
                    onDrag = { dragOffsetY += it },
                    currentTime = currentTime
                )
            }
        }
    }
}

@Composable
private fun TimeBlockSection(
    currentTime: LocalTime,
    block: TimeBlock,
    hourHeightDp: Dp,
    scheduledTasks: List<Task>,
    isToday: Boolean,
    currentMinutes: Int,
    hourHeightPx: Float,
    draggedTaskId: String?,
    dragOffsetY: Float,
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (Task, LocalTime) -> Unit,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit
) {
    Column {
        block.periodTasks?.takeIf { it.isNotEmpty() }?.let {
            PeriodTasksSection(block.name, it, onTaskSelected)
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TimeLabelsForBlock(block.startHour, block.endHour, hourHeightDp)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(hourHeightDp * (block.endHour - block.startHour))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tasksInBlock = scheduledTasks.filter { task ->
                        task.startTime.hour in block.startHour until block.endHour
                    }
                    val taskPositions = positionTasks(tasksInBlock)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color.White)
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

                    taskPositions.forEach { (task, position) ->
                        TaskBox(
                            task = task,
                            position = position,
                            block = block,
                            hourHeightPx = hourHeightPx,
                            isDragging = task.id.toString() == draggedTaskId,
                            dragOffsetY = dragOffsetY,
                            parentWidth = maxWidth,
                            onTaskSelected = onTaskSelected,
                            onTaskTimeChanged = onTaskTimeChanged,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDrag = onDrag
                        )
                    }

                    if (isToday && currentTime.hour in block.startHour until block.endHour) {
                        CurrentTimeIndicator(
                            block = block,
                            currentMinutes = currentMinutes,
                            hourHeightPx = hourHeightPx,
                            currentTime = currentTime,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskBox(
    task: Task,
    position: TaskPosition,
    block: TimeBlock,
    hourHeightPx: Float,
    isDragging: Boolean,
    dragOffsetY: Float,
    parentWidth: Dp,
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (Task, LocalTime) -> Unit,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit
) {
    val startMinutes = task.startTime.toMinutes()
    val blockStart = block.startHour * 60
    val yPosition = (startMinutes - blockStart) / 60f * hourHeightPx
    val duration = task.durationConf?.totalMinutes ?: 30
    val taskHeight = (duration / 60f) * hourHeightPx

    val xOffset = with(LocalDensity.current) { (parentWidth * position.xFraction).toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth(position.width)
            .offset {
                IntOffset(
                    x = xOffset.roundToInt(),
                    y = (yPosition + dragOffsetY).toInt()
                )
            }
            .height(taskHeight.dp)
            .padding(end = 2.dp, top = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isDragging) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragStart = { onDragStart(task.id.toString()) },
                    onDragEnd = { onDragEnd() },
                    onDrag = { _, dragAmount ->
                        val maxY =
                            hourHeightPx * (block.endHour - block.startHour) - yPosition - taskHeight
                        val minY = -yPosition
                        val newOffset = (dragOffsetY + dragAmount.y).coerceIn(minY, maxY)
                        onDrag(newOffset)
                    }
                )
            }
            .clickable { onTaskSelected(task) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                task.priority.takeIf { it != Priority.NONE }?.let {
                    PriorityIndicator(it)
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
                    text = "${task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} (${task.durationConf?.totalMinutes ?: 30}min)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodTasksSection(
    periodName: String,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "$periodName Tasks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            tasks.forEach { task ->
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

@Composable
private fun AllDayTasksSection(
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
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
            tasks.forEach { task ->
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

@Composable
private fun CurrentTimeIndicator(
    block: TimeBlock,
    currentTime: LocalTime,
    currentMinutes: Int,
    hourHeightPx: Float
) {
    val blockStartMinutes = block.startHour * 60
    val relativeCurrentMinutes = currentMinutes - blockStartMinutes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (relativeCurrentMinutes / 60f * hourHeightPx).dp)
            .height(2.dp)
            .background(MaterialTheme.colorScheme.primary)
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

@Composable
private fun TimeLabelsForBlock(
    startHour: Int,
    endHour: Int,
    hourHeightDp: Dp
) {
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
            PriorityIndicator(task.priority)
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
            CompletedIcon()
        }
    }
}

@Composable
private fun PriorityIndicator(priority: Priority) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(getPriorityColor(priority)),
    )
}

@Composable
private fun CompletedIcon() {
    Icon(
        painter = painterResource(id = R.drawable.ic_completed),
        contentDescription = "Completed",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
    )
}

private fun positionTasks(tasks: List<Task>): Map<Task, TaskPosition> {
    val columns = mutableListOf<MutableList<Task>>()

    tasks.sortedBy { it.startTime }.forEach { task ->
        val targetColumn = columns.firstOrNull { column ->
            column.lastOrNull()?.let { lastTask ->
                !taskOverlaps(lastTask, task)
            } ?: true
        } ?: run {
            mutableListOf<Task>().also { columns.add(it) }
        }
        targetColumn.add(task)
    }

    val columnWidth = 1f / columns.size.coerceAtLeast(1)
    return columns.flatMapIndexed { colIndex, columnTasks ->
        columnTasks.map { task ->
            task to TaskPosition(
                xFraction = colIndex * columnWidth,
                width = columnWidth
            )
        }
    }.toMap()
}

private fun taskOverlaps(prevTask: Task, newTask: Task): Boolean {
    val prevEnd =
        prevTask.startTime.plusMinutes(prevTask.durationConf?.totalMinutes?.toLong() ?: 30)
    return newTask.startTime.isBefore(prevEnd)
}

private fun getPriorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> Color.Red
    Priority.MEDIUM -> Color(0xFFFFA500)
    Priority.LOW -> Color(0xFF4CAF50)
    Priority.NONE -> Color.Gray
}

data class TimeBlock(
    val name: String,
    val startHour: Int,
    val endHour: Int,
    val periodTasks: List<Task>?
)

data class TaskPosition(val xFraction: Float, val width: Float)
