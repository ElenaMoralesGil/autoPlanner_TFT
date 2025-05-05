// src/main/java/presentation/ui/screens/more/SectionSelectionDialog.kt

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
import androidx.compose.runtime.* // Add remember import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog // Keep using this
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator

@Composable
fun SectionSelectionDialog(
    isLoading: Boolean,
    listName: String,
    sections: List<TaskSection>,
    currentSelectedSectionId: Long?, // Renamed for clarity
    onDismiss: () -> Unit,
    onConfirmSelection: (sectionId: Long?) -> Unit, // Changed callback name
    onCreateNewSection: () -> Unit,
) {
    // State to hold the selection *within* the dialog before confirming
    var temporarySelectedSectionId by remember { mutableStateOf(currentSelectedSectionId) }

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
                                isSelected = temporarySelectedSectionId == null, // Use temporary state
                                onClick = { temporarySelectedSectionId = null }, // Update temporary state
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
                                isSelected = section.id == temporarySelectedSectionId, // Use temporary state
                                onClick = { temporarySelectedSectionId = section.id } // Update temporary state
                            )
                            HorizontalDivider()
                        }

                        // Option to create new section
                        item {
                            SectionSelectItem(
                                name = "Create New Section...",
                                isSelected = false, // Create is never "selected"
                                onClick = onCreateNewSection // Trigger create flow immediately
                            )
                        }
                    }
                }
            }
        },
        onDismiss = onDismiss,
        onConfirm = {
            // Only call the final callback when the "Ready" button is pressed
            onConfirmSelection(temporarySelectedSectionId)
            onDismiss() // Close dialog after confirming
        }
        // Keep default "Cancel" and "Ready" buttons
    )
}

// SectionSelectItem remains largely the same
@Composable
private fun SectionSelectItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit, // This now updates temporary state
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Click updates temp state
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.width(24.dp)) // Keep indentation
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