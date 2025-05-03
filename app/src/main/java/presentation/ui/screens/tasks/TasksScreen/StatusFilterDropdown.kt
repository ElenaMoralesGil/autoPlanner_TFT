package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.elena.autoplanner.R
import com.elena.autoplanner.presentation.states.TaskStatus

@Composable
fun StatusFilterDropdown(
    currentStatus: TaskStatus,
    onSelected: (TaskStatus) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_filter),
                contentDescription = "Status filter",
                tint = if (currentStatus != TaskStatus.ALL) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary
            )
        }

        DropdownMenu(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            TaskStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName) },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(status.iconRes()),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

private fun TaskStatus.iconRes() = when (this) {
    TaskStatus.COMPLETED -> R.drawable.ic_completed
    TaskStatus.UNCOMPLETED -> R.drawable.ic_uncompleted
    else -> R.drawable.ic_lists
}
