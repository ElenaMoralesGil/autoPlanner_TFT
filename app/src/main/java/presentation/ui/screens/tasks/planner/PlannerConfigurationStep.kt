package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.DayOrganization
import com.elena.autoplanner.domain.models.OverdueTaskHandling
import com.elena.autoplanner.domain.models.PrioritizationStrategy
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState

@Composable
fun PlannerConfigurationStep(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
) {

    QuestionnaireSectionCard(title = "Availability Hours", icon = Icons.Outlined.DateRange) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            ModernTimeSelectorButton(
                time = state.workStartTime,
                label = "Start Time",
                onClick = onStartTimeClick,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            ModernTimeSelectorButton(
                time = state.workEndTime,
                label = "End Time",
                onClick = onEndTimeClick,
                modifier = Modifier.weight(1f)
            )
        }
    }


    QuestionnaireSectionCard(title = "Scheduling Scope", icon = Icons.Outlined.DateRange) {
        SingleChoiceChipGroup(
            options = ScheduleScope.entries,
            selectedOption = state.scheduleScope,
            onOptionSelected = { onIntent(PlannerIntent.SelectScheduleScope(it)) },
            labelSelector = { it.toDisplayString() },
        )
    }


    QuestionnaireSectionCard(title = "Prioritization Strategy", icon = Icons.Outlined.List) {
        SingleChoiceChipGroup(
            options = PrioritizationStrategy.entries,
            selectedOption = state.selectedPriority,
            onOptionSelected = { priority ->
                onIntent(PlannerIntent.SelectPriority(priority))
            },
            labelSelector = { it.toDisplayString() },
        )
    }


    QuestionnaireSectionCard(title = "Day Organization Style", icon = Icons.Outlined.List) {
        SingleChoiceChipGroup(
            options = DayOrganization.entries,
            selectedOption = state.selectedDayOrganization,
            onOptionSelected = { organization ->
                onIntent(PlannerIntent.SelectDayOrganization(organization))
            },
            labelSelector = { it.toDisplayString() }

        )
    }


    BooleanSettingCard(
        title = "Task Splitting",
        icon = Icons.Outlined.Menu,
        description = "Allow splitting long tasks with durations?",
        checked = state.allowSplitting,
        onCheckedChange = { allow -> onIntent(PlannerIntent.SelectAllowSplitting(allow)) }
    )



    AnimatedVisibility(visible = state.numOverdueTasks > 0) {
        QuestionnaireSectionCard(
            title = "Overdue Tasks (${state.numOverdueTasks})",
            icon = Icons.Outlined.Warning
        ) {
            Text(
                text = "How should we handle tasks past their deadline?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            SingleChoiceChipGroup(
                options = OverdueTaskHandling.entries,
                selectedOption = state.selectedOverdueHandling,
                onOptionSelected = { handling ->
                    onIntent(PlannerIntent.SelectOverdueHandling(handling))
                },
                labelSelector = { it.toDisplayString() }
            )
        }
    }


    AnimatedVisibility(visible = state.numOverdueTasks <= 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "No overdue tasks found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}