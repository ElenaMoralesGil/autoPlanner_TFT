package com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.presentation.ui.screens.calendar.getPriorityColor
import com.elena.autoplanner.presentation.utils.DateTimeFormatters
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatDurationForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatReminderForDisplay
import com.elena.autoplanner.presentation.utils.DateTimeFormatters.formatRepeatForDisplay
import java.util.Locale

@Composable
fun TaskConfigDisplay(
    startDate: TimePlanning?,
    endDate: TimePlanning?,
    duration: DurationPlan?,
    reminder: ReminderPlan?,
    repeat: RepeatPlan?,
    priority: Priority,
    listName: String?,
    sectionName: String?,
    allowSplitting: Boolean? = null,
    listColor: Color?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            startDate?.let {
                ConfigItem(
                    painter = painterResource(R.drawable.ic_calendar),
                    label = "Starts",
                    value = DateTimeFormatters.formatDateTimeWithPeriod(it)
                )
            }

            endDate?.let {
                ConfigItem(
                    painter = painterResource(R.drawable.ic_calendar),
                    label = "Ends",
                    value = DateTimeFormatters.formatDateTimeWithPeriod(it)
                )
            }

            duration?.takeIf { it.totalMinutes != null && it.totalMinutes > 0 }
                ?.let { 
                ConfigItem(
                    painter = painterResource(R.drawable.ic_duration),
                    label = "Duration",
                    value = formatDurationForDisplay(it)
                )
                    if (it.totalMinutes!! >= 30 && allowSplitting != null) {
                        val splittingText = when (allowSplitting) {
                            null -> "Auto splitting"
                            true -> "Allow splitting"
                            false -> "Keep together"
                        }

                        val splittingIcon = when (allowSplitting) {
                            null -> Icons.Outlined.PlayArrow
                            true -> Icons.Outlined.Check
                            false -> Icons.Outlined.Clear
                        }

                        ConfigDisplayItem(
                            icon = splittingIcon,
                            label = "Splitting",
                            value = splittingText,
                            modifier = Modifier.padding(start = 8.dp) 
                        )
                    }
                }

            reminder?.takeIf { it.mode != ReminderMode.NONE }?.let { 
                ConfigItem(
                    painter = painterResource(R.drawable.ic_reminder),
                    label = "Reminder",
                    value = formatReminderForDisplay(it)
                )
            }

            repeat?.takeIf { it.frequencyType != com.elena.autoplanner.domain.models.FrequencyType.NONE }
                ?.let { 
                ConfigItem(
                    painter = painterResource(R.drawable.ic_repeat),
                    label = "Repeat",
                    value = formatRepeatForDisplay(it)
                )
            }
            if (priority != Priority.NONE) {
                ConfigItem(
                    painter = painterResource(R.drawable.priority),
                    label = "Priority",
                    value = priority.name.lowercase(Locale.ROOT)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    color = getPriorityColor(priority)
                )
            }

            listName?.let { name ->
                val displayValue = if (sectionName != null) "$name / $sectionName" else name
                ConfigItem(
                    painter = painterResource(R.drawable.ic_lists),
                    label = "List",
                    value = displayValue,
                    color = listColor ?: MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun ConfigDisplayItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}