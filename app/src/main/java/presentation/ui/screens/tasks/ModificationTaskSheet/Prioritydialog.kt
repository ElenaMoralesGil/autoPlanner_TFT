package com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Priority

@Composable
fun PriorityDialog(
    currentPriority: Priority,
    onDismiss: () -> Unit,
    onSelectPriority: (Priority) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Priority") },
        text = {
            Column {
                Priority.entries.forEach { priority ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPriority(priority) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (priority == currentPriority),
                            onClick = { onSelectPriority(priority) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = priority.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}