package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTopBar(
    state: TaskState,
    onStatusSelected: (TaskStatus) -> Unit,
    onTimeFrameSelected: (TimeFrame) -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Column {
                Text("Tasks", style = MaterialTheme.typography.headlineSmall)
                Text(
                    buildFilterText(state),
                    Modifier.padding(start = 2.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                )
            }
        },
        actions = {
            StatusFilterDropdown(state.filters.status, onStatusSelected)
            TimeFrameFilterDropdown(state.filters.timeFrame, onTimeFrameSelected)
        }
    )
}

@Composable
private fun buildFilterText(state: TaskState): String {
    val filters = listOfNotNull(
        state.filters.timeFrame.takeIf { it != TimeFrame.ALL }?.displayName,
        state.filters.status.takeIf { it != TaskStatus.ALL }?.displayName
    )
    return filters.joinToString(" • ").ifEmpty { "All Tasks" }
}