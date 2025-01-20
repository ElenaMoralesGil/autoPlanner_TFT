package com.elena.autoplanner.presentation.ui.screens.tasks


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.models.DurationPlan

@Composable
fun DurationAlertDialog(
    existing: DurationPlan?,
    onDismiss: () -> Unit,
    onReady: (DurationPlan?) -> Unit
) {
    var useHours by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf(0) }

    // interpret existing
    LaunchedEffect(Unit) {
        val mins = existing?.totalMinutes
        if (mins != null) {
            if (mins % 60 == 0) {
                useHours = true
                number = mins / 60
            } else {
                number = mins
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duration") },
        text = {
            Column {
                Row {
                    Text("Value: $number")
                    Spacer(Modifier.width(8.dp))
                    Text(if (useHours) "Hours" else "Minutes")
                }
                // plus +/- buttons, or a slider
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val total = if (useHours) number * 60 else number
                    onReady(DurationPlan(total))
                }
            ) {
                Text("Ready")
            }
        },
        dismissButton = {
            TextButton(onClick = { onReady(null) }) {
                Text("None")
            }
        }
    )
}
