package com.elena.autoplanner.presentation.ui.screens.more.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.repositories.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale 

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
            DailyWidgetContent(context = context, tasks = tasks, date = today)
        }
    }
}

@Composable
fun DailyWidgetContent(context: Context, tasks: List<Task>, date: LocalDate) {
    val titleDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.widgetBackground) 
            .padding(16.dp)
            .cornerRadius(24.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.widget_title_daily),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = WidgetColors.titleText 
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_refresh), 
                contentDescription = context.getString(R.string.widget_refresh_description),
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(onClick = actionRunCallback<RefreshAction>())
            )
        }
        Text(
            text = date.format(titleDateFormatter).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            style = TextStyle(
                fontSize = 15.sp,
                color = WidgetColors.secondaryText, 
                fontWeight = FontWeight.Normal
            ),
            modifier = GlanceModifier.padding(bottom = 12.dp)
        )

        if (tasks.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    context.getString(R.string.widget_no_tasks_today),
                    style = TextStyle(
                        color = WidgetColors.tertiaryText, 
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                items(
                    items = tasks,
                    itemId = { task -> task.id.hashCode().toLong() }
                ) { task ->
                    DailyTaskWidgetItem(task = task)
                    Spacer(GlanceModifier.height(8.dp))
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
            .background(WidgetColors.itemBackground) 
            .cornerRadius(16.dp)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(

            modifier = GlanceModifier
                .size(width = 4.dp, height = 32.dp)
                .background(WidgetColors.accent) 
                .cornerRadius(2.dp),
            contentAlignment = Alignment.Center,
            content = {}
        ) 

        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = task.name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = WidgetColors.primaryText 
                ),
                maxLines = 2
            )
            task.startDateConf.dateTime?.let {
                Text(
                    text = it.format(timeFormatter),
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = WidgetColors.secondaryText 
                    ),
                    modifier = GlanceModifier.padding(top = 3.dp)
                )
            }
        }
    }
}