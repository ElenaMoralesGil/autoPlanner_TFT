package com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val priorityVisuals = priority.getVisuals() 
    val backgroundColor = if (isSelected) {
        priorityVisuals.color.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        priorityVisuals.color
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconTint = if (isSelected) {
        contentColor 
    } else {
        priorityVisuals.color 
    }


    val iconToShowVector =
        if (isSelected) Icons.Filled.CheckCircle else null 
    val iconToShowResId =
        if (isSelected) null else priorityVisuals.iconResId 

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

            when {
                iconToShowVector != null -> { 
                    Icon(
                        imageVector = iconToShowVector,
                        contentDescription = "${priority.name} Priority Selected",
                        tint = iconTint, 
                        modifier = Modifier.size(24.dp)
                    )
                }

                iconToShowResId != null -> { 
                    Icon(
                        painter = painterResource(id = iconToShowResId),
                        contentDescription = "${priority.name} Priority",
                        tint = iconTint, 
                        modifier = Modifier.size(24.dp)
                    )
                }

                else -> Spacer(Modifier.size(24.dp)) 
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = priority.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = contentColor 
            )
        }
    } 
}


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
            iconResId = R.drawable.priority 
        )

        Priority.MEDIUM -> PriorityVisuals(
            color = Color(0xFFFFA500),
            iconResId = R.drawable.priority 
        )

        Priority.LOW -> PriorityVisuals(
            color = Color(0xFF4CAF50),
            iconResId = R.drawable.priority 
        )

        Priority.NONE -> PriorityVisuals(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            iconResId = R.drawable.priority 
        )
    }
}