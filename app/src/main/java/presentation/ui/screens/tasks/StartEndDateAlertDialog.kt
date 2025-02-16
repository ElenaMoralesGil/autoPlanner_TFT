package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun StartEndDateAlertDialog(
    label: String,
    existing: TimePlanning?,
    highlightDate: LocalDate?,
    onDismiss: () -> Unit,
    onReady: (TimePlanning?) -> Unit,
    validator: ((TimePlanning?) -> String?)? = null
) {
    var selectedDateTime by remember { mutableStateOf(existing?.dateTime ?: LocalDateTime.now()) }
    var dayPeriod by remember { mutableStateOf(existing?.dayPeriod ?: DayPeriod.NONE) }

    val selectedDate = selectedDateTime.toLocalDate()
    val selectedTime = selectedDateTime.toLocalTime()

    var displayYear by remember { mutableStateOf(selectedDate.year) }
    var displayMonth by remember { mutableStateOf(selectedDate.monthValue) }

    var showHourPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    var errorMessage by remember { mutableStateOf<String?>(null) }

    GeneralAlertDialog(

        title = {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },

        content = {
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
                    ) {
                        dayPeriod =
                            if (dayPeriod == DayPeriod.MORNING) DayPeriod.NONE else DayPeriod.MORNING

                    }

                    DayPeriodOption(
                        iconRes = R.drawable.evening,
                        iconLabel = "Evening",
                        range = "12PM-6PM",
                        isSelected = (dayPeriod == DayPeriod.EVENING)
                    ) {
                        dayPeriod =
                            if (dayPeriod == DayPeriod.EVENING) DayPeriod.NONE else DayPeriod.EVENING

                    }

                    DayPeriodOption(
                        iconRes = R.drawable.night,
                        iconLabel = "Night",
                        range = "6PM-12AM",
                        isSelected = (dayPeriod == DayPeriod.NIGHT)
                    ) {
                        dayPeriod =
                            if (dayPeriod == DayPeriod.NIGHT) DayPeriod.NONE else DayPeriod.NIGHT
                    }

                    DayPeriodOption(
                        iconRes = R.drawable.all_day,
                        iconLabel = "All day",
                        range = "",
                        isSelected = (dayPeriod == DayPeriod.ALLDAY)
                    ) {
                        dayPeriod =
                            if (dayPeriod == DayPeriod.ALLDAY) DayPeriod.NONE else DayPeriod.ALLDAY

                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthYearLabel(displayMonth, displayYear),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f),
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
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month"
                            )
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
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month"
                            )
                        }
                    }
                }

                DayOfWeekHeaderRow()

                CalendarGrid(
                    displayYear = displayYear,
                    displayMonth = displayMonth,
                    selectedDay = selectedDate.dayOfMonth,
                    todayDay = if (today.year == displayYear && today.monthValue == displayMonth) today.dayOfMonth else null,
                    highlightDate = highlightDate
                ) { day ->
                    val newLocalDate = LocalDate.of(displayYear, displayMonth, day)
                    selectedDateTime = LocalDateTime.of(newLocalDate, selectedTime)
                }

                Spacer(Modifier.height(16.dp))


                AnimatedVisibility(
                    visible = (dayPeriod == DayPeriod.NONE),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Time",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box(
                                modifier = Modifier,
                                contentAlignment = Alignment.Center
                            ) {
                                val timeLabel by remember {
                                    derivedStateOf {
                                        val hour = selectedDateTime.toLocalTime().hour.toString().padStart(2, '0')
                                        val minute = selectedDateTime.toLocalTime().minute.toString().padStart(2, '0')
                                        "$hour:$minute"
                                    }
                                }

                                TextButton(onClick = { showHourPicker = true }) {
                                    Text(
                                        text = timeLabel,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (showHourPicker) {
                            HourMinutePickerDialog(
                                initialTime = selectedDateTime.toLocalTime(),
                                onDismiss = { showHourPicker = false },
                                onConfirm = { chosenTime ->
                                    selectedDateTime = LocalDateTime.of(selectedDate, chosenTime)
                                    showHourPicker = false
                                }
                            )
                        }
                    }
                }
            }
        },
        onDismiss = onDismiss,

        onConfirm = {
            val finalPlanning = TimePlanning(dateTime = selectedDateTime, dayPeriod = dayPeriod)

            if (validator != null) {
                val validationError = validator(finalPlanning)
                if (validationError != null) {
                    errorMessage = validationError
                    return@GeneralAlertDialog
                }
            }
            onReady(finalPlanning)
        },

        onNeutral = {
            onReady(null)
        }
    )

    if (errorMessage != null) {
        GeneralAlertDialog(
            title = { Text("Error") },
            content = { Text(errorMessage!!) },
            onDismiss = { errorMessage = null },
            onConfirm = { errorMessage = null },
            hideDismissButton = true
        )
    }
}

@Composable
fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isHighlighted -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(20))
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

@Composable
fun CalendarGrid(
    displayYear: Int,
    displayMonth: Int,
    selectedDay: Int,
    highlightDate: LocalDate?,
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
                val isHighlighted = highlightDate != null && highlightDate == LocalDate.of(displayYear, displayMonth, day)

                DayCell(
                    day = day,
                    isToday = isToday,
                    isSelected = isSelected,
                    isHighlighted = isHighlighted,
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

@Composable
fun HourMinutePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Selector de Horas (0..23)
                TimePickerColumn(
                    range = 0..23,
                    selectedValue = selectedHour,
                    onValueChange = { newHour -> selectedHour = newHour },
                    label = "Hour"
                )

                // Selector de Minutos (0..59)
                TimePickerColumn(
                    range = 0..59,
                    selectedValue = selectedMinute,
                    onValueChange = { newMinute -> selectedMinute = newMinute },
                    label = "Minute"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(selectedHour, selectedMinute))
            }) {
                Text("Ready")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimePickerColumn(
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    val itemHeight = 40.dp

    // 1. Convertir la altura en píxeles dentro del contexto composable:
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedValue)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .height(itemHeight * 5)  // 5 ítems visibles, por ejemplo
                .width(80.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = itemHeight * 2),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(range.count()) { index ->
                    val value = range.first + index
                    val isSelected = (value == selectedValue)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
                    .height(itemHeight)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
    }

    LaunchedEffect(listState, itemHeightPx) {
        snapshotFlow {
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val offsetFirstItem = firstVisibleItem?.offset ?: 0
            val offsetItems = offsetFirstItem / itemHeightPx

            Pair(listState.firstVisibleItemIndex, offsetItems)
        }.collect { (firstIndex, offsetItems) ->
            // Ajuste a 2.5 para que el 3er ítem (índice 2 de 0..4) sea el verdadero centro
            val centerIndex = (firstIndex + offsetItems + 2.5f).roundToInt()
            val boundedIndex = centerIndex.coerceIn(0, range.count() - 1)
            val newValue = range.first + boundedIndex

            if (newValue != selectedValue) {
                onValueChange(newValue)
            }
        }
    }

}