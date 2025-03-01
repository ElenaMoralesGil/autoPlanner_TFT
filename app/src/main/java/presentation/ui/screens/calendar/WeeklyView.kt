package com.elena.autoplanner.presentation.ui.screens.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.isToday
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun WeeklyView(
    weekStartDate: LocalDate,
    tasks: List<Task>,
    onTaskSelected: (Task) -> Unit,
    calendarViewModel: CalendarViewModel,
    taskViewModel: TaskViewModel
) {
    val hourHeightDp = 60.dp
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }
    val scrollState = rememberScrollState()
    val currentTime = LocalTime.now()
    val currentMinutes = currentTime.hour * 60 + currentTime.minute

    val weekDays = remember(weekStartDate) {
        val firstDay = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0..6).map { firstDay.plusDays(it.toLong()) }
    }

    val weekTasks = tasks.filter { task ->
        task.startDateConf?.dateTime?.toLocalDate()?.let { date ->
            weekDays.contains(date)
        } ?: false
    }
    val allDayTasks = weekTasks.filter { it.isAllDay() }
    val scheduledTasks = weekTasks.filter { !it.hasPeriod && !it.isAllDay() }
    val morningTasks = weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.MORNING }
    val eveningTasks = weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.EVENING }
    val nightTasks = weekTasks.filter { it.startDateConf?.dayPeriod == DayPeriod.NIGHT }


    LaunchedEffect(weekStartDate) {
        if (weekDays.any { it.isToday() }) {
            val scrollToPosition = (currentMinutes / 60f * hourHeightPx).toInt() - 200
            scrollState.animateScrollTo(scrollToPosition.coerceAtLeast(0))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        WeekNavigationHeader(
            weekStartDate = weekDays.first(),
            weekEndDate = weekDays.last(),
            onPreviousWeek = {
                calendarViewModel.processIntent(
                    CalendarIntent.ChangeDate(weekStartDate.minusWeeks(1))
                )
            },
            onNextWeek = {
                calendarViewModel.processIntent(
                    CalendarIntent.ChangeDate(weekStartDate.plusWeeks(1))
                )
            }
        )

        DaysOfWeekHeader(
            weekDays = weekDays,
            currentDate = LocalDate.now(),
            onDateSelected = { selectedDate ->
                calendarViewModel.processIntent(CalendarIntent.ChangeDate(selectedDate))
            }
        )

        Column(modifier = Modifier.weight(1f)) {
            if (allDayTasks.isNotEmpty()) {
                AllDayTasksSection(
                    allDayTasks = allDayTasks,
                    weekDays = weekDays,
                    onTaskSelected = onTaskSelected
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                TimeGridWithPeriodSections(
                    weekDays = weekDays,
                    scheduledTasks = scheduledTasks,
                    morningTasks = morningTasks,
                    eveningTasks = eveningTasks,
                    nightTasks = nightTasks,
                    hourHeightDp = hourHeightDp,
                    onTaskSelected = onTaskSelected,
                    onTaskTimeChanged = { task, newTime, dayOffset ->
                        val currentDate =
                            task.startDateConf?.dateTime?.toLocalDate() ?: LocalDate.now()
                        val newDate = currentDate.plusDays(dayOffset)

                        taskViewModel.updateTask(
                            task.copy(
                                startDateConf = TimePlanning(
                                    dateTime = LocalDateTime.of(newDate, newTime),
                                    dayPeriod = task.startDateConf?.dayPeriod
                                )
                            )
                        )
                    },
                    scrollState = scrollState,
                    currentTime = currentTime
                )
            }
        }
    }
}

@Composable
private fun WeekNavigationHeader(
    weekStartDate: LocalDate,
    weekEndDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_left),
                contentDescription = "Previous Week"
            )
        }

        Text(
            text = "${weekStartDate.format(DateTimeFormatter.ofPattern("MMM d"))} - ${
                weekEndDate.format(
                    DateTimeFormatter.ofPattern("MMM d, yyyy")
                )
            }",
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(onClick = onNextWeek) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "Next Week"
            )
        }
    }
}

