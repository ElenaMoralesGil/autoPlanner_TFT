package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.presentation.ui.utils.CustomCalendar
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog
import com.elena.autoplanner.presentation.ui.utils.NumberPicker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

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

    // Calendar state management
    val initialDate = selectedDateTime.toLocalDate()
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    var showHourPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultPeriodTimes = mapOf(
        DayPeriod.MORNING to LocalTime.of(6, 0),
        DayPeriod.EVENING to LocalTime.of(12, 0),
        DayPeriod.NIGHT to LocalTime.of(18, 0),
        DayPeriod.ALLDAY to LocalTime.MIDNIGHT
    )

    fun handlePeriodSelection(newPeriod: DayPeriod) {
        val finalPeriod = if (dayPeriod == newPeriod) DayPeriod.NONE else newPeriod
        dayPeriod = finalPeriod

        if (finalPeriod != DayPeriod.NONE) {
            val newTime = when (finalPeriod) {
                DayPeriod.MORNING -> LocalTime.of(6, 0)
                DayPeriod.EVENING -> LocalTime.of(12, 0)
                DayPeriod.NIGHT -> LocalTime.of(18, 0)
                DayPeriod.ALLDAY -> LocalTime.MIDNIGHT
                else -> selectedDateTime.toLocalTime()
            }
            selectedDateTime = LocalDateTime.of(selectedDateTime.toLocalDate(), newTime)
        }
    }


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
                        handlePeriodSelection(DayPeriod.MORNING)
                    }


                    DayPeriodOption(
                        iconRes = R.drawable.evening,
                        iconLabel = "Evening",
                        range = "12PM-6PM",
                        isSelected = (dayPeriod == DayPeriod.EVENING)
                    ) {
                        handlePeriodSelection(DayPeriod.EVENING)
                    }

                    DayPeriodOption(
                        iconRes = R.drawable.night,
                        iconLabel = "Night",
                        range = "6PM-12AM",
                        isSelected = (dayPeriod == DayPeriod.NIGHT)
                    ) {
                        handlePeriodSelection(DayPeriod.NIGHT)
                    }

                    DayPeriodOption(
                        iconRes = R.drawable.all_day,
                        iconLabel = "All day",
                        range = "",
                        isSelected = (dayPeriod == DayPeriod.ALLDAY)
                    ) {
                        handlePeriodSelection(DayPeriod.ALLDAY)
                    }
                }

                Spacer(Modifier.height(16.dp))

                CustomCalendar(
                    currentMonth = currentMonth,
                    selectedDates = listOf(selectedDateTime.toLocalDate()),
                    highlightedDates = highlightDate?.let { listOf(it) } ?: emptyList(),
                    onDateSelected = { date ->
                        selectedDateTime = LocalDateTime.of(date, selectedDateTime.toLocalTime())
                    },
                    onMonthChanged = { newMonth ->
                        currentMonth = newMonth
                    }
                )

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
                                        val hour = selectedDateTime.toLocalTime().hour.toString()
                                            .padStart(2, '0')
                                        val minute =
                                            selectedDateTime.toLocalTime().minute.toString()
                                                .padStart(2, '0')
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
                                    dayPeriod = DayPeriod.NONE
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
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour picker using the refactored NumberPicker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hour")
                    NumberPicker(
                        value = selectedHour,
                        range = 0..23,
                        onValueChange = { selectedHour = it },
                        modifier = Modifier.width(100.dp)
                    )
                }
                // Minute picker using the refactored NumberPicker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Minute")
                    NumberPicker(
                        value = selectedMinute,
                        range = 0..59,
                        onValueChange = { selectedMinute = it },
                        modifier = Modifier.width(100.dp)
                    )
                }
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


@Composable
fun TimePickerColumn(
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))
        NumberPicker(
            value = selectedValue,
            range = range,
            onValueChange = onValueChange,
            modifier = Modifier.width(80.dp)
        )
    }
}