package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import androidx.compose.foundation.* // Import foundation ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times // Keep this import
import androidx.compose.ui.zIndex
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.isToday
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun TimeGridWithPeriodSections(
    weekDays: List<LocalDate>,
    scheduledTasks: List<Task>,
    morningTasks: List<Task>,
    eveningTasks: List<Task>,
    nightTasks: List<Task>,   // Tasks for the night period section
    hourHeightDp: Dp,
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (task: Task, newTime: LocalTime, dayOffset: Long) -> Unit, // Callback when drag ends
    scrollState: ScrollState, // Use androidx.compose.foundation.ScrollState
    currentTime: LocalTime, // Current time for the indicator line
) {
    val hours = (0..23).toList()
    val currentDateIndex = weekDays.indexOfFirst { it.isToday() }
    val density = LocalDensity.current
    val hourHeightPx = remember(density, hourHeightDp) { with(density) { hourHeightDp.toPx() } }

    // State for the currently dragged task and grid layout info
    var dayWidthPx by remember { mutableStateOf(0f) }
    var gridStartTimeAreaWidthPx by remember { mutableStateOf(0f) } // Width of the time label area
    var draggedTaskState by remember { mutableStateOf<WeeklyTaskDragState?>(null) }
    var taskAreaCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) } // Coordinates of the grid content area

    // Helper to calculate task vertical position (offset) and height in Dp
    @Composable
    fun rememberTaskMetrics(task: Task): Pair<Dp, Dp> {
        val startDateTime = task.startDateConf?.dateTime ?: LocalDateTime.MIN
        val effectiveDuration = task.effectiveDurationMinutes.coerceAtLeast(1)
        // Ensure end time is at least slightly after start time for calculation
        val endDateTime = startDateTime.plusMinutes(
            (task.durationConf?.totalMinutes?.toLong() ?: 15L).coerceAtLeast(1L)
        )

        val startTotalMinutes = startDateTime.hour * 60 + startDateTime.minute
        val endTotalMinutes = endDateTime.hour * 60 + endDateTime.minute

        val topOffsetDp = with(density) { (startTotalMinutes / 60f * hourHeightPx).toDp() }

        // Calculate duration, ensuring a minimum visual height (e.g., for 15 mins)
        val durationMinutes =
            (endTotalMinutes - startTotalMinutes).coerceAtLeast(15) // Ensure min 15 min visually?
        val heightDp = with(density) {
            (effectiveDuration / 60f * hourHeightPx)
                .toDp()
                .coerceAtLeast(15.dp)
        }
        return Pair(topOffsetDp, heightDp)
    }

    // Group tasks by date for efficient rendering
    val tasksByDate = remember(scheduledTasks, weekDays) {
        weekDays.associateWith { date ->
            scheduledTasks.filter { task ->
                task.startDateConf?.dateTime?.toLocalDate() == date
            }.sortedBy { it.startDateConf?.dateTime }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState) // Apply vertical scroll
    ) {
        // --- Period Sections (Rendered above the main grid) ---
        PeriodSection(
            title = "Morning (6AM-12PM)",
            tasks = morningTasks,
            weekDays = weekDays,
            onTaskSelected = onTaskSelected,
        )
        PeriodSection(
            title = "Afternoon (12PM-6PM)", // Changed title for clarity
            tasks = eveningTasks, // Assuming eveningTasks covers afternoon
            weekDays = weekDays,
            onTaskSelected = onTaskSelected,
        )
        PeriodSection(
            title = "Evening & Night (6PM-6AM)", // Changed title for clarity
            tasks = nightTasks, // Assuming nightTasks covers this range
            weekDays = weekDays,
            onTaskSelected = onTaskSelected,
        )
        val color1 = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        val color2 = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

        Spacer(Modifier.height(8.dp)) // Spacing before the grid

        // --- Main Time Grid Container ---
        Box(modifier = Modifier.fillMaxWidth()) {

            // --- Layer 1: Background Grid and Time Labels ---
            Column {
                hours.forEach { hour ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeightDp) // Fixed height for each hour row
                    ) {
                        // Time Label Box (Left Side)
                        Box(
                            modifier = Modifier
                                .width(48.dp) // Fixed width for consistency
                                .fillMaxHeight()
                                .onGloballyPositioned {
                                    if (gridStartTimeAreaWidthPx == 0f) gridStartTimeAreaWidthPx =
                                        it.size.width.toFloat()
                                },
                            contentAlignment = Alignment.TopCenter // Center time label vertically
                        ) {
                            Text(
                                text = when (hour) {
                                    0 -> "12 AM"
                                    12 -> "12 PM"
                                    in 1..11 -> "$hour AM"
                                    else -> "${hour - 12} PM"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                // Apply offset for visual adjustment, NOT negative padding
                                modifier = Modifier.offset(y = (-4).dp)
                            )
                        }

                        // Grid Content Area (Right Side)
                        Row(
                            modifier = Modifier
                                .weight(1f) // Take remaining width
                                .fillMaxHeight()
                                .onGloballyPositioned {
                                    // Calculate dayWidthPx once layout is known
                                    if (dayWidthPx == 0f && it.size.width > 0 && weekDays.isNotEmpty()) {
                                        dayWidthPx = it.size.width.toFloat() / weekDays.size
                                    }
                                    if (taskAreaCoordinates == null) {
                                        taskAreaCoordinates =
                                            it // Store coordinates of the grid content area itself
                                    }
                                }

                        ) {
                            weekDays.forEachIndexed { dayIndex, _ ->
                                Box( // Represents one hour slot for a specific day
                                    modifier = Modifier
                                        .weight(1f) // Equal width for each day column
                                        .fillMaxHeight()
                                )

                                {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        // Horizontal line at the top of the hour
                                        drawLine(
                                            color = color1,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = 1f
                                        )
                                        // Dashed line at half hour (optional)
                                        val halfHourY = size.height / 2
                                        drawLine(
                                            color = color2,
                                            start = Offset(0f, halfHourY),
                                            end = Offset(size.width, halfHourY),
                                            strokeWidth = 1f,
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(
                                                    4f,
                                                    4f
                                                )
                                            )
                                        )
                                        // Vertical line separating days (draw on the left edge)
                                        if (dayIndex > 0) {
                                            drawLine(
                                                color = color1,
                                                start = Offset(0f, 0f),
                                                end = Offset(0f, size.height),
                                                strokeWidth = 1f
                                            )
                                        }
                                    }

                                    // Current Time Indicator Line
                                    if (dayIndex == currentDateIndex && hour == currentTime.hour && dayWidthPx > 0) {
                                        val minuteRatio = currentTime.minute / 60f
                                        val indicatorY = minuteRatio * hourHeightPx

                                        Box( // Use a Box for thickness
                                            modifier = Modifier
                                                .padding(start = 0.dp) // Align with the start of the day column
                                                .width(with(density) { dayWidthPx.toDp() })
                                                .height(2.dp)
                                                .offset(y = with(density) { indicatorY.toDp() })
                                                .background(MaterialTheme.colorScheme.primary)
                                                .zIndex(5f) // Above grid lines, below tasks
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } // End Background Grid Column

            // --- Layer 2: Task Rendering Area ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24 * hourHeightDp) // Must match total grid height
                    .padding(start = 48.dp) // Offset start to align with grid content area
                    .onGloballyPositioned {
                        if (taskAreaCoordinates == null) taskAreaCoordinates = it
                    }
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDays.forEachIndexed { dayIndex, date ->
                        Box( // Column for tasks on a specific day
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clipToBounds() // Clip tasks overflowing this day's column bounds
                        ) {
                            tasksByDate[date]?.forEach { task ->
                                val (topOffsetDp, heightDp) = rememberTaskMetrics(task)
                                val isBeingDragged = draggedTaskState?.task?.id == task.id
                                val initialYPx = with(density) { topOffsetDp.toPx() }

                                val taskModifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .height(heightDp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = topOffsetDp)
                                    .zIndex(if (isBeingDragged) 20f else 10f)
                                    .then(
                                        if (isBeingDragged) {
                                            Modifier.offset {
                                                IntOffset(
                                                    x = draggedTaskState?.dragOffsetPx?.x?.roundToInt()
                                                        ?: 0,
                                                    y = draggedTaskState?.dragOffsetPx?.y?.roundToInt()
                                                        ?: 0
                                                )
                                            }
                                        } else Modifier
                                    )
                                    .pointerInput(
                                        task,
                                        weekDays,
                                        dayWidthPx,
                                        hourHeightPx,
                                        taskAreaCoordinates
                                    ) {
                                        if (dayWidthPx <= 0f || hourHeightPx <= 0f || taskAreaCoordinates == null) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { touchOffsetInTaskBox ->
                                                task.startDateConf?.dateTime?.let { originalDateTime ->
                                                    draggedTaskState = WeeklyTaskDragState(
                                                        task = task,
                                                        originalDateTime = originalDateTime,
                                                        targetDate = originalDateTime.toLocalDate(),
                                                        targetTime = originalDateTime.toLocalTime(),
                                                        dragOffsetPx = Offset.Zero
                                                    )
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                draggedTaskState?.let { currentDrag ->
                                                    val newVisualOffset =
                                                        currentDrag.dragOffsetPx + dragAmount
                                                    val taskHeightPx =
                                                        with(density) { heightDp.toPx() }
                                                    val currentTaskCenterPx = Offset(
                                                        x = (dayIndex * dayWidthPx) + (dayWidthPx / 2f) + newVisualOffset.x,
                                                        y = initialYPx + (taskHeightPx / 2f) + newVisualOffset.y
                                                    )
                                                    val targetDayIndex =
                                                        (currentTaskCenterPx.x / dayWidthPx)
                                                            .toInt()
                                                            .coerceIn(0, weekDays.size - 1)
                                                    val targetDate = weekDays[targetDayIndex]
                                                    val totalMinutesFromTop =
                                                        (currentTaskCenterPx.y / hourHeightPx) * 60f
                                                    val snappedMinutes =
                                                        ((totalMinutesFromTop / 15.0).roundToInt() * 15)
                                                    val maxStartMinute =
                                                        1440 - (task.durationConf?.totalMinutes
                                                            ?: 15)
                                                    val clampedMinutes =
                                                        snappedMinutes.coerceIn(0, maxStartMinute)
                                                    val targetHour =
                                                        (clampedMinutes / 60).coerceIn(0, 23)
                                                    val targetMinute = clampedMinutes % 60
                                                    val targetTime =
                                                        LocalTime.of(targetHour, targetMinute)
                                                    draggedTaskState = currentDrag.copy(
                                                        targetDate = targetDate,
                                                        targetTime = targetTime,
                                                        dragOffsetPx = newVisualOffset
                                                    )
                                                }
                                            },
                                            onDragEnd = {
                                                draggedTaskState?.let { finalState ->
                                                    val originalDate =
                                                        finalState.originalDateTime.toLocalDate()
                                                    val dayDifference = ChronoUnit.DAYS.between(
                                                        originalDate,
                                                        finalState.targetDate
                                                    )
                                                    if (finalState.targetTime != finalState.originalDateTime.toLocalTime() || dayDifference != 0L) {
                                                        onTaskTimeChanged(
                                                            finalState.task,
                                                            finalState.targetTime,
                                                            dayDifference
                                                        )
                                                    }
                                                }
                                                draggedTaskState = null
                                            },
                                            onDragCancel = { draggedTaskState = null }
                                        )
                                    }

                                TaskBox(
                                    task = task,
                                    modifier = taskModifier,
                                    onTaskSelected = {
                                        if (draggedTaskState == null) onTaskSelected(it)
                                    }
                                )
                            }
                        }
                    }
                }
            } // End Task Rendering Area Row (Layer 2)

            // --- Layer 3: Ghost / Placeholder for Dragging ---
            draggedTaskState?.let { currentDrag ->
                val ghostDayIndex = weekDays.indexOf(currentDrag.targetDate)
                // Ensure dayWidthPx is positive before using it in calculations
                if (ghostDayIndex != -1 && dayWidthPx > 0f) {
                    val (_, originalHeightDp) = rememberTaskMetrics(task = currentDrag.task)
                    val ghostTargetMinutes =
                        currentDrag.targetTime.hour * 60 + currentDrag.targetTime.minute
                    val ghostTargetYPx = (ghostTargetMinutes / 60f) * hourHeightPx
                    val ghostTargetXpx = (ghostDayIndex * dayWidthPx)

                    Box(
                        modifier = Modifier
                            .padding(start = 48.dp)
                            .offset {
                                IntOffset(
                                    x = ghostTargetXpx.roundToInt(),
                                    y = ghostTargetYPx.roundToInt()
                                )
                            }
                            // Apply width safely
                            .width(
                                if (dayWidthPx > 0f) {
                                    with(density) { dayWidthPx.toDp() } * 0.95f
                                } else {
                                    0.dp // Fallback
                                }
                            )
                            .height(originalHeightDp)
                            // Apply padding safely
                            .padding(
                                horizontal = if (dayWidthPx > 0f) {
                                    with(density) { (dayWidthPx * 0.025f).toDp() }
                                } else {
                                    0.dp // Fallback
                                }
                            )
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                            .zIndex(15f)
                    ) {
                        Text(
                            currentDrag.targetTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } // End Ghost Layer (Layer 3)

        } // End Main Time Grid Box
    } // End Main Column (Scrollable)
}