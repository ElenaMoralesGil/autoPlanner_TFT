package com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Subtask

@Composable
fun SubtasksSection(
    subtasks: List<Subtask>,
    onSubtaskToggled: (subtaskId: Int, isCompleted: Boolean) -> Unit,
    onSubtaskAdded: (String) -> Unit,
    onSubtaskDeleted: (Subtask) -> Unit,
    showDeleteButton: Boolean,
    showAddButton: Boolean,
    errorMessage: String?, 
    modifier: Modifier = Modifier,
) {

    var newSubtaskText by remember { mutableStateOf("") }

    var showEmptyInputError by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Subtasks",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .heightIn(min = 0.dp, max = 200.dp) 
        ) {
            items(subtasks, key = { it.id }) { subtask ->
                SubtaskItem(
                    subtask = subtask,
                    showDeleteButton = showDeleteButton,
                    onCheckedChange = { checked ->
                        onSubtaskToggled(subtask.id, checked)
                    },
                    onDelete = {
                        onSubtaskDeleted(subtask)
                    }
                )
            }
        }

        if (showAddButton) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = {
                        newSubtaskText = it
                        if (it.isNotBlank()) {
                            showEmptyInputError = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add subtask") },
                    isError = showEmptyInputError
                )
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            onSubtaskAdded(newSubtaskText)
                            newSubtaskText = ""
                            showEmptyInputError = false
                        } else {
                            showEmptyInputError = true
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add subtask")
                }
            }

            if (showEmptyInputError) {
                Text(
                    text = "Subtask name required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}