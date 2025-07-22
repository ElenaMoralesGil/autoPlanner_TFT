package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.Task
import java.time.format.DateTimeFormatter

@Composable
fun ConflictResolutionCard(
    conflict: ConflictItem,
    options: List<ResolutionOption>,
    selectedOption: ResolutionOption?,
    onOptionSelected: (ResolutionOption) -> Unit,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    "Conflict",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Conflict: ${conflict.reason}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    conflict.conflictTime?.let {
                        Text(
                            "Around: ${it.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(start = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Involved Tasks:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                conflict.conflictingTasks.forEach { task ->
                    val displayStartTime = conflict.conflictTime?.toLocalTime() ?: task.startTime
                    val displayEndTime =
                        displayStartTime.plusMinutes(task.effectiveDurationMinutes.toLong())
                    ReviewTaskCard(
                        task = task,
                        startTime = displayStartTime,
                        endTime = displayEndTime,
                        isFlaggedForManualEdit = tasksFlaggedForManualEdit.contains(task.id),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onTaskClick(task) },
                        isConflicted = true
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            selectedOption?.toDisplayString() ?: "Select Action",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.toDisplayString()) },
                                onClick = { onOptionSelected(option); expanded = false },
                                trailingIcon = if (option == selectedOption) {
                                    { Icon(Icons.Filled.Check, "Selected", Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}