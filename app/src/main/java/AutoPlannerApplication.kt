package com.elena.autoplanner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.elena.autoplanner.di.appModule
import com.elena.autoplanner.di.developmentModule
import com.elena.autoplanner.di.useCaseModule
import com.elena.autoplanner.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AutoPlannerApplication : Application() {

    companion object {
        const val REMINDER_CHANNEL_ID = "task_reminders"
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@AutoPlannerApplication)
            modules(listOf(appModule, viewModelModule, useCaseModule, developmentModule))
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Reminders"
            val descriptionText = "Notifications for task reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(REMINDER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d(
                "AutoPlannerApp",
                "Notification channel created: $REMINDER_CHANNEL_ID"
            )
        }
    }
}