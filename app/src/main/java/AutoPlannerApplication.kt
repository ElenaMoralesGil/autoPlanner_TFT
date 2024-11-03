package com.elena.autoplanner

import android.app.Application
import appModule
import useCaseModule
import viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AutoPlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@AutoPlannerApplication)
            modules(listOf(appModule, viewModelModule, useCaseModule))
        }
    }
}
