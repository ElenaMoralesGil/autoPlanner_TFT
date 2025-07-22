package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
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
    nightTasks: List<Task>,   
    hourHeightDp: Dp,
    onTaskSelected: (Task) -> Unit,
    onTaskTimeChanged: (task: Task, newTime: LocalTime, dayOffset: Long) -> Unit,
    scrollState: ScrollState,
    currentTime: LocalTime, 
) {
    val hours = (0..23).toList()
    val currentDateIndex = weekDays.indexOfFirst { it.isToday() }
    val density = LocalDensity.current
    val hourHeightPx = remember(density, hourHeightDp) { with(density) { hourHeightDp.toPx() } }

    var dayWidthPx by remember { mutableStateOf(0f) }
    var gridStartTimeAreaWidthPx by remember { mutableStateOf(0f) } 
    var draggedTaskState by remember { mutableStateOf<WeeklyTaskDragState?>(null) }
    var taskAreaCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    @Composable
    fun rememberTaskMetrics(task: Task): Pair<Dp, Dp> {
        val startDateTime = task.startDateConf?.dateTime ?: LocalDateTime.MIN
        val effectiveDuration = task.effectiveDurationMinutes.coerceAtLeast(1)

        val endDateTime = startDateTime.plusMinutes(
            (task.durationConf?.totalMinutes?.toLong() ?: 15L).coerceAtLeast(1L)
        )

        val startTotalMinutes = startDateTime.hour * 60 + startDateTime.minute
        val endTotalMinutes = endDateTime.hour * 60 + endDateTime.minute

        val topOffsetDp = with(density) { (startTotalMinutes / 60f * hourHeightPx).toDp() }

        (endTotalMinutes - startTotalMinutes).coerceAtLeast(15) 
        val heightDp = with(density) {
            (effectiveDuration / 60f * hourHeightPx)
                .toDp()
                .coerceAtLeast(15.dp)
        }
        return Pair(topOffsetDp, heightDp)
    }

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
            .verticalScroll(scrollState) 
    ) {

        PeriodSection(
            title = "Morning (6AM-12PM)",
            tasks = morningTasks,
            weekDays = weekDays,
            onTaskSelected = onTaskSelected,
        )
        PeriodSection(
            title = "Afternoon (12PM-6PM)",
            tasks = eveningTasks, 
            weekDays = weekDays,
            onTaskSelected = onTaskSelected,
        )
        PeriodSection(
            title = "Evening & Night (6PM-6AM)",
            tasks = nightTasks, 
            weekDays = weekDays,
            onTaskSelected = onTaskSelected,
        )
        val color1 = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        val color2 = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {

            Column {
                hours.forEach { hour ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeightDp) 
                    ) {

                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .fillMaxHeight()
                                .onGloballyPositioned {
                                    if (gridStartTimeAreaWidthPx == 0f) gridStartTimeAreaWidthPx =
                                        it.size.width.toFloat()
                                },
                            contentAlignment = Alignment.TopCenter 
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

                                modifier = Modifier.offset(y = (-4).dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned {

                                    if (dayWidthPx == 0f && it.size.width > 0 && weekDays.isNotEmpty()) {
                                        dayWidthPx = it.size.width.toFloat() / weekDays.size
                                    }
                                    if (taskAreaCoordinates == null) {
                                        taskAreaCoordinates =
                                            it
                                    }
                                }

                        ) {
                            weekDays.forEachIndexed { dayIndex, _ ->
                                Box( 
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )

                                {
                                    Canvas(modifier = Modifier.matchParentSize()) {

                                        drawLine(
                                            color = color1,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = 1f
                                        )

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

                                        if (dayIndex > 0) {
                                            drawLine(
                                                color = color1,
                                                start = Offset(0f, 0f),
                                                end = Offset(0f, size.height),
                                                strokeWidth = 1f
                                            )
                                        }
                                    }

                                    if (dayIndex == currentDateIndex && hour == currentTime.hour && dayWidthPx > 0) {
                                        val minuteRatio = currentTime.minute / 60f
                                        val indicatorY = minuteRatio * hourHeightPx

                                        Box( 
                                            modifier = Modifier
                                                .padding(start = 0.dp)
                                                .width(with(density) { dayWidthPx.toDp() })
                                                .height(2.dp)
                                                .offset(y = with(density) { indicatorY.toDp() })
                                                .background(MaterialTheme.colorScheme.primary)
                                                .zIndex(5f) 
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24 * hourHeightDp)
                    .padding(start = 48.dp)
                    .onGloballyPositioned {
                        if (taskAreaCoordinates == null) taskAreaCoordinates = it
                    }
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDays.forEachIndexed { dayIndex, date ->
                        Box( 
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clipToBounds() 
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
            }

            draggedTaskState?.let { currentDrag ->
                val ghostDayIndex = weekDays.indexOf(currentDrag.targetDate)

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

                            .width(
                                if (dayWidthPx > 0f) {
                                    with(density) { dayWidthPx.toDp() } * 0.95f
                                } else {
                                    0.dp
                                }
                            )
                            .height(originalHeightDp)

                            .padding(
                                horizontal = if (dayWidthPx > 0f) {
                                    with(density) { (dayWidthPx * 0.025f).toDp() }
                                } else {
                                    0.dp
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
            }

        }
    } 
}