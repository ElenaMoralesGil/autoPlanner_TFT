package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task

/*** Todo:
 * fix added subtask doesnt show inmidiatly after adding in subtasklist
 * fix that everytime when adding task the whole screen seems to reload
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSubtaskAdded: (String) -> Unit,
    onSubtaskToggled: (Subtask, Boolean) -> Unit
) {
    var newSubtaskText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    task.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }

            // Task Details
            TaskConfigDisplay(
                startDate = task.startDateConf,
                endDate = task.endDateConf,
                duration = task.durationConf,
                reminder = task.reminderPlan,
                repeat = task.repeatPlan,
                priority = task.priority
            )

            // Subtasks Section
            Text("Subtasks", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(task.subtasks) { subtask ->
                    SubtaskItem(
                        subtask = subtask,
                        onCheckedChange = { checked ->
                            onSubtaskToggled(subtask, checked)
                        }
                    )
                }
            }

            // Add Subtask Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add subtask") }
                )
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            onSubtaskAdded(newSubtaskText)
                            newSubtaskText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, "Add subtask")
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onEdit) {
                    Text("Edit Task")
                }
            }
        }
    }
}

@Composable
fun SubtaskItem(
    subtask: Subtask,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = subtask.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}