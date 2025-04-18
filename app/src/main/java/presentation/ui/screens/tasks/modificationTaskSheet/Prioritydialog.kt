package com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector // Keep ImageVector for CheckCircle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Priority
import java.util.Locale

@Composable
fun PriorityDialog(
    currentPriority: Priority,
    onDismiss: () -> Unit,
    onSelectPriority: (Priority) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Select Priority",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { priority ->
                    PriorityOptionItem(
                        priority = priority,
                        isSelected = (priority == currentPriority),
                        onClick = { onSelectPriority(priority) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PriorityOptionItem(
    priority: Priority,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val priorityVisuals = priority.getVisuals() // Get the updated visuals
    val backgroundColor = if (isSelected) {
        priorityVisuals.color.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    // Content color is the priority color when selected, otherwise default onSurface
    val contentColor = if (isSelected) {
        priorityVisuals.color
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    // Icon tint follows content color, except for the default priority icon when not selected
    val iconTint = if (isSelected) {
        contentColor // Use priority color for CheckCircle
    } else {
        priorityVisuals.color // Use priority color for the flag icon
    }

    // Determine which icon to show
    val iconToShowVector =
        if (isSelected) Icons.Filled.CheckCircle else null // Only CheckCircle uses vector
    val iconToShowResId =
        if (isSelected) null else priorityVisuals.iconResId // Use flag resource otherwise

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(
            1.dp,
            priorityVisuals.color.copy(alpha = 0.5f)
        ) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Conditional Icon rendering
            when {
                iconToShowVector != null -> { // For CheckCircle when selected
                    Icon(
                        imageVector = iconToShowVector,
                        contentDescription = "${priority.name} Priority Selected",
                        tint = iconTint, // Use priority color
                        modifier = Modifier.size(24.dp)
                    )
                }

                iconToShowResId != null -> { // For the flag icon when not selected
                    Icon(
                        painter = painterResource(id = iconToShowResId),
                        contentDescription = "${priority.name} Priority",
                        tint = iconTint, // Use priority color for the flag
                        modifier = Modifier.size(24.dp)
                    )
                }

                else -> Spacer(Modifier.size(24.dp)) // Fallback
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = priority.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = contentColor // Text color changes based on selection
            )
        } // End Row
    } // End Surface
}

// Helper data class remains the same
private data class PriorityVisuals(
    val color: Color,
    val iconVector: ImageVector? = null,
    val iconResId: Int? = null,
) {
    init {
        require(iconVector == null || iconResId == null) {
            "Provide either iconVector or iconResId, not both."
        }
        require(iconVector != null || iconResId != null) {
            "Provide either iconVector or iconResId."
        }
    }
}


@Composable
private fun Priority.getVisuals(): PriorityVisuals {
    return when (this) {
        Priority.HIGH -> PriorityVisuals(
            color = MaterialTheme.colorScheme.error,
            iconResId = R.drawable.priority // Use flag icon
        )

        Priority.MEDIUM -> PriorityVisuals(
            color = Color(0xFFFFA500), // Orange
            iconResId = R.drawable.priority // Use flag icon
        )

        Priority.LOW -> PriorityVisuals(
            color = Color(0xFF4CAF50), // Green
            iconResId = R.drawable.priority // Use flag icon
        )

        Priority.NONE -> PriorityVisuals(
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Use a neutral color for tint
            iconResId = R.drawable.priority // Use flag icon
        )
    }
}