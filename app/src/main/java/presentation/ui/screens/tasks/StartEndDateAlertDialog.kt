package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import domain.models.DayPeriod
import domain.models.TimePlanning
import java.time.*

@Composable
fun StartEndDateAlertDialog(
    label: String,
    existing: TimePlanning?,
    onDismiss: () -> Unit,
    onReady: (TimePlanning?) -> Unit
) {
    var selectedDateTime by remember { mutableStateOf(existing?.dateTime ?: LocalDateTime.now()) }
    var dayPeriod by remember { mutableStateOf(existing?.dayPeriod ?: DayPeriod.MORNING) }

    val selectedDate = selectedDateTime.toLocalDate()
    val selectedTime = selectedDateTime.toLocalTime()

    var displayYear by remember { mutableStateOf(selectedDate.year) }
    var displayMonth by remember { mutableStateOf(selectedDate.monthValue) }

    var showHourPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val finalPlanning = TimePlanning(
                        dateTime = selectedDateTime,
                        dayPeriod = dayPeriod
                    )
                    onReady(finalPlanning)
                }
            ) {
                Text("Ready")
            }
        },
        dismissButton = {
            TextButton(onClick = { onReady(null) }) {
                Text("None")
            }
        },
        text = {
            Column(Modifier.padding(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DayPeriodOption(
                        iconRes = R.drawable.morning,
                        iconLabel = "Morning",
                        range = "6AM-12PM",
                        isSelected = (dayPeriod == DayPeriod.MORNING)
                    ) { dayPeriod = DayPeriod.MORNING }

                    DayPeriodOption(
                        iconRes = R.drawable.evening,
                        iconLabel = "Evening",
                        range = "12PM-6PM",
                        isSelected = (dayPeriod == DayPeriod.EVENING)
                    ) { dayPeriod = DayPeriod.EVENING }

                    DayPeriodOption(
                        iconRes = R.drawable.night,
                        iconLabel = "Night",
                        range = "6PM-12AM",
                        isSelected = (dayPeriod == DayPeriod.NIGHT)
                    ) { dayPeriod = DayPeriod.NIGHT }

                    DayPeriodOption(
                        iconRes = R.drawable.all_day,
                        iconLabel = "All day",
                        range = "",
                        isSelected = (dayPeriod == DayPeriod.ALLDAY)
                    ) { dayPeriod = DayPeriod.ALLDAY }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthYearLabel(displayMonth, displayYear),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f) // Empuja las flechas hacia la derecha
                    )
                    Row {
                        IconButton(onClick = {
                            val newMonth = displayMonth - 1
                            if (newMonth < 1) {
                                displayMonth = 12
                                displayYear--
                            } else {
                                displayMonth = newMonth
                            }
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Month")
                        }

                        IconButton(onClick = {
                            val newMonth = displayMonth + 1
                            if (newMonth > 12) {
                                displayMonth = 1
                                displayYear++
                            } else {
                                displayMonth = newMonth
                            }
                        }) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    }
                }

                DayOfWeekHeaderRow()

                CalendarGrid(
                    displayYear = displayYear,
                    displayMonth = displayMonth,
                    selectedDay = selectedDate.dayOfMonth,
                    todayDay = if (today.year == displayYear && today.monthValue == displayMonth) today.dayOfMonth else null
                ) { day ->
                    val newLocalDate = LocalDate.of(displayYear, displayMonth, day)
                    selectedDateTime = LocalDateTime.of(newLocalDate, selectedTime)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hour")
                    val timeLabel = if (dayPeriod == DayPeriod.ALLDAY) "None"
                    else "${selectedTime.hour}:${selectedTime.minute.toString().padStart(2, '0')}"
                    TextButton(onClick = { showHourPicker = true }) {
                        Text(timeLabel)
                    }
                }

                if (showHourPicker) {
                    HourMinutePickerDialog(
                        initialTime = selectedTime,
                        onDismiss = { showHourPicker = false },
                        onConfirm = { chosenTime ->
                            val newDate = selectedDateTime.toLocalDate()
                            selectedDateTime = LocalDateTime.of(newDate, chosenTime)
                            showHourPicker = false
                        }
                    )
                }
            }
        },
        title = null
    )
}

@Composable
fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(40.dp) // Ajustar tamaño de las celdas
            .padding(4.dp)
            .clip(RoundedCornerShape(50)) // Celdas redondeadas
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$day",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun DayOfWeekHeaderRow() {
    val dayNames = listOf("mon","tue","wen","thu","fri","sat","sun")
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

/**
 * Just a simple arrangement using LazyVerticalGrid or manual approach
 * We'll do a 6-row x 7-column grid to display up to 42 cells.
 * We show 1..30 for simplicity.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarGrid(
    displayYear: Int,
    displayMonth: Int,
    selectedDay: Int,
    todayDay: Int?,
    onDayClick: (Int) -> Unit
) {
    // 1. Obtener el número de días en el mes actual
    val daysInMonth = YearMonth.of(displayYear, displayMonth).lengthOfMonth()

    // 2. Determinar el día de la semana del primer día del mes
    val firstDayOfWeek = LocalDate.of(displayYear, displayMonth, 1).dayOfWeek.value
    val leadingEmptyDays = (firstDayOfWeek - 1) % 7 // Ajustar al inicio de la semana

    // 3. Crear una lista para celdas: vacías + días del mes
    val calendarCells = buildList {
        // Agregar celdas vacías al inicio
        repeat(leadingEmptyDays) { add(null) }
        // Agregar los días del mes
        for (day in 1..daysInMonth) add(day)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(200.dp), // Ajustar el alto según tus necesidades
        userScrollEnabled = false // Sin desplazamiento vertical
    ) {
        items(calendarCells) { day ->
            if (day != null) {
                // Día válido del mes
                val isToday = (day == todayDay)
                val isSelected = (day == selectedDay)

                DayCell(
                    day = day,
                    isToday = isToday,
                    isSelected = isSelected,
                    onClick = { onDayClick(day) }
                )
            } else {
                // Celda vacía
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp)
                )
            }
        }
    }
}


/**
 * "DayPeriodOption" for morning/evening/night/all day.
 * Renders an icon from resources + text + sublabel if needed.
 */
@Composable
fun DayPeriodOption(
    iconRes: Int,
    iconLabel: String,
    range: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = iconLabel,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = iconLabel,
            color = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            style = MaterialTheme.typography.bodySmall
        )
        if (range.isNotEmpty()) {
            Text(
                text = range,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Utility for e.g. "Oct 2024"
 */
fun monthYearLabel(month: Int, year: Int): String {
    val monthName = when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> "??"
    }
    return "$monthName $year"
}

/**
 * Sub-dialog for picking hour and minute
 */
@Composable
fun HourMinutePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by remember { mutableStateOf(initialTime.hour) }
    var minute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Hour & Minute") },
        text = {
            Column {
                Text("Hour: $hour")
                Row {
                    TextButton(onClick = { if (hour > 0) hour-- }) { Text("-") }
                    TextButton(onClick = { if (hour < 23) hour++ }) { Text("+") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Minute: $minute")
                Row {
                    TextButton(onClick = { if (minute > 0) minute-- }) { Text("-") }
                    TextButton(onClick = { if (minute < 59) minute++ }) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(hour, minute))
            }) {
                Text("Ready")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
