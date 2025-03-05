package com.elena.autoplanner.presentation.ui.screens.tasks.ModificationTaskSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
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
    priority: Priority
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

            duration?.let {
                ConfigItem(
                    painter = painterResource(R.drawable.ic_duration),
                    label = "Duration",
                    value = formatDurationForDisplay(it)
                )
            }

            reminder?.let {
                ConfigItem(
                    painter = painterResource(R.drawable.ic_reminder),
                    label = "Reminder",
                    value = formatReminderForDisplay(it)
                )
            }

            repeat?.let {
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
        }
    }
}