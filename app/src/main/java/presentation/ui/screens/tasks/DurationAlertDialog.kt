package com.elena.autoplanner.presentation.ui.screens.tasks


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog

@Composable
fun DurationAlertDialog(
    existing: DurationPlan?,
    onDismiss: () -> Unit,
    onReady: (DurationPlan?) -> Unit,
) {

    var useHours by remember { mutableStateOf(false) }
    var numberText by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val minutes = existing?.totalMinutes
        if (minutes != null) {
            if (minutes % 60 == 0) {
                useHours = true
                numberText = (minutes / 60).toString()
            } else {
                numberText = minutes.toString()
            }
        }
    }

    val titleContent: @Composable () -> Unit = {
        Text(
            text = "Duration",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    val bodyContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween

        ) {
            OutlinedTextField(
                value = numberText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        numberText = newValue
                    }
                },
                label = { Text("Value") },
                placeholder = { Text("E.g. 100") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(Modifier.height(16.dp))

            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text(if (useHours) "Hours" else "Minutes")
                }


                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Hours") },
                        onClick = {
                            useHours = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Minutes") },
                        onClick = {
                            useHours = false
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    GeneralAlertDialog(
        title = titleContent,
        content = bodyContent,
        onDismiss = onDismiss,
        onConfirm = {
            val numberInt = numberText.toIntOrNull() ?: 0
            val totalMinutes = if (useHours) numberInt * 60 else numberInt
            if (totalMinutes <= 0) {
                errorMessage = "Duration must be greater than 0."
                return@GeneralAlertDialog
            }
            onReady(DurationPlan(totalMinutes))
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