@Composable
private fun DaysOfWeekHeader(
    weekDays: List<LocalDate>,
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)

    ) {
        weekDays.forEach { date ->
            val isToday = date == currentDate
            val backgroundColor = if (isToday)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(backgroundColor, shape = RoundedCornerShape(20.dp))
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEE")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayTasksSection(
    allDayTasks: List<Task>,
    weekDays: List<LocalDate>,
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
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {

                Row(modifier = Modifier.weight(1f)) {
                    weekDays.forEach { date ->
                        val tasksForDay = allDayTasks.filter {
                            it.startDateConf?.dateTime?.toLocalDate() == date
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (tasksForDay.isNotEmpty()) {
                                Column {
                                    tasksForDay.forEach { task ->
                                        TaskItem(
                                            task = task,
                                            onTaskSelected = onTaskSelected,
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .shadow(1.dp, shape = RoundedCornerShape(4.dp))
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
}
@Composable
private fun TimeGridWithPeriodSections(
    weekDays: List<LocalDate>,
    scheduledTasks: List<Task>,
    morningTasks: List<Task>,
    eveningTasks: List<Task>,
    nightTasks: List<Task>,
    hourHeightDp: Dp,
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (Task, LocalTime, Long) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    currentTime: LocalTime
) {
    val hours = (0..23).toList()
    val currentDateIndex = weekDays.indexOfFirst { it.isToday() }
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }

    var draggedTaskState by remember { mutableStateOf<WeeklyTaskDragState?>(null) }

    // Preprocess tasks to create a map of continuous tasks per day
    val continuousTasksMap = weekDays.associateWith { date ->
        scheduledTasks.filter { task ->
            task.startDateConf?.dateTime?.toLocalDate() == date
        }.map { task ->
            // Calculate start and end datetime for each task
            val startDateTime = task.startDateConf?.dateTime ?: LocalDateTime.MIN
            val endDateTime =
                startDateTime.plusMinutes(task.durationConf?.totalMinutes?.toLong() ?: 60L)

            ContinuousTask(
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                tasks = listOf(task)
            )
        }.sortedBy { it.startDateTime }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        // Period sections at appropriate positions
        if (morningTasks.isNotEmpty()) {
            PeriodSection(
                title = "Morning (6AM-12PM)",
                tasks = morningTasks,
                weekDays = weekDays,
                onTaskSelected = onTaskSelected,
            )
        }

        if (eveningTasks.isNotEmpty()) {
            PeriodSection(
                title = "Evening (12PM-6PM)",
                tasks = eveningTasks,
                weekDays = weekDays,
                onTaskSelected = onTaskSelected,
            )
        }

        if (nightTasks.isNotEmpty()) {
            PeriodSection(
                title = "Night (6PM-12AM)",
                tasks = nightTasks,
                weekDays = weekDays,
                onTaskSelected = onTaskSelected,
            )
        }

        // Hours grid with continuous tasks
        Box(modifier = Modifier.fillMaxWidth()) {
            // Hours grid layer
            Column {
                hours.forEach { hour ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeightDp)
                    ) {
                        // Time column
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .fillMaxHeight(),
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
                                modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                            )
                        }

                        // Days row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            weekDays.forEachIndexed { dayIndex, date ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (hour % 2 == 0)
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                                            else Color.Transparent
                                        )
                                ) {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        drawLine(
                                            color = Color.LightGray.copy(alpha = 0.2f),
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = 1f
                                        )
                                        val halfHourY = size.height / 2
                                        drawLine(
                                            color = Color.LightGray.copy(alpha = 0.1f),
                                            start = Offset(0f, halfHourY),
                                            end = Offset(size.width, halfHourY),
                                            strokeWidth = 0.5f,
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(
                                                    5f,
                                                    5f
                                                )
                                            )
                                        )
                                    }

                                    // Current time indicator
                                    if (dayIndex == currentDateIndex && hour == currentTime.hour) {
                                        val minuteOffset =
                                            with(LocalDensity.current) { (currentTime.minute / 60f) * hourHeightDp.toPx() }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .offset(y = with(LocalDensity.current) { minuteOffset.toDp() })
                                                .background(MaterialTheme.colorScheme.primary)
                                                .zIndex(10f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tasks overlay - placed in the same Box to allow spanning across hour boundaries
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24 * hourHeightDp) // Full day height
            ) {
                // Spacing for time column
                Spacer(modifier = Modifier.width(48.dp))

                // Content area for all days
                Row(modifier = Modifier.weight(1f)) {
                    // For each day column
                    weekDays.forEachIndexed { dayIndex, date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clipToBounds()
                        ) {
                            // Render all tasks for this day
                            continuousTasksMap[date]?.forEach { continuousTask ->
                                val startDateTime = continuousTask.startDateTime
                                val endDateTime = continuousTask.endDateTime

                                val startHourMinutes =
                                    startDateTime.hour * 60 + startDateTime.minute
                                val endHourMinutes = endDateTime.hour * 60 + endDateTime.minute

                                // Calculate top position based on start time
                                val topOffset = with(LocalDensity.current) {
                                    (startHourMinutes / 60f * hourHeightDp.toPx()).toDp()
                                }

                                // Calculate height based on duration
                                val durationMinutes = endHourMinutes - startHourMinutes
                                val height = with(LocalDensity.current) {
                                    (durationMinutes / 60f * hourHeightDp.toPx()).toDp()
                                }

                                // Only render if task has positive duration
                                if (durationMinutes > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.95f)
                                            .height(height)
                                            .offset(y = topOffset)
                                    ) {
                                        continuousTask.tasks.firstOrNull()?.let { task ->
                                            TaskBox(
                                                task = task,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .pointerInput(task.id) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                draggedTaskState =
                                                                    WeeklyTaskDragState(
                                                                        task = task,
                                                                        originalTime = task.startTime,
                                                                        originalDayIndex = dayIndex,
                                                                        tempTime = task.startTime,
                                                                        tempDayIndex = dayIndex
                                                                    )
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()

                                                                draggedTaskState?.let { state ->
                                                                    // Handle vertical movement (time change)
                                                                    val minuteDelta =
                                                                        (dragAmount.y / hourHeightPx * 60).roundToInt()
                                                                    if (minuteDelta != 0) {
                                                                        val newMinutes =
                                                                            (state.tempTime.hour * 60 + state.tempTime.minute + minuteDelta)
                                                                                .coerceIn(
                                                                                    0,
                                                                                    24 * 60 - 1
                                                                                )
                                                                        val newHour =
                                                                            newMinutes / 60
                                                                        val newMinute =
                                                                            newMinutes % 60

                                                                        draggedTaskState =
                                                                            state.copy(
                                                                                tempTime = LocalTime.of(
                                                                                    newHour,
                                                                                    newMinute
                                                                                )
                                                                            )
                                                                    }

                                                                    val horizontalThreshold = 20f
                                                                    if (Math.abs(dragAmount.x) > horizontalThreshold) {
                                                                        val direction =
                                                                            if (dragAmount.x > 0) 1 else -1
                                                                        val newDayIndex =
                                                                            (state.tempDayIndex + direction)
                                                                                .coerceIn(
                                                                                    0,
                                                                                    weekDays.size - 1
                                                                                )

                                                                        if (newDayIndex != state.tempDayIndex) {
                                                                            draggedTaskState =
                                                                                state.copy(
                                                                                    tempDayIndex = newDayIndex
                                                                                )
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                draggedTaskState?.let { state ->
                                                                    val dayDiff =
                                                                        state.tempDayIndex - state.originalDayIndex
                                                                    onTaskTimeChanged(
                                                                        task,
                                                                        state.tempTime,
                                                                        dayDiff.toLong()
                                                                    )
                                                                }
                                                                draggedTaskState = null
                                                            },
                                                            onDragCancel = {
                                                                draggedTaskState = null
                                                            }
                                                        )
                                                    },
                                                onTaskSelected = onTaskSelected
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
    }
}

data class ContinuousTask(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val tasks: List<Task>
)

@Composable
private fun PeriodSection(
    title: String,
    tasks: List<Task>,
    weekDays: List<LocalDate>,
    onTaskSelected: (Task) -> Unit,
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
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(48.dp))

                Row(modifier = Modifier.weight(1f)) {
                    weekDays.forEach { date ->
                        val tasksForDay = tasks.filter {
                            it.startDateConf?.dateTime?.toLocalDate() == date
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (tasksForDay.isNotEmpty()) {
                                Column {
                                    tasksForDay.forEach { task ->
                                        PeriodTaskItem(
                                            task = task,
                                            onTaskSelected = onTaskSelected,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp, horizontal = 2.dp)
                                                .shadow(1.dp, shape = RoundedCornerShape(4.dp))
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
}

@Composable
private fun PeriodTaskItem(
    task: Task,
    onTaskSelected: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onTaskSelected(task) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(getPriorityColor(task.priority), CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = task.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (task.isCompleted)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (task.isCompleted) {
            Spacer(modifier = Modifier.width(4.dp))
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
private fun TaskItem(
    task: Task,
    onTaskSelected: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onTaskSelected(task) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(getPriorityColor(task.priority), CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = task.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (task.isCompleted)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (task.isCompleted) {
            Spacer(modifier = Modifier.width(4.dp))
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
private fun TaskBox(
    task: Task,
    modifier: Modifier = Modifier,
    onTaskSelected: (Task) -> Unit
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(4.dp),
                clip = true
            )
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 1f)
            )
            .clickable { onTaskSelected(task) }
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = getPriorityColor(task.priority),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${task.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
                        task.startTime.plusMinutes(task.durationConf?.totalMinutes?.toLong() ?: 60)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    }",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun getPriorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> Color(0xFFEA6C6C)
    Priority.MEDIUM -> Color(0xFFE0A800)
    Priority.LOW -> Color(0xFF5DB258)
    Priority.NONE -> Color.Gray
}

// Data class to track task dragging state
data class WeeklyTaskDragState(
    val task: Task,
    val originalTime: LocalTime,
    val originalDayIndex: Int,
    val tempTime: LocalTime,
    val tempDayIndex: Int
)