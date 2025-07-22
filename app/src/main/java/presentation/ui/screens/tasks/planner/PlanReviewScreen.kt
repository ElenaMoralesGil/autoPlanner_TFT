package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.PlannerStep
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduleScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.states.PlannerState
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun PlanReviewScreen(
    state: PlannerState,
    onIntent: (PlannerIntent) -> Unit,
    onReviewTaskClick: (Task) -> Unit,
) {
    var currentCalendarView by remember { mutableStateOf(CalendarView.DAY) }

    val availableViews = remember(state.scheduleScope) {
        when (state.scheduleScope) {
            ScheduleScope.TODAY, ScheduleScope.TOMORROW -> listOf(CalendarView.DAY)
            else -> listOf(CalendarView.DAY, CalendarView.WEEK)
        }
    }

    LaunchedEffect(availableViews) {
        if (availableViews.size > 1 && state.scheduleScope == ScheduleScope.THIS_WEEK) {
            currentCalendarView = CalendarView.WEEK
        } else {
            currentCalendarView = CalendarView.DAY
        }
    }

    val planStartDate = remember(state.generatedPlan, state.scheduleScope) {
        state.generatedPlan.keys.minOrNull() ?: state.scheduleScope?.let { scope ->
            val today = LocalDate.now()
            when (scope) {
                ScheduleScope.TODAY -> today
                ScheduleScope.TOMORROW -> today.plusDays(1)
                ScheduleScope.THIS_WEEK -> today // Cambiar para mostrar desde hoy hasta +6 días
            }
        } ?: LocalDate.now()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        AnimatedVisibility(visible = state.requiresResolution) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                            alpha = 0.4f
                        )
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val unresolvedExpired =
                            remember(state.expiredTasksToResolve, state.taskResolutions) {
                                state.expiredTasksToResolve.filter { state.taskResolutions[it.id] == null }
                            }
                        if (unresolvedExpired.isNotEmpty()) {
                            ReviewSectionHeader(
                                title = "Action Required: Expired Tasks (${unresolvedExpired.size})",
                                isError = true
                            )
                            unresolvedExpired.forEach { task ->
                                ResolutionCard(
                                    task = task,
                                    options = listOf(
                                        ResolutionOption.MOVE_TO_TOMORROW,
                                        ResolutionOption.MANUALLY_SCHEDULE
                                    ),
                                    selectedOption = state.taskResolutions[task.id],
                                    onOptionSelected = { option ->
                                        onIntent(PlannerIntent.ResolveExpiredTask(task, option))
                                        if (option == ResolutionOption.MANUALLY_SCHEDULE) {
                                            onIntent(PlannerIntent.FlagTaskForManualEdit(task.id))
                                        } else {
                                            onIntent(PlannerIntent.UnflagTaskForManualEdit(task.id))
                                        }
                                    }
                                )
                            }
                        }

                        val unresolvedConflicts =
                            remember(state.conflictsToResolve, state.conflictResolutions) {
                                state.conflictsToResolve.filter { state.conflictResolutions[it.hashCode()] == null }
                            }
                        if (unresolvedConflicts.isNotEmpty()) {
                            ReviewSectionHeader(
                                title = "Action Required: Scheduling Conflicts (${unresolvedConflicts.size})",
                                isError = true
                            )
                            unresolvedConflicts.forEach { conflict ->
                                ConflictResolutionCard(
                                    conflict = conflict,
                                    options = listOf(
                                        ResolutionOption.MOVE_TO_TOMORROW,
                                        ResolutionOption.MANUALLY_SCHEDULE,
                                        ResolutionOption.LEAVE_IT_LIKE_THAT
                                    ),
                                    selectedOption = state.conflictResolutions[conflict.hashCode()],
                                    onOptionSelected = { option ->
                                        onIntent(PlannerIntent.ResolveConflict(conflict, option))
                                        val taskToFlag =
                                            conflict.conflictingTasks.minByOrNull { it.priority.ordinal }
                                        taskToFlag?.let {
                                            if (option == ResolutionOption.MANUALLY_SCHEDULE || option == ResolutionOption.LEAVE_IT_LIKE_THAT) {
                                                onIntent(PlannerIntent.FlagTaskForManualEdit(it.id))
                                            } else {
                                                onIntent(PlannerIntent.UnflagTaskForManualEdit(it.id))
                                            }
                                        }
                                    },
                                    onTaskClick = onReviewTaskClick,
                                    tasksFlaggedForManualEdit = state.tasksFlaggedForManualEdit,
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
                if (state.requiresResolution) {
                    Text(
                        "Please resolve the items above to add the plan",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (availableViews.size > 1) {
                    ReviewViewToggle(
                        availableViews = availableViews,
                        selectedView = currentCalendarView,
                        onViewSelected = { currentCalendarView = it }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .heightIn(min = 200.dp, max = 550.dp)
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (state.generatedPlan.isNotEmpty() || state.conflictsToResolve.isNotEmpty()) {
                    GeneratedPlanReviewView(
                        viewType = currentCalendarView,
                        plan = state.generatedPlan,
                        conflicts = state.conflictsToResolve,
                        resolutions = state.conflictResolutions + state.taskResolutions,
                        startDate = planStartDate,
                        onTaskClick = onReviewTaskClick,
                        tasksFlaggedForManualEdit = state.tasksFlaggedForManualEdit
                    )
                } else if (!state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No tasks scheduled. Try adjusting the options or check conflicts.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (state.isLoading && state.currentStep == PlannerStep.REVIEW_PLAN) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}