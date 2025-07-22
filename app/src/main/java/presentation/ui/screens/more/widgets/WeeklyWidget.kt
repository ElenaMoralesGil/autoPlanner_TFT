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
import androidx.glance.layout.width
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.repositories.UserRepository
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

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
            WeeklyWidgetContent(context = context, tasks = tasks, weekStartDate = weekStartDate)
        }
    }
}

@Composable
fun WeeklyWidgetContent(context: Context, tasks: List<Task>, weekStartDate: LocalDate) {
    val today = LocalDate.now()
    val weekEndDate = weekStartDate.plusDays(6)
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val weekRangeFormatter = DateTimeFormatter.ofPattern("d")

    val tasksByDay: Map<LocalDate, List<Task>> by produceState(emptyMap(), tasks) {
        value = tasks
            .filter { it.startDateConf?.dateTime != null }
            .groupBy { it.startDateConf?.dateTime!!.toLocalDate() }
            .toSortedMap()
    }
    val daysInWeek = (0..6).map { weekStartDate.plusDays(it.toLong()) }

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
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_title_weekly),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = WidgetColors.titleText 
                    )
                )
                Text(
                    text = "${weekStartDate.format(weekRangeFormatter)} - ${
                        weekEndDate.format(
                            weekRangeFormatter
                        )
                    } ${
                        weekStartDate.format(monthYearFormatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                    }",
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = WidgetColors.secondaryText, 
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = GlanceModifier.padding(top = 2.dp)
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_refresh), 
                contentDescription = context.getString(R.string.widget_refresh_description),
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(onClick = actionRunCallback<RefreshAction>())
            )
        }
        Spacer(GlanceModifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    context.getString(R.string.widget_no_tasks_week),
                    style = TextStyle(color = WidgetColors.tertiaryText, fontSize = 14.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                daysInWeek.forEachIndexed { index, day ->
                    val tasksForThisDay = tasksByDay[day].orEmpty()
                    val isCurrentDay = day.isEqual(today)

                    item(itemId = "header-${day.toEpochDay()}".hashCode().toLong()) {
                        WeeklyDayHeader(
                            date = day,
                            isToday = isCurrentDay,
                            taskCount = tasksForThisDay.size,
                            context = context
                        )
                    }

                    if (tasksForThisDay.isNotEmpty()) {
                        items(
                            items = tasksForThisDay,
                            itemId = { task -> task.id.hashCode().toLong() }
                        ) { task ->
                            WeeklyTaskWidgetItem(task = task, isToday = isCurrentDay)

                            Spacer(GlanceModifier.height(6.dp))
                        }
                    } else {
                        item(itemId = "empty-${day.toEpochDay()}".hashCode().toLong()) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isCurrentDay) WidgetColors.todayEmptyDayBackground
                                        else WidgetColors.emptyDayBackground 
                                    )
                                    .cornerRadius(10.dp)
                                    .padding(vertical = 12.dp, horizontal = 12.dp)

                            ) {
                                Text(
                                    text = context.getString(R.string.widget_no_tasks_for_this_day),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        color = WidgetColors.tertiaryText, 
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = GlanceModifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    if (index < daysInWeek.size - 1) {
                        item(itemId = "spacer-${day.toEpochDay()}".hashCode().toLong()) {
                            Spacer(GlanceModifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyDayHeader(date: LocalDate, isToday: Boolean, taskCount: Int, context: Context) {
    val dayNameFormatter = DateTimeFormatter.ofPattern("EEE")
    val dayNumberFormatter = DateTimeFormatter.ofPattern("d")

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(if (isToday) WidgetColors.todayHighlightBackground else WidgetColors.dayHeaderBackground)
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .cornerRadius(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(dayNameFormatter).uppercase(Locale.getDefault()),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isToday) WidgetColors.todayHighlightText else WidgetColors.dayHeaderText 
            )
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = date.format(dayNumberFormatter),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isToday) WidgetColors.todayHighlightText else WidgetColors.dayHeaderText 
            )
        )
        if (isToday) {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = "(${context.getString(R.string.widget_today_indicator)})",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = WidgetColors.todayHighlightText 
                )
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        if (taskCount > 0) {
            Text(
                text = context.resources.getQuantityString(
                    R.plurals.widget_task_count,
                    taskCount,
                    taskCount
                ),
                style = TextStyle(
                    fontSize = 13.sp,
                    color = if (isToday) WidgetColors.todayHighlightText else WidgetColors.tertiaryText 
                )
            )
        }
    }
    Spacer(GlanceModifier.height(6.dp))
}

@Composable
fun WeeklyTaskWidgetItem(task: Task, isToday: Boolean) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val backgroundProvider =
        if (isToday) WidgetColors.todayTaskItemBackground else WidgetColors.itemBackground

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(backgroundProvider) 
            .cornerRadius(12.dp)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(width = 4.dp, height = 30.dp)
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
                    fontSize = 15.sp,
                    color = WidgetColors.primaryText 
                ),
                maxLines = 2
            )
            task.startDateConf?.dateTime?.let {
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