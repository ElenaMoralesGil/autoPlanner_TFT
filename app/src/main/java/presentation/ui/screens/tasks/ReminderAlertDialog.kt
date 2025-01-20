package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan

@Composable
fun ReminderAlertDialog(
    existing: ReminderPlan?,
    onDismiss: () -> Unit,
    onReady: (ReminderPlan?) -> Unit
) {
    var showPersonalized by remember { mutableStateOf(false) }
    var localRem by remember { mutableStateOf(existing ?: ReminderPlan(mode = ReminderMode.NONE)) }

    if (!showPersonalized) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Reminder") },
            text = {
                Column {
                    // None
                    RowItem(label = "None", selected = (localRem.mode == ReminderMode.NONE)) {
                        localRem = localRem.copy(mode = ReminderMode.NONE, offsetMinutes = null)
                    }
                    DividerLine()

                    // Quick picks for offset
                    val picks = listOf(
                        "On time" to 0,
                        "5 min early" to 5,
                        "30 min early" to 30,
                        "1 day early" to 1440,
                        "1 week early" to 10080
                    )
                    picks.forEach { (label, offset) ->
                        val isSelected = (localRem.mode == ReminderMode.PRESET_OFFSET && localRem.offsetMinutes == offset)
                        RowItem(label = label, selected = isSelected) {
                            localRem = localRem.copy(
                                mode = ReminderMode.PRESET_OFFSET,
                                offsetMinutes = offset,
                                exactDateTime = null
                            )
                        }
                        DividerLine()
                    }

                    // Personalized
                    val isPersonalized = (localRem.mode == ReminderMode.CUSTOM)
                    RowItem(label = "Personalized", selected = isPersonalized) {
                        showPersonalized = true
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onReady(localRem) }) {
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
        // Show personalized sub-dialog
        ReminderPersonalizedAlertDialog(
            existing = localRem,
            onDismiss = { showPersonalized = false },
            onReady = { newVal ->
                localRem = newVal
                showPersonalized = false
            }
        )
    }
}

/**
 * A second small dialog for "Personalized" mode:
 * the user can pick offset minutes in days/hours/min or an exact time, etc.
 */
@Composable
fun ReminderPersonalizedAlertDialog(
    existing: ReminderPlan,
    onDismiss: () -> Unit,
    onReady: (ReminderPlan) -> Unit
) {
    var daysBefore by remember { mutableStateOf(0) }
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }

    // If we had an offset, parse it
    LaunchedEffect(Unit) {
        if (existing.mode == ReminderMode.CUSTOM && existing.offsetMinutes != null) {
            val total = existing.offsetMinutes
            daysBefore = total / 1440
            val leftover = total % 1440
            hours = leftover / 60
            minutes = leftover % 60
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalized Reminder") },
        text = {
            Column {
                Text("Days before: $daysBefore")
                RowItem(label = "Increase days", selected = false) {
                    daysBefore++
                }
                RowItem(label = "Decrease days", selected = false) {
                    if (daysBefore > 0) daysBefore--
                }

                Spacer(Modifier.width(16.dp))

                Text("Hours: $hours")
                RowItem(label = "Increase hours", selected = false) {
                    if (hours < 23) hours++
                }
                RowItem(label = "Decrease hours", selected = false) {
                    if (hours > 0) hours--
                }

                Spacer(Modifier.width(16.dp))

                Text("Minutes: $minutes")
                RowItem(label = "Increase minutes", selected = false) {
                    if (minutes < 59) minutes++
                }
                RowItem(label = "Decrease minutes", selected = false) {
                    if (minutes > 0) minutes--
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalOffset = daysBefore*1440 + hours*60 + minutes
                    val plan = existing.copy(
                        mode = ReminderMode.CUSTOM,
                        offsetMinutes = totalOffset
                    )
                    onReady(plan)
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

