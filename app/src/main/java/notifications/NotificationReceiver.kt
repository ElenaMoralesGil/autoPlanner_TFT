package com.elena.autoplanner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.elena.autoplanner.AutoPlannerApplication
import com.elena.autoplanner.R
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_COMPLETE_TASK = "com.elena.autoplanner.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.elena.autoplanner.ACTION_SNOOZE_TASK"
        const val ACTION_OPEN_TASK = "com.elena.autoplanner.ACTION_OPEN_TASK"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}")

        when (intent.action) {
            ACTION_COMPLETE_TASK -> handleCompleteTask(context, intent)
            ACTION_SNOOZE_TASK -> handleSnoozeTask(context, intent)
            ACTION_OPEN_TASK -> handleOpenTask(context, intent)
            else -> handleShowNotification(context, intent)
        }
    }

    private fun handleShowNotification(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(NotificationScheduler.EXTRA_TASK_ID, -1)
        val taskName =
            intent.getStringExtra(NotificationScheduler.EXTRA_TASK_NAME) ?: "Task Reminder"

        Log.d(TAG, "Received notification for task: $taskId - $taskName")

        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID received")
            return
        }

        scope.launch {
            try {
                val taskRepository: TaskRepository by inject(TaskRepository::class.java)
                val taskResult = taskRepository.getTask(taskId)

                if (taskResult is TaskResult.Success) {
                    showEnhancedNotification(context, taskResult.data)
                } else {

                    showBasicNotification(context, taskId, taskName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching task details", e)
                showBasicNotification(context, taskId, taskName)
            }
        }
    }

    private fun showEnhancedNotification(context: Context, task: Task) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel(context, notificationManager)

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

        val startTime = task.startDateConf.dateTime
        val timeText = startTime?.format(timeFormatter) ?: ""
        val dateText = startTime?.format(dateFormatter) ?: ""

        val contextText = buildString {
            if (timeText.isNotEmpty()) append("🕐 $timeText")
            if (!task.listName.isNullOrEmpty()) {
                if (isNotEmpty()) append(" • ")
                append("📁 ${task.listName}")
            }
            if (!task.sectionName.isNullOrEmpty()) {
                append(" > ${task.sectionName}")
            }
        }

        val (icon, accentColor) = when (task.priority) {
            Priority.HIGH -> R.drawable.priority to android.graphics.Color.parseColor("#DC2626")
            Priority.MEDIUM -> R.drawable.priority to android.graphics.Color.parseColor("#F59E0B")
            Priority.LOW -> R.drawable.priority to android.graphics.Color.parseColor("#3B82F6")
            else -> R.drawable.autoplanner to android.graphics.Color.parseColor("#6366F1") 
        }

        val openIntent = createOpenTaskIntent(context, task.id)
        val completeIntent = createActionIntent(context, ACTION_COMPLETE_TASK, task.id)
        val snoozeIntent = createActionIntent(context, ACTION_SNOOZE_TASK, task.id)

        val builder =
            NotificationCompat.Builder(context, AutoPlannerApplication.REMINDER_CHANNEL_ID)
                .setSmallIcon(icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.app_logo))
                .setContentTitle(task.name)
                .setContentText(contextText)
                .setSubText(dateText)
                .setColor(accentColor)
                .setPriority(
                    when (task.priority) {
                        Priority.HIGH -> NotificationCompat.PRIORITY_HIGH
                        Priority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
                        Priority.LOW -> NotificationCompat.PRIORITY_LOW
                        else -> NotificationCompat.PRIORITY_DEFAULT
                    }
                )
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())

        builder.addAction(
            R.drawable.ic_completed,
            "Complete",
            completeIntent
        )

        builder.addAction(
            R.drawable.ic_snooze,
            "Snooze 10m",
            snoozeIntent
        )

        if (task.subtasks.isNotEmpty()) {
            val subtaskInfo =
                "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size} subtasks completed"
            builder.setProgress(task.subtasks.size, task.subtasks.count { it.isCompleted }, false)
            if (contextText.isEmpty()) {
                builder.setContentText(subtaskInfo)
            } else {
                builder.setContentText("$contextText • $subtaskInfo")
            }
        }

        try {
            notificationManager.notify(task.id, builder.build())
            Log.i(TAG, "Enhanced notification shown for task ${task.id}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot post notification", e)
        }
    }

    private fun showBasicNotification(context: Context, taskId: Int, taskName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel(context, notificationManager)

        val openIntent = createOpenTaskIntent(context, taskId)

        val notification =
            NotificationCompat.Builder(context, AutoPlannerApplication.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.autoplanner)
                .setContentTitle("Task Reminder")
                .setContentText(taskName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

        try {
            notificationManager.notify(taskId, notification)
            Log.i(TAG, "Basic notification shown for task $taskId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot post notification", e)
        }
    }

    private fun createOpenTaskIntent(context: Context, taskId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_task_id", taskId)
            action = ACTION_OPEN_TASK
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                taskId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun createActionIntent(context: Context, action: String, taskId: Int): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationScheduler.EXTRA_TASK_ID, taskId)
        }

        return PendingIntent.getBroadcast(
            context,
            taskId + action.hashCode(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handleCompleteTask(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(NotificationScheduler.EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        scope.launch {
            try {
                val taskRepository: TaskRepository by inject(TaskRepository::class.java)
                val result = taskRepository.updateTaskCompletion(taskId, true)

                if (result is TaskResult.Success) {

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(taskId)

                    showFeedbackNotification(context, "Task completed! ✅", "Great job!")
                } else {
                    Log.e(TAG, "Failed to complete task $taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task", e)
            }
        }
    }

    private fun handleSnoozeTask(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(NotificationScheduler.EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        scope.launch {
            try {
                val taskRepository: TaskRepository by inject(TaskRepository::class.java)
                val notificationScheduler: NotificationScheduler by inject(NotificationScheduler::class.java)

                val taskResult = taskRepository.getTask(taskId)
                if (taskResult is TaskResult.Success) {
                    val task = taskResult.data

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(taskId)

                    val snoozeTime = LocalDateTime.now().plusMinutes(10)
                    notificationScheduler.scheduleNotificationAt(task, snoozeTime)

                    showFeedbackNotification(
                        context,
                        "Snoozed for 10 minutes",
                        "I'll remind you at ${snoozeTime.format(DateTimeFormatter.ofPattern("h:mm a"))}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing task", e)
            }
        }
    }

    private fun handleOpenTask(context: Context, intent: Intent) {

        Log.d(TAG, "Opening task from notification")
    }

    private fun showFeedbackNotification(context: Context, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification =
            NotificationCompat.Builder(context, AutoPlannerApplication.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_uncompleted)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(3000) 
                .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureNotificationChannel(
        context: Context,
        notificationManager: NotificationManager,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                notificationManager.getNotificationChannel(AutoPlannerApplication.REMINDER_CHANNEL_ID)
            if (channel == null) {
                createNotificationChannel(context, notificationManager)
            }
        }
    }

    private fun createNotificationChannel(
        context: Context,
        notificationManager: NotificationManager,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AutoPlannerApplication.REMINDER_CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task reminders"
                enableLights(true)
                lightColor = Color(0xFF6200EE).toArgb()
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}