package com.elena.autoplanner.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.glance.Image
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.color.ColorProvider

import android.graphics.Color as AndroidColor


class DailyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyWidget()
}

class DailyWidget : GlanceAppWidget(), KoinComponent {
    private val taskRepository: TaskRepository by inject()
    private val userRepository: UserRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val today = LocalDate.now()
            val user: User? by produceState<User?>(initialValue = null) {
                value = userRepository.getCurrentUser().firstOrNull()
            }
            val tasks: List<Task> by produceState<List<Task>>(initialValue = emptyList(), user, today) {
                value = taskRepository.getTasksForDate(today, user?.uid)
            }
            DailyWidgetContent(context = context, tasks = tasks, date = today, glanceId = id)
        }
    }
}

@Composable
fun DailyWidgetContent( context: Context, tasks: List<Task>, date: LocalDate, glanceId: GlanceId) {
    val titleDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background_daily)) // Create a drawable
            .padding(8.dp)
            .cornerRadius(16.dp) // Apply corner radius
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.widget_title_daily),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = androidx.glance.unit.ColorProvider(
                    Color(AndroidColor.BLACK)
                )
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_refresh), // Create a refresh icon
                contentDescription = "Refresh",
                modifier = GlanceModifier.size(20.dp).clickable(onClick = actionRunCallback<RefreshAction>())
            )
        }
        Text(
            text = date.format(titleDateFormatter),
            style = TextStyle(fontSize = 12.sp, color = androidx.glance.unit.ColorProvider(
                Color(
                    AndroidColor.DKGRAY
                )
            )
            ),
            modifier = GlanceModifier.padding(bottom = 6.dp)
        )

        if (tasks.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize().defaultWeight(), contentAlignment = Alignment.Center) {
                Text(context.getString(R.string.widget_no_tasks), style = TextStyle(color = androidx.glance.unit.ColorProvider(
                    Color(AndroidColor.GRAY)
                )
                ))
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                items(tasks, itemId = { it.id.toLong() }) { task ->
                    DailyTaskWidgetItem(task = task)
                    Spacer(GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun DailyTaskWidgetItem(task: Task) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(R.drawable.widget_item_background)) // Create a drawable
            .padding(vertical = 4.dp, horizontal = 6.dp)
            .cornerRadius(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                    style = TextStyle(fontSize = 12.sp, color = androidx.glance.unit.ColorProvider(
                        Color(AndroidColor.DKGRAY)
                    )
                    )
                )
            }
        }
        // Optionally, add a checkbox or priority indicator
    }
}