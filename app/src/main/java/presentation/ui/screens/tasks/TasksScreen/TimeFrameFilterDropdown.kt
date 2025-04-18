package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

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
import androidx.compose.ui.res.painterResource
import com.elena.autoplanner.R
import com.elena.autoplanner.presentation.states.TimeFrame

@Composable
fun TimeFrameFilterDropdown(
    currentTimeFrame: TimeFrame,
    onSelected: (TimeFrame) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar),
                contentDescription = "Time filter",
                tint = if (currentTimeFrame != TimeFrame.ALL) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeFrame.entries.forEach { timeFrame ->
                DropdownMenuItem(
                    text = { Text(timeFrame.displayName) },
                    onClick = {
                        onSelected(timeFrame)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(timeFrame.iconRes()),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

private fun TimeFrame.iconRes() = when (this) {
    TimeFrame.TODAY, TimeFrame.WEEK, TimeFrame.MONTH -> R.drawable.ic_lists
    else -> R.drawable.ic_calendar
}