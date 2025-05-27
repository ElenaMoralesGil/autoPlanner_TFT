
package com.elena.autoplanner.notifications
import com.elena.autoplanner.notifications.NotificationScheduler
import org.koin.java.KoinJavaComponent.inject
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay // Optional: For extra safety if needed
import org.koin.java.KoinJavaComponent // Use KoinJavaComponent

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device booted. Attempting to reschedule notifications...")

            val pendingResult = goAsync()

            scope.launch {
                try {
                    val taskRepository: TaskRepository by KoinJavaComponent.inject(TaskRepository::class.java)
                    val notificationScheduler: NotificationScheduler by inject(NotificationScheduler::class.java)

                    Log.d(TAG, "Koin dependencies retrieved. Fetching tasks...")
                    // Fetch the TaskResult first
                    val tasksResult: TaskResult<List<Task>>? =
                        taskRepository.getTasks().firstOrNull()

                    if (tasksResult is TaskResult.Success) {
                        val tasksList: List<Task> = tasksResult.data
                        if (tasksList.isEmpty()) {
                            Log.w(TAG, "Task list is empty after boot.")
                        } else {
                            Log.d(TAG, "Found ${tasksList.size} tasks to check for rescheduling.")
                            var rescheduledCount = 0
                            tasksList.forEach { task ->
                                val reminderPlan = task.reminderPlan
                                val startTime = task.startDateConf.dateTime
                                if (reminderPlan != null && reminderPlan.mode != ReminderMode.NONE && startTime != null && !task.isCompleted) {
                                    val triggerTime =
                                        notificationScheduler.calculateReminderTime(task)
                                    if (triggerTime != null && triggerTime.isAfter(java.time.LocalDateTime.now())) {
                                        notificationScheduler.scheduleNotification(task)
                                        rescheduledCount++
                                        Log.v(TAG, "Rescheduled notification for task ${task.id}")
                                    } else {
                                        Log.v(
                                            TAG,
                                            "Skipping reschedule for task ${task.id}: Reminder time $triggerTime is in the past or null."
                                        )
                                    }
                                }
                            }
                            Log.i(TAG, "Finished rescheduling $rescheduledCount notifications.")
                        }
                    } else {
                        val errorMessage =
                            if (tasksResult is TaskResult.Error) tasksResult.message else "null result"
                        Log.w(TAG, "Failed to fetch tasks after boot: $errorMessage")
                    }

                } catch (e: Exception) {
                    // Catch potential Koin resolution errors here as well
                    Log.e(TAG, "Error rescheduling notifications after boot", e)
                } finally {
                    Log.d(TAG, "Finishing background work for BootReceiver.")
                    pendingResult.finish()
                }
            }
        }
    }
}