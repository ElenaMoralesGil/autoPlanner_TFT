package com.elena.autoplanner.presentation.ui.utils


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.isToday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun CustomCalendar(
    currentMonth: YearMonth,
    selectedDates: List<LocalDate>,
    modifier: Modifier = Modifier,
    highlightedDates: List<LocalDate> = emptyList(),
    isInteractive: Boolean = true,
    showNavigation: Boolean = true,
    onDateSelected: (LocalDate) -> Unit = {},
    onMonthChanged: (YearMonth) -> Unit = {},
) {
    Column(modifier = modifier) {
        if (showNavigation) {
            CalendarHeader(
                currentMonth = currentMonth,
                onPreviousMonthClick = { onMonthChanged(currentMonth.minusMonths(1)) },
                onNextMonth = { onMonthChanged(currentMonth.plusMonths(1)) }
            )
        }

        DayOfWeekHeaderRow()

        MonthGrid(
            currentMonth = currentMonth,
            selectedDates = selectedDates,
            highlightedDates = highlightedDates,
            isInteractive = isInteractive,
            onDayClick = { day ->
                onDateSelected(LocalDate.of(currentMonth.year, currentMonth.monthValue, day))
            }
        )
    }
}

@Composable
fun WeekHeader(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekDays = remember(selectedDate) {
        selectedDate.getWeekDays()
    }

    Column(modifier = modifier) {
        DayOfWeekHeaderRow()
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(56.dp)
        ) {
            items(weekDays) { date ->
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date.isToday(),
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonthClick: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onPreviousMonthClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month")
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month")
        }
    }
}

@Composable
fun DayOfWeekHeaderRow() {
    val dayNames = listOf("mon", "tue", "wen", "thu", "fri", "sat", "sun")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayNames.forEach { label ->
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(32.dp)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    currentMonth: YearMonth,
    selectedDates: List<LocalDate>,
    highlightedDates: List<LocalDate>,
    isInteractive: Boolean,
    onDayClick: (Int) -> Unit,
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value
    val leadingEmptyDays = (firstDayOfWeek - 1) % 7
    val today = LocalDate.now()

    val calendarCells = remember(currentMonth) {
        MutableList(leadingEmptyDays) { null } + (1..daysInMonth).toList()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(200.dp),
        userScrollEnabled = false
    ) {
        items(calendarCells) { day ->
            if (day != null) {
                val date = LocalDate.of(currentMonth.year, currentMonth.monthValue, day)
                DayCell(
                    date = date,
                    isSelected = selectedDates.contains(date),
                    isToday = date == today,
                    onClick = { onDayClick(day) }
                )
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = when {
                isSelected -> Color.White
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

fun LocalDate.getWeekDays(): List<LocalDate> {
    val firstDay = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { firstDay.plusDays(it.toLong()) }
}
