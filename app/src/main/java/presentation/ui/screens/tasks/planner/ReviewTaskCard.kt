package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ReviewTaskCard(
    task: Task,
    startTime: LocalTime,
    endTime: LocalTime,
    isFlaggedForManualEdit: Boolean,
    isConflicted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val priorityColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
        Priority.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        Priority.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    var cardColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.surface
    }

    var borderColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.tertiary
        else -> priorityColor.copy(alpha = 0.5f)
    }

    var textColor = when {
        isFlaggedForManualEdit -> MaterialTheme.colorScheme.onTertiaryContainer
        task.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    if (isConflicted) {
        cardColor =
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        textColor =
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
    }

    val borderStroke = BorderStroke(
        width = if (isFlaggedForManualEdit || isConflicted) 1.5.dp else 1.dp,
        color = borderColor
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(cardColor)
            .border(border = borderStroke, shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        color = if (isConflicted) borderColor else priorityColor,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(5.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFlaggedForManualEdit) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit, "Edit Required", Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (isConflicted) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Warning, "Conflict", Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Text(
                    "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            if (task.isCompleted && !isFlaggedForManualEdit && !isConflicted) {
                Icon(
                    Icons.Default.Check, "Completed", Modifier
                        .size(14.dp)
                        .padding(start = 2.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}