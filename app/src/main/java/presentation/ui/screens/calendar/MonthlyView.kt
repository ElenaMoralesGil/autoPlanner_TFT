package com.elena.autoplanner.presentation.ui.screens.calendar.MonthlyView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.isToday
import com.elena.autoplanner.presentation.intents.CalendarIntent
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import com.elena.autoplanner.presentation.ui.screens.calendar.getPriorityColor
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@Composable
fun MonthlyView(
    selectedMonth: YearMonth,
    onTaskSelected: (Task) -> Unit,
    calendarViewModel: CalendarViewModel,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = calendarViewModel.state.collectAsState().value
    val tasks = calendarViewModel.loadedTasks // Usar directamente el estado observable
    val isLoading = state?.isLoading ?: false

    val weeksInMonth = remember(selectedMonth) {
        generateCalendarDays(selectedMonth)
    }

    val monthTasks = remember(tasks, selectedMonth) {
        tasks.filter { task ->
            val relevantDate = task.scheduledStartDateTime?.toLocalDate()
                ?: task.startDateConf?.dateTime?.toLocalDate()
            relevantDate?.year == selectedMonth.year && relevantDate.month == selectedMonth.month
        }
            // Ordenar tareas completadas por fecha de finalización y luego por id para evitar glitches
            .sortedWith(compareBy<Task>({ it.completionDateTime }, { it.id }))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthHeader(
            currentMonth = selectedMonth,
            onPreviousMonth = {
                calendarViewModel.sendIntent(
                    CalendarIntent.ChangeDate(selectedMonth.atDay(1).minusMonths(1))
                )
            },
            onNextMonth = {
                calendarViewModel.sendIntent(
                    CalendarIntent.ChangeDate(selectedMonth.atDay(1).plusMonths(1))
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                DaysOfWeekHeader()
            }

            weeksInMonth.forEach { week ->
                item {
                    WeekDaysRow(
                        week = week,
                        onDayClick = { date ->
                            calendarViewModel.sendIntent(CalendarIntent.ChangeDate(date))
                            calendarViewModel.sendIntent(
                                CalendarIntent.ChangeView(CalendarView.DAY)
                            )
                        }
                    )
                }

                item {
                    WeekTasksGrid(
                        week = week,
                        tasks = monthTasks,
                        onTaskClick = onTaskSelected
                    )
                }
            }
            // Eliminar scroll infinito y cargar todas las tareas del mes al entrar
            // Ya no se muestra el indicador de carga ni lógica de scroll
        }
    }
}

@Composable
fun MonthHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_left),
                contentDescription = "Previous Month",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onPreviousMonth)
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "Next Month",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onNextMonth)
            )
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("mon", "tue", "wen", "thu", "fri", "sat", "sun")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun WeekDaysRow(
    week: List<CalendarDay>,
    onDayClick: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        week.forEach { calendarDay ->
            DayCell(
                day = calendarDay,
                isToday = calendarDay.date.isToday(),
                onClick = { onDayClick(calendarDay.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = when {
        isToday -> Color.White
        day.isCurrentMonth -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .padding(2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .run {
                    if (isToday) {
                        clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    } else {
                        this
                    }
                }
                .clickable(onClick = onClick)
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WeekTasksGrid(
    week: List<CalendarDay>,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
) {
    val tasksByDay = remember(tasks, week) {
        tasks.groupBy { task ->

            val relevantDate = task.scheduledStartDateTime?.toLocalDate()
                ?: task.startDateConf?.dateTime?.toLocalDate()

            week.indexOfFirst { calendarDay -> calendarDay.date == relevantDate }
        }.filterKeys { it != -1 } 
    }

    val maxTasksPerDay = remember(tasksByDay) {
        tasksByDay.values.maxOfOrNull { it.size } ?: 0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        for (i in 0 until maxTasksPerDay) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayIndex in week.indices) {
                    val dayTasks = tasksByDay[dayIndex] ?: emptyList()

                    Box(modifier = Modifier.weight(1f)) {
                        if (i < dayTasks.size) {
                            val task = dayTasks[i]
                            TaskCard(
                                task = task,
                                onClick = { onTaskClick(task) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        if (maxTasksPerDay == 0) {
            Spacer(modifier = Modifier.height(24.dp)) 
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(vertical = 1.dp, horizontal = 2.dp)
            .height(24.dp)
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

        Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(
                        color = getPriorityColor(task.priority),
                        shape = RoundedCornerShape(1.dp)
                    )
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = task.name,
                fontSize = 10.sp,
                color = if (task.isCompleted)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
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
    }
}

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
)

fun generateCalendarDays(yearMonth: YearMonth): List<List<CalendarDay>> {
    val result = mutableListOf<List<CalendarDay>>()

    val firstDay = yearMonth.atDay(1)
    val today = LocalDate.now()

    val firstDisplayDay = if (yearMonth.month == today.month) {
        when {
            today.dayOfMonth <= 7 -> firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            today.dayOfMonth >= yearMonth.lengthOfMonth() - 7 ->
                yearMonth.atEndOfMonth().minusWeeks(3)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            else -> today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
    } else {
        val middleOfMonth = yearMonth.atDay(yearMonth.lengthOfMonth() / 2)
        middleOfMonth.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    var currentDay = firstDisplayDay
    val days = mutableListOf<CalendarDay>()

    for (i in 0 until 28) {
        days.add(
            CalendarDay(
                date = currentDay,
                isCurrentMonth = currentDay.month == yearMonth.month
            )
        )

        currentDay = currentDay.plusDays(1)

        if (days.size == 7) {
            result.add(days.toList())
            days.clear()
        }
    }

    return result
}