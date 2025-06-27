package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatDateTimeWithPeriod
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatDurationForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatReminderForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatRepeatForDisplay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeConfigSheet(
    onClose: () -> Unit,
    currentStart: TimePlanning?,
    currentEnd: TimePlanning?,
    currentDuration: DurationPlan?,
    currentReminder: ReminderPlan?,
    currentRepeat: RepeatPlan?,
    currentAllowSplitting: Boolean? = null,
    onSaveAll: (
        TimePlanning?,
        TimePlanning?,
        DurationPlan?,
        ReminderPlan?,
        RepeatPlan?,
        allowSplitting: Boolean?,
    ) -> Unit,
) {

    var localStart by remember { mutableStateOf(currentStart) }
    var localEnd by remember { mutableStateOf(currentEnd) }
    var localDuration by remember { mutableStateOf(currentDuration) }
    var localReminder by remember { mutableStateOf(currentReminder) }
    var localRepeat by remember { mutableStateOf(currentRepeat) }
    var allowSplitting by remember { mutableStateOf(currentAllowSplitting) }
    val durationMinutes = localDuration?.totalMinutes ?: 60
    val shouldShowSplitting = localDuration?.let { it.totalMinutes!! >= 30 } == true

    var openDialog by remember { mutableStateOf<TimeDialogType?>(null) }


    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
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
                onSaveAll(
                    localStart, localEnd, localDuration, localReminder, localRepeat,
                    allowSplitting
                )
                onClose()
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save time config")
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TimeConfigItem(
                "Start date",
                formatDateTimeWithPeriod(localStart)
            ) { openDialog = TimeDialogType.StartDate }
            HorizontalDivider()
            TimeConfigItem(
                "End date",
                formatDateTimeWithPeriod(localEnd)
            ) { openDialog = TimeDialogType.EndDate }
            HorizontalDivider()
            TimeConfigItem(
                "Duration",
                formatDurationForDisplay(localDuration)
            ) { openDialog = TimeDialogType.Duration }

            AnimatedVisibility(
                visible = shouldShowSplitting,
                enter = slideInVertically() + expandVertically(),
                exit = slideOutVertically() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Task Splitting",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Can this task be split into smaller chunks if needed?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Compact splitting options for dialog
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactSplittingOption(
                                text = "Auto",
                                isSelected = allowSplitting == null,
                                onClick = { allowSplitting = null },
                                modifier = Modifier.weight(1f)
                            )
                            CompactSplittingOption(
                                text = "Allow",
                                isSelected = allowSplitting == true,
                                onClick = { allowSplitting = true },
                                modifier = Modifier.weight(1f)
                            )
                            CompactSplittingOption(
                                text = "Keep Together",
                                isSelected = allowSplitting == false,
                                onClick = { allowSplitting = false },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            TimeConfigItem(
                "Reminder",
                formatReminderForDisplay(localReminder)
            ) { openDialog = TimeDialogType.Reminder }

            HorizontalDivider()
            TimeConfigItem(
                "Repeat",
                formatRepeatForDisplay(localRepeat)
            ) { openDialog = TimeDialogType.Repeat }

        }
    }

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
                validator = { planning ->
                    planning?.let {
                        if (localStart != null) {
                            if (it.dateTime?.isBefore(localStart!!.dateTime) == true)
                                "End date cannot be before start date."
                            else null
                        } else {
                            if (it.dateTime?.toLocalDate()?.isBefore(LocalDate.now()) == true)
                                "End date cannot be before today."
                            else null
                        }
                    }
                },
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

@Composable
fun CompactSplittingOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier
    )
}

enum class TimeDialogType {
    StartDate, EndDate, Duration, Reminder, Repeat
}