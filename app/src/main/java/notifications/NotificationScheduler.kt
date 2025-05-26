package com.elena.autoplanner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.Task
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
) {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_NAME = "extra_task_name"
        private const val TAG = "NotificationScheduler"
    }

    fun scheduleNotification(task: Task) {
        val taskId = task.id
        val triggerTime = calculateReminderTime(task)

        if (triggerTime == null || triggerTime.isBefore(LocalDateTime.now())) {
            Log.d(
                TAG,
                "Reminder for task $taskId is null or in the past. Canceling any existing alarm."
            )
            cancelNotification(taskId) // Cancel if time is invalid or past
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_NAME, task.name)
        }

        // Use task ID as request code for uniqueness
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId, // Use task ID as request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ensure exact alarm permission before scheduling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms. Skipping notification for task $taskId.")
                // Optionally, inform the user or use setAndAllowWhileIdle instead
                // For now, we just skip if permission not granted
                return
            }
        }

        try {
            val triggerMillis =
                triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
            Log.i(
                TAG,
                "Scheduled notification for task $taskId ('${task.name}') at $triggerTime ($triggerMillis)"
            )
        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "SecurityException: Failed to schedule exact alarm for task $taskId. Check SCHEDULE_EXACT_ALARM permission.",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification for task $taskId", e)
        }
    }

    fun cancelNotification(taskId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        // Recreate the *exact same* PendingIntent used for scheduling
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId, // Use the same task ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Canceled notification for task $taskId")
    }

    internal fun calculateReminderTime(task: Task): LocalDateTime? {
        val reminderPlan = task.reminderPlan ?: return null
        val startTime = task.startDateConf.dateTime ?: return null // Reminder needs a start time

        return when (reminderPlan.mode) {
            ReminderMode.NONE -> null
            ReminderMode.PRESET_OFFSET -> {
                reminderPlan.offsetMinutes?.let { offset ->
                    startTime.minusMinutes(offset.toLong())
                } ?: startTime // If offset is somehow null, remind at start time
            }

            ReminderMode.EXACT -> reminderPlan.exactDateTime
            ReminderMode.CUSTOM -> {
                var reminderDateTime = startTime
                reminderPlan.customWeekOffset?.let { weeks ->
                    reminderDateTime = reminderDateTime.minusWeeks(weeks.toLong())
                }
                reminderPlan.customDayOffset?.let { days ->
                    // Important: Day offset is relative to the (potentially week-adjusted) date
                    reminderDateTime = reminderDateTime.minusDays(days.toLong())
                }
                reminderPlan.customHour?.let { hour ->
                    reminderPlan.customMinute?.let { minute ->
                        // Set the specific time on the calculated date
                        reminderDateTime =
                            reminderDateTime.withHour(hour).withMinute(minute).withSecond(0)
                                .withNano(0)
                    }
                }
                reminderDateTime
            }
        }
    }
}