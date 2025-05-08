package com.elena.autoplanner.widgets

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.repositories.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import androidx.glance.Image
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import kotlinx.coroutines.flow.firstOrNull


class WeeklyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeeklyWidget()
}

class WeeklyWidget : GlanceAppWidget(), KoinComponent {
    private val taskRepository: TaskRepository by inject()
    private val userRepository: UserRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val today = LocalDate.now()
            val weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val user: User? by produceState<User?>(initialValue = null) {
                value = userRepository.getCurrentUser().firstOrNull()
            }
            val tasks: List<Task> by produceState<List<Task>>(initialValue = emptyList(), user, weekStartDate) {
                value = taskRepository.getTasksForWeek(weekStartDate, user?.uid)
            }
            WeeklyWidgetContent(context = context, tasks = tasks, weekStartDate = weekStartDate, glanceId = id)
        }
    }
}

@Composable
fun WeeklyWidgetContent(context: Context, tasks: List<Task>, weekStartDate: LocalDate, glanceId: GlanceId) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEE d")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background_weekly)) // Create a drawable
            .padding(8.dp)
            .cornerRadius(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.widget_title_weekly),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorProvider(Color(AndroidColor.BLACK)))
            )
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                modifier = GlanceModifier.size(20.dp).clickable(onClick = actionRunCallback<RefreshAction>())
            )
        }
        Text(
            text = "${weekStartDate.format(DateTimeFormatter.ofPattern("MMM d"))} - ${weekStartDate.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}",
            style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color(AndroidColor.DKGRAY))),
            modifier = GlanceModifier.padding(bottom = 6.dp)
        )

        if (tasks.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize().defaultWeight(), contentAlignment = Alignment.Center) {
                Text(context.getString(R.string.widget_no_tasks), style = TextStyle(color = ColorProvider(Color(AndroidColor.GRAY))))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                items(tasks, itemId = { it.id.toLong() }) { task ->
                    WeeklyTaskWidgetItem(task = task, dayFormatter = dayFormatter)
                    Spacer(GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun WeeklyTaskWidgetItem(task: Task, dayFormatter: DateTimeFormatter) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_item_background))
            .padding(vertical = 4.dp, horizontal = 6.dp)
            .cornerRadius(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        task.startDateConf.dateTime?.let {
            Text(
                text = it.toLocalDate().format(dayFormatter),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ColorProvider(Color(AndroidColor.DKGRAY))),
                modifier = GlanceModifier.padding(end = 6.dp)
            )
        }
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = task.name,
                style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = androidx.glance.unit.ColorProvider(
                    Color(AndroidColor.BLACK)
                )
                ),
                maxLines = 1
            )
            task.startDateConf.dateTime?.let {
                Text(
                    text = it.format(timeFormatter),
                    style = TextStyle(fontSize = 12.sp, color = androidx.glance.unit.ColorProvider(Color(AndroidColor.DKGRAY)))
                )
            }
        }
    }
}