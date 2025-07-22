package com.elena.autoplanner.presentation.ui.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.tasks.RepeatTaskDeleteOption

@Composable
fun RepeatTaskDeleteDialog(
    task: Task,
    onOptionSelected: (RepeatTaskDeleteOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOption by remember { mutableStateOf<RepeatTaskDeleteOption?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Delete Repeating Task",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "\"${task.name}\" is a repeating task. What would you like to delete?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatTaskDeleteOption.values().forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedOption == option),
                                    onClick = { selectedOption = option }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedOption == option),
                                onClick = { selectedOption = option },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = when (option) {
                                        RepeatTaskDeleteOption.THIS_INSTANCE_ONLY -> "Just This Task"
                                        RepeatTaskDeleteOption.THIS_AND_FUTURE -> "This and Future Tasks"
                                        RepeatTaskDeleteOption.ALL_INSTANCES -> "All Tasks in Series"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = when (option) {
                                        RepeatTaskDeleteOption.THIS_INSTANCE_ONLY -> "Delete only this occurrence"
                                        RepeatTaskDeleteOption.THIS_AND_FUTURE -> "Delete this and all future occurrences"
                                        RepeatTaskDeleteOption.ALL_INSTANCES -> "Delete all occurrences (past and future)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            selectedOption?.let { option ->
                                onOptionSelected(option)
                            }
                        },
                        enabled = selectedOption != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}