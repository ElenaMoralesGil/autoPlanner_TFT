package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.presentation.states.TaskListState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTopBar(
    state: TaskListState,
    currentListName: String?, 
    onStatusSelected: (TaskStatus) -> Unit,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    onPlannerClick: () -> Unit,
    onShowAllTasks: () -> Unit,
    onEditList: () -> Unit,
    onEditSections: () -> Unit, 
) {
    var showMenu by remember { mutableStateOf(false) }
    val isSpecificListSelected = state.currentListId != null

    val titleText = buildString {
        append(currentListName ?: "Tasks") 
        state.currentSectionName?.let { sectionName ->
            append(" / $sectionName") 
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Column {

                Text(
                    text = titleText, 
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    buildFilterText(state),
                    Modifier.padding(start = 2.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        navigationIcon = {

            if (isSpecificListSelected) {
                IconButton(onClick = onShowAllTasks) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Show All Tasks")
                }
            }
        },
        actions = {
            StatusFilterDropdown(state.statusFilter, onStatusSelected)
            TimeFrameFilterDropdown(state.timeFrameFilter, onTimeFrameSelected)
            IconButton(onClick = onPlannerClick) {
                Icon(
                    painter = painterResource(id = R.drawable.autoplanner),
                    contentDescription = "Auto Plan Schedule",
                )
            }


            if (isSpecificListSelected) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "List Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit List") },
                            onClick = { onEditList(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Sections") },
                            onClick = { onEditSections(); showMenu = false }
                        )


                    }
                }
            }
        }
    )
}

@Composable
private fun buildFilterText(state: TaskListState): String {
    val filters = listOfNotNull(
        state.timeFrameFilter.takeIf { it != TimeFrame.ALL }?.displayName,
        state.statusFilter.takeIf { it != TaskStatus.ALL }?.displayName
    )
    return filters.joinToString(" • ").ifEmpty { "All Tasks" }
}