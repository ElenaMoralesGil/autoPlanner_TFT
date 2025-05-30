

package com.elena.autoplanner.presentation.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.* 
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog 
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator

@Composable
fun ListSelectionDialog(
    isLoading: Boolean,
    lists: List<TaskList>,
    currentSelectedListId: Long?, 
    onDismiss: () -> Unit,
    onConfirmSelection: (listId: Long?) -> Unit, 
    onCreateNewList: () -> Unit,
) {

    var temporarySelectedListId by remember { mutableStateOf(currentSelectedListId) }

    GeneralAlertDialog(
        title = { Text("Assign to List") },
        content = {
            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    LazyColumn {

                        item {
                            ListSelectItem(
                                name = "None (Remove from List)",
                                color = MaterialTheme.colorScheme.outline,
                                isSelected = temporarySelectedListId == null,
                                onClick = { temporarySelectedListId = null }, 
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


                        items(lists, key = { it.id }) { list ->
                            ListSelectItem(
                                name = list.name,
                                color = try {
                                    Color(android.graphics.Color.parseColor(list.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.secondary
                                },
                                isSelected = list.id == temporarySelectedListId,
                                onClick = { temporarySelectedListId = list.id } 
                            )
                            HorizontalDivider()
                        }


                        item {
                            ListSelectItem(
                                name = "Create New List...",
                                color = MaterialTheme.colorScheme.primary,
                                isSelected = false,
                                onClick = onCreateNewList 
                            )
                        }
                    }
                }
            }
        },
        onDismiss = onDismiss,
        onConfirm = {

            onConfirmSelection(temporarySelectedListId)
            onDismiss() 
        },


        )
}


@Composable
private fun ListSelectItem(
    name: String,
    color: Color,
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
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
        }
        Spacer(Modifier.width(16.dp))
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