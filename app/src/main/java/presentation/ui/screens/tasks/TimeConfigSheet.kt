package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatDateTimeWithPeriod
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatDurationForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatReminderForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatRepeatForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeConfigSheet(
    onClose: () -> Unit,
    currentStart: TimePlanning?,
    currentEnd: TimePlanning?,
    currentDuration: DurationPlan?,
    currentReminder: ReminderPlan?,
    currentRepeat: RepeatPlan?,
    onSaveAll: (
        TimePlanning?,
        TimePlanning?,
        DurationPlan?,
        ReminderPlan?,
        RepeatPlan?
    ) -> Unit
) {
    // Local copies
    var localStart by remember { mutableStateOf(currentStart) }
    var localEnd by remember { mutableStateOf(currentEnd) }
    var localDuration by remember { mutableStateOf(currentDuration) }
    var localReminder by remember { mutableStateOf(currentReminder) }
    var localRepeat by remember { mutableStateOf(currentRepeat) }

    // Which dialog is open above the sheet
    var openDialog by remember { mutableStateOf<TimeDialogType?>(null) }

    // The bottom sheet that sits behind
    ModalBottomSheet(onDismissRequest = onClose) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close time config")
            }
            Text("Time options", modifier = Modifier.align(Alignment.CenterVertically))
            IconButton(onClick = {
                onSaveAll(localStart, localEnd, localDuration, localReminder, localRepeat)
                onClose()
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save time config")
            }
        }

        // The 5 items
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            TimeConfigItem("Start date",
                formatDateTimeWithPeriod(localStart)
            ) { openDialog = TimeDialogType.StartDate }
            HorizontalDivider()
            TimeConfigItem("End date",
                formatDateTimeWithPeriod(localEnd)
            ) { openDialog = TimeDialogType.EndDate }
            HorizontalDivider()
            TimeConfigItem("Duration",
                formatDurationForDisplay(localDuration)
            ) { openDialog = TimeDialogType.Duration }

            HorizontalDivider()
            TimeConfigItem("Reminder",
                formatReminderForDisplay(localReminder)
            ) { openDialog = TimeDialogType.Reminder }

            HorizontalDivider()
            TimeConfigItem("Repeat",
                formatRepeatForDisplay(localRepeat)
            ) { openDialog = TimeDialogType.Repeat }

        }
    }

    // Show the small floating dialogs as an AlertDialog (or custom) above the sheet
    when (openDialog) {
        TimeDialogType.StartDate -> {
            StartEndDateAlertDialog(
                label = "Start date",
                existing = localStart,
                highlightDate = null,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    localStart = newVal
                    openDialog = null
                }
            )
        }
        TimeDialogType.EndDate -> {
            StartEndDateAlertDialog(
                label = "End date",
                existing = localEnd,
                highlightDate = localStart?.dateTime?.toLocalDate(),
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    localEnd = newVal
                    openDialog = null
                }
            )
        }
        TimeDialogType.Duration -> {
            DurationAlertDialog(
                existing = localDuration,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    localDuration = newVal
                    openDialog = null
                }
            )
        }
        TimeDialogType.Reminder -> {
            ReminderAlertDialog(
                existing = localReminder,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    localReminder = newVal
                    openDialog = null
                }
            )
        }
        TimeDialogType.Repeat -> {
            RepeatAlertDialog(
                existing = localRepeat,
                onDismiss = { openDialog = null },
                onReady = { newVal ->
                    localRepeat = newVal
                    openDialog = null
                }
            )
        }
        else -> {}
    }
}

@Composable
fun TimeConfigItem(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}

enum class TimeDialogType {
    StartDate, EndDate, Duration, Reminder, Repeat
}
