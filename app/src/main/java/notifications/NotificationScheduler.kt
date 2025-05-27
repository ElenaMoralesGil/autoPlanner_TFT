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
        val triggerTime = calculateReminderTime(task)
        if (triggerTime != null) {
            scheduleNotificationAt(task, triggerTime)
        } else {
            Log.d(TAG, "No reminder time calculated for task ${task.id}")
            cancelNotification(task.id)
        }
    }

    fun scheduleNotificationAt(task: Task, triggerTime: LocalDateTime) {
        val taskId = task.id

        if (triggerTime.isBefore(LocalDateTime.now())) {
            Log.d(TAG, "Reminder time for task $taskId is in the past: $triggerTime")

            val minutesPast =
                java.time.Duration.between(triggerTime, LocalDateTime.now()).toMinutes()
            if (minutesPast <= 5) {
                Log.d(TAG, "Reminder is only $minutesPast minutes past, showing immediately")
                showImmediateNotification(task)
            }
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_NAME, task.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms. Skipping notification for task $taskId.")
                return
            }
        }

        try {
            val triggerMillis =
                triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.i(TAG, "Scheduled exact alarm for task $taskId at $triggerTime")
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.i(TAG, "Scheduled inexact alarm for task $taskId at $triggerTime")
                }

                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.i(TAG, "Scheduled alarm for task $taskId at $triggerTime")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification for task $taskId", e)
        }
    }

    private fun showImmediateNotification(task: Task) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_NAME, task.name)
        }
        context.sendBroadcast(intent)
    }

    fun cancelNotification(taskId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Canceled notification for task $taskId")
    }

    internal fun calculateReminderTime(task: Task): LocalDateTime? {
        val reminderPlan = task.reminderPlan ?: return null
        val startTime = task.startDateConf.dateTime ?: return null

        return when (reminderPlan.mode) {
            ReminderMode.NONE -> null
            ReminderMode.PRESET_OFFSET -> {
                reminderPlan.offsetMinutes?.let { offset ->
                    startTime.minusMinutes(offset.toLong())
                } ?: startTime
            }
            ReminderMode.EXACT -> reminderPlan.exactDateTime
            ReminderMode.CUSTOM -> {
                var reminderDateTime = startTime
                reminderPlan.customWeekOffset?.let { weeks ->
                    reminderDateTime = reminderDateTime.minusWeeks(weeks.toLong())
                }
                reminderPlan.customDayOffset?.let { days ->
                    reminderDateTime = reminderDateTime.minusDays(days.toLong())
                }
                reminderPlan.customHour?.let { hour ->
                    reminderPlan.customMinute?.let { minute ->
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