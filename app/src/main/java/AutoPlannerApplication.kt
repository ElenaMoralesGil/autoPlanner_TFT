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
        val name = getString(R.string.channel_name_reminders) // Add this string resource
        val descriptionText =
            getString(R.string.channel_description_reminders) // Add this string resource
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(REMINDER_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            // Configure channel further if needed (lights, vibration, etc.)
            enableLights(true)
            lightColor = getColor(R.color.purple_500) // Example color
            enableVibration(true)
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}