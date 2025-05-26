package com.elena.autoplanner.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.elena.autoplanner.AutoPlannerApplication
import com.elena.autoplanner.R
import com.elena.autoplanner.presentation.MainActivity

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification alarm received")
        val taskId = intent.getIntExtra(NotificationScheduler.EXTRA_TASK_ID, -1)
        val taskName =
            intent.getStringExtra(NotificationScheduler.EXTRA_TASK_NAME) ?: "Task Reminder"

        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID received in notification intent.")
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch MainActivity when notification is tapped
        // Ideally, navigate directly to the task detail if possible, but launching MainActivity is simpler for now.
        val launchAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Optional: Add extra data to navigate to the specific task upon launch
            // This requires handling in MainActivity/Navigation logic
            putExtra("navigate_to_task_id", taskId)
        }

        val launchAppPendingIntent = PendingIntent.getActivity(
            context,
            taskId, // Use task ID for request code to potentially differentiate clicks
            launchAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, AutoPlannerApplication.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a dedicated notification icon
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(taskName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss notification when tapped
                .setContentIntent(launchAppPendingIntent) // Set the intent to launch when tapped
                .build()

        try {
            // Use taskId as notification ID to allow updating/canceling specific notifications
            notificationManager.notify(taskId, notification)
            Log.i(TAG, "Notification shown for task $taskId ('$taskName')")
        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "SecurityException: Failed to post notification for task $taskId. Check POST_NOTIFICATIONS permission.",
                e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification for task $taskId", e)
        }
    }
}