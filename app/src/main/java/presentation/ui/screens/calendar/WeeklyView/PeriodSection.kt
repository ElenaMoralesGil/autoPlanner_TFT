package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Task
import java.time.LocalDate

@Composable
fun PeriodSection(
    title: String,
    tasks: List<Task>,
    weekDays: List<LocalDate>,
    onTaskSelected: (Task) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(1.dp, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(48.dp))

                Row(modifier = Modifier.weight(1f)) {
                    weekDays.forEach { date ->
                        val tasksForDay = tasks.filter {
                            it.startDateConf?.dateTime?.toLocalDate() == date
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (tasksForDay.isNotEmpty()) {
                                Column {
                                    tasksForDay.forEach { task ->
                                        PeriodTaskItem(
                                            task = task,
                                            onTaskSelected = onTaskSelected,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp, horizontal = 2.dp)
                                                .shadow(1.dp, shape = RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}