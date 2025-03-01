package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    var draggedTasks by remember { mutableStateOf(emptyMap<Int, TaskDragState>()) }
    val timeBlocks = listOf(
        TimeBlock("Late Night", 0, 6, null),
        TimeBlock("Morning", 6, 12, morningTasks),
        TimeBlock("Afternoon", 12, 18, eveningTasks),
        TimeBlock("Night", 18, 24, nightTasks)
    )

    fun updateDragState(newState: TaskDragState) {
        draggedTasks = draggedTasks + (newState.task.id to newState)
    }

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
                    draggedTasks = draggedTasks,
                    currentMinutes = currentMinutes,
                    hourHeightPx = hourHeightPx,
                    onTaskSelected = onTaskSelected,
                    onTaskTimeChanged = onTaskTimeChanged,
                    currentTime = currentTime,
                    updateDragState = ::updateDragState
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
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (Task, LocalTime) -> Unit,
    draggedTasks: Map<Int, TaskDragState>,
    updateDragState: (TaskDragState) -> Unit
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
                    .height(hourHeightDp * (if (block.endHour == 24) 6 else block.endHour - block.startHour))
                    .clipToBounds()
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tasksInBlock = scheduledTasks.filter { task ->
                        val effectiveTaskStart =
                            draggedTasks[task.id]?.tempStartTime ?: task.startTime
                        val taskDuration = task.durationConf?.totalMinutes ?: 60
                        val taskEnd = effectiveTaskStart.plusMinutes(taskDuration.toLong())
                        val blockStartTime = LocalTime.of(block.startHour, 0)
                        val blockEndTime = if (block.endHour == 24) LocalTime.MAX else LocalTime.of(
                            block.endHour,
                            0
                        )
                        effectiveTaskStart < blockEndTime && taskEnd > blockStartTime
                    }


                    val modifiedTasks = tasksInBlock.map { task ->
                        draggedTasks[task.id]?.tempStartTime?.let { newTime ->
                            task.copy(
                                startDateConf = task.startDateConf?.copy(
                                    dateTime = LocalDateTime.of(
                                        task.startDateConf!!.dateTime?.toLocalDate(),
                                        newTime
                                    )
                                )
                            )
                        } ?: task
                    }
                    val taskPositions = positionTasks(modifiedTasks)

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
                            parentWidth = maxWidth,
                            onTaskSelected = onTaskSelected,
                            onTaskTimeChanged = onTaskTimeChanged,
                            hourHeightDp = hourHeightDp,
                            draggedTasks = draggedTasks,
                            updateDragState = updateDragState
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
fun TaskBox(
    task: Task,
    position: TaskPosition,
    block: TimeBlock,
    hourHeightPx: Float,
    hourHeightDp: Dp,
    parentWidth: Dp,
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (Task, LocalTime) -> Unit,
    draggedTasks: Map<Int, TaskDragState>,
    updateDragState: (TaskDragState) -> Unit
) {
    val dayStart = LocalTime.MIDNIGHT
    val dayEnd = LocalTime.of(23, 59)
    var localDragOffset by remember { mutableStateOf(0f) }
    val dragOffset by animateFloatAsState(targetValue = localDragOffset)
    var initialTaskStartTime by remember { mutableStateOf(task.startTime) }

    val totalDraggedMinutes = (dragOffset / hourHeightPx * 60).roundToInt()
    val snappedMinutes = (totalDraggedMinutes / 5) * 5L
    val taskDuration = task.durationConf?.totalMinutes ?: 60
    val isShortTask = taskDuration < 50
    val effectiveStartTime = if (dragOffset != 0f) {
        initialTaskStartTime.plusMinutes(snappedMinutes)
            .coerceIn(dayStart, dayEnd.minusMinutes(taskDuration.toLong()))
    } else {
        draggedTasks[task.id]?.tempStartTime ?: task.startTime
    }

    val xOffset = with(LocalDensity.current) { (parentWidth * position.xFraction).toPx() }
    val blockStartTime = LocalTime.of(block.startHour, 0)
    val blockEndTime = if (block.endHour == 24) LocalTime.MAX else LocalTime.of(block.endHour, 0)
    val taskEndTime = effectiveStartTime.plusMinutes(taskDuration.toLong())
    val displayStartTime =
        if (effectiveStartTime.isBefore(blockStartTime)) blockStartTime else effectiveStartTime
    val displayEndTime = if (taskEndTime.isAfter(blockEndTime)) blockEndTime else taskEndTime
    val offsetMinutes =
        java.time.Duration.between(blockStartTime, displayStartTime).toMinutes().toFloat()
    val heightMinutes =
        java.time.Duration.between(displayStartTime, displayEndTime).toMinutes().toFloat()

    val visualY = (offsetMinutes / 60f) * hourHeightPx
    val rectHeightDp = (heightMinutes / 60f) * hourHeightDp.value
    val minVisibleHeight = if (isShortTask) 30.dp else 40.dp
    val adjustedHeight = maxOf(rectHeightDp.dp, minVisibleHeight)

    Box(
        modifier = Modifier
            .fillMaxWidth(position.width)
            .widthIn(min = 120.dp)
            .offset { IntOffset(x = xOffset.roundToInt(), y = visualY.roundToInt()) }
            .zIndex(if (dragOffset != 0f) 1f else 0f)
            .heightIn(min = minVisibleHeight, max = rectHeightDp.dp)
            .height(adjustedHeight)
            .pointerInput(task.id) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        if (localDragOffset == 0f) initialTaskStartTime = task.startTime
                        localDragOffset += dragAmount.y
                        val totalMinutes = (localDragOffset / hourHeightPx * 60).roundToInt()
                        val snappedMinutes = (totalMinutes / 5) * 5L
                        val newTempStartTime = initialTaskStartTime
                            .plusMinutes(snappedMinutes)
                            .coerceIn(dayStart, dayEnd.minusMinutes(taskDuration.toLong()))
                        updateDragState(TaskDragState(task, localDragOffset, newTempStartTime))
                    },
                    onDragEnd = {
                        val totalMinutes = (localDragOffset / hourHeightPx * 60).roundToInt()
                        val snappedMinutes = (totalMinutes / 5) * 5L
                        val newTime = initialTaskStartTime
                            .plusMinutes(snappedMinutes)
                            .coerceIn(dayStart, dayEnd.minusMinutes(taskDuration.toLong()))
                        updateDragState(TaskDragState(task, 0f, newTime))
                        onTaskTimeChanged(task, newTime)
                        localDragOffset = 0f
                    }
                )
            }
            .padding(end = 4.dp, top = 2.dp)
            .shadow(
                elevation = if (dragOffset != 0f) 8.dp else 2.dp,
                shape = RoundedCornerShape(8.dp),
                clip = true
            )
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 1f)
            )
            .clickable { onTaskSelected(task) }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterStart)
                .background(
                    color = if (task.isCompleted) MaterialTheme.colorScheme.primary
                    else getPriorityColor(task.priority),
                    shape = RoundedCornerShape(4.dp)
                )
        )

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxSize()
        ) {
            if (!isShortTask) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough
                            else TextDecoration.None
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (task.isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_completed),
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isShortTask) {
                    Text(
                        text = "${effectiveStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))}-${
                            taskEndTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        }",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.6f
                        )
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = effectiveStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.6f
                        )
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = "to",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.4f
                        )
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = taskEndTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.6f
                        )
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


data class TaskDragState(
    val task: Task,
    val offset: Float = 0f,
    val tempStartTime: LocalTime? = null
)

@Composable
private fun PeriodTasksSection(
    periodName: String,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(1.dp, shape = RoundedCornerShape(8.dp)),

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
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .shadow(1.dp, shape = RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

@Composable
fun AllDayTasksSection(
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(1.dp, shape = RoundedCornerShape(8.dp)),
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
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .shadow(1.dp, shape = RoundedCornerShape(8.dp)),
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
        for (hour in startHour until endHour.coerceAtMost(24)) {
            if (hour >= 24) return@Column
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
        if (endHour == 24) {
            Box(
                modifier = Modifier
                    .height(hourHeightDp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = "12 AM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TaskItem(
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
                    text = "${task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} (${task.durationConf?.totalMinutes ?: 60}min)",
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
        prevTask.startTime.plusMinutes(prevTask.durationConf?.totalMinutes?.toLong() ?: 60)
    return newTask.startTime.isBefore(prevEnd)
}

fun getPriorityColor(priority: Priority): Color = when (priority) {
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
