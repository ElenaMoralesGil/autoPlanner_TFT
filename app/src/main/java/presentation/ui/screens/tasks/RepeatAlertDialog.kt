package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.RepeatPlan

@Composable
fun RepeatAlertDialog(
    existing: RepeatPlan?,
    onDismiss: () -> Unit,
    onReady: (RepeatPlan?) -> Unit
) {
    var showPersonalized by remember { mutableStateOf(false) }
    var localRepeat by remember {
        mutableStateOf(existing ?: RepeatPlan(frequencyType = FrequencyType.NONE))
    }

    if (!showPersonalized) {
        // Basic "quick picks" (None, daily, weekly, monthly, etc.)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Repeat") },
            text = {
                Column {
                    // "None" option
                    RowItem(
                        label = "None",
                        selected = localRepeat.frequencyType == FrequencyType.NONE
                    ) {
                        localRepeat = localRepeat.copy(frequencyType = FrequencyType.NONE)
                    }
                    DividerLine()

                    // Some quick frequencies:
                    val picks = listOf(
                        FrequencyType.DAILY to "Daily",
                        FrequencyType.WEEKLY to "Weekly",
                        FrequencyType.MONTHLY to "Monthly",
                        FrequencyType.YEARLY to "Yearly",
                        FrequencyType.WEEKDAYS to "Weekdays",
                        FrequencyType.WEEKENDS to "Weekends"
                    )
                    picks.forEach { (type, label) ->
                        RowItem(
                            label = label,
                            selected = (localRepeat.frequencyType == type)
                        ) {
                            localRepeat = localRepeat.copy(frequencyType = type)
                        }
                        DividerLine()
                    }

                    // Personalized
                    RowItem(
                        label = "Personalized",
                        selected = localRepeat.frequencyType == FrequencyType.CUSTOM
                    ) {
                        showPersonalized = true
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onReady(localRepeat) }) {
                    Text("Ready")
                }
            },
            dismissButton = {
                TextButton(onClick = { onReady(null) }) {
                    Text("None")
                }
            }
        )
    } else {
        // Show an advanced "personalized" sub-dialog:
        RepeatPersonalizedAlertDialog(
            existing = localRepeat,
            onDismiss = { showPersonalized = false },
            onReady = { newVal ->
                localRepeat = newVal
                showPersonalized = false
            }
        )
    }
}

/**
 * Example of a "personalized" sub-dialog if the user picks CUSTOM frequency.
 * You can expand it with day-of-month, week-of-month, etc.
 */
@Composable
fun RepeatPersonalizedAlertDialog(
    existing: RepeatPlan,
    onDismiss: () -> Unit,
    onReady: (RepeatPlan) -> Unit
) {
    var interval by remember { mutableStateOf(existing.interval ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalized Repeat") },
        text = {
            Column {
                Text("Interval: $interval")
                // E.g. basic +/- or a slider
                RowItem(label = "Interval++", selected = false) { interval++ }
                RowItem(label = "Interval--", selected = false) {
                    if (interval > 1) interval--
                }
                // expand for daysOfWeek, etc.
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onReady(existing.copy(
                        frequencyType = FrequencyType.CUSTOM,
                        interval = interval
                    ))
                }
            ) {
                Text("Ready")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back")
            }
        }
    )
}

/** Simple row item with a label and a "selectable" behavior. */
@Composable
fun RowItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.clickable { onClick() }) {
        if (selected) {
            Text("✓ ", modifier = Modifier.width(20.dp))
        } else {
            Spacer(Modifier.width(20.dp))
        }
        Text(label)
    }
}

@Composable
fun DividerLine() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}
