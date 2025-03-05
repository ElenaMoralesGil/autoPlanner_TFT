package com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Subtask

@Composable
fun SubtaskItem(
    subtask: Subtask,
    showDeleteButton: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
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
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )

        if (showDeleteButton) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete subtask",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}