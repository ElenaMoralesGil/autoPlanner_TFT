

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
import androidx.compose.runtime.* 
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
    currentSelectedSectionId: Long?, 
    onDismiss: () -> Unit,
    onConfirmSelection: (sectionId: Long?) -> Unit, 
    onCreateNewSection: () -> Unit,
) {

    var temporarySelectedSectionId by remember { mutableStateOf(currentSelectedSectionId) }

    GeneralAlertDialog(
        title = { Text("Assign Section in '$listName'") },
        content = {
            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    LazyColumn {

                        item {
                            SectionSelectItem(
                                name = "None (No Section)",
                                isSelected = temporarySelectedSectionId == null,
                                onClick = { temporarySelectedSectionId = null }, 
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


                        items(sections, key = { it.id }) { section ->
                            SectionSelectItem(
                                name = section.name,
                                isSelected = section.id == temporarySelectedSectionId,
                                onClick = { temporarySelectedSectionId = section.id } 
                            )
                            HorizontalDivider()
                        }


                        item {
                            SectionSelectItem(
                                name = "Create New Section...",
                                isSelected = false,
                                onClick = onCreateNewSection 
                            )
                        }
                    }
                }
            }
        },
        onDismiss = onDismiss,
        onConfirm = {

            onConfirmSelection(temporarySelectedSectionId)
            onDismiss() 
        }

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
            Spacer(Modifier.width(24.dp)) 
        }

        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected && leadingIcon == null) { 
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}