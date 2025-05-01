package com.elena.autoplanner.presentation.ui.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator

@Composable
fun SectionSelectionDialog(
    isLoading: Boolean,
    listName: String,
    sections: List<TaskSection>,
    selectedSectionId: Long?,
    onDismiss: () -> Unit,
    onSectionSelected: (TaskSection?) -> Unit, // Null for "None"
    onCreateNewSection: () -> Unit,
) {
    GeneralAlertDialog(
        title = { Text("Assign Section in '$listName'") },
        content = {
            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    LazyColumn {
                        // Option to remove from section
                        item {
                            SectionSelectItem(
                                name = "None (No Section)",
                                isSelected = selectedSectionId == null,
                                onClick = { onSectionSelected(null) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                            HorizontalDivider()
                        }

                        // Existing sections
                        items(sections, key = { it.id }) { section ->
                            SectionSelectItem(
                                name = section.name,
                                isSelected = section.id == selectedSectionId,
                                onClick = { onSectionSelected(section) }
                            )
                            HorizontalDivider()
                        }

                        // Option to create new section
                        item {
                            SectionSelectItem(
                                name = "Create New Section...",
                                isSelected = false,
                                onClick = onCreateNewSection,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        onDismiss = onDismiss,
        onConfirm = { /* Confirmation happens via item clicks */ onDismiss() },
        hideDismissButton = true
    )
}

@Composable
private fun SectionSelectItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(16.dp))
        } else {
            // Indent regular sections slightly
            Spacer(Modifier.width(24.dp)) // Adjust as needed
        }

        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected && leadingIcon == null) { // Show check only for actual sections
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}