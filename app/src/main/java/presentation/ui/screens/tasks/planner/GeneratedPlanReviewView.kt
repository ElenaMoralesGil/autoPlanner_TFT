package com.elena.autoplanner.presentation.ui.screens.tasks.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ResolutionOption
import com.elena.autoplanner.domain.models.ScheduledTaskItem
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.ui.screens.calendar.CalendarView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun GeneratedPlanReviewView(
    viewType: CalendarView,
    plan: Map<LocalDate, List<ScheduledTaskItem>>,
    conflicts: List<ConflictItem>,
    resolutions: Map<Int, ResolutionOption>,
    startDate: LocalDate,
    onTaskClick: (Task) -> Unit,
    tasksFlaggedForManualEdit: Set<Int>,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (viewType) {
            CalendarView.WEEK -> {
                // Usar startDate directamente en lugar de forzar lunes
                ReviewWeeklyViewContent(
                    startDate = startDate,
                    plan = plan,
                    conflicts = conflicts,
                    resolutions = resolutions,
                    onTaskClick = onTaskClick,
                    tasksFlaggedForManualEdit = tasksFlaggedForManualEdit,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            CalendarView.DAY -> {
                ReviewDailyViewContent(
                    date = startDate,
                    items = plan[startDate] ?: emptyList(),
                    conflicts = conflicts.filter {
                        it.conflictTime?.toLocalDate() == startDate || it.conflictingTasks.any { t -> t.startDateConf?.dateTime?.toLocalDate() == startDate }
                    },
                    resolutions = resolutions,
                    onTaskClick = onTaskClick,
                    tasksFlaggedForManualEdit = tasksFlaggedForManualEdit,
                    modifier = Modifier.fillMaxHeight()
                )
            }

            CalendarView.MONTH -> {

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Monthly view preview not available.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Switch to Day or Week view.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}