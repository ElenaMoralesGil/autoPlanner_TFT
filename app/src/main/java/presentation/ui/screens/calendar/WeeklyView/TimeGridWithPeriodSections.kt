package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.isToday
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    onTaskTimeChanged: (Task, LocalTime, Long) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    currentTime: LocalTime
) {
    val hours = (0..23).toList()
    val currentDateIndex = weekDays.indexOfFirst { it.isToday() }
    val hourHeightPx = with(LocalDensity.current) { hourHeightDp.toPx() }

    var draggedTaskState by remember { mutableStateOf<WeeklyTaskDragState?>(null) }

    val continuousTasksMap = weekDays.associateWith { date ->
        scheduledTasks.filter { task ->
            task.startDateConf?.dateTime?.toLocalDate() == date
        }.map { task ->
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24 * hourHeightDp)
            ) {

                Spacer(modifier = Modifier.width(48.dp))


                Row(modifier = Modifier.weight(1f)) {

                    weekDays.forEachIndexed { dayIndex, date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clipToBounds()
                        ) {

                            continuousTasksMap[date]?.forEach { continuousTask ->
                                val startDateTime = continuousTask.startDateTime
                                val endDateTime = continuousTask.endDateTime

                                val startHourMinutes =
                                    startDateTime.hour * 60 + startDateTime.minute
                                val endHourMinutes = endDateTime.hour * 60 + endDateTime.minute


                                val topOffset = with(LocalDensity.current) {
                                    (startHourMinutes / 60f * hourHeightDp.toPx()).toDp()
                                }


                                val durationMinutes = endHourMinutes - startHourMinutes
                                val height = with(LocalDensity.current) {
                                    (durationMinutes / 60f * hourHeightDp.toPx()).toDp()
                                }


                                if (durationMinutes > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .width(IntrinsicSize.Max)
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
