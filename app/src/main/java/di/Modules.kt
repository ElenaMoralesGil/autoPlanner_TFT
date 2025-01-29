package com.elena.autoplanner.di

import android.util.Log
import org.koin.dsl.module
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.usecases.GetTasksUseCase
import com.elena.autoplanner.data.repository.TaskRepositoryImpl

import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao

import com.elena.autoplanner.data.local.TaskDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.domain.usecases.AddTaskUseCase
import com.elena.autoplanner.domain.usecases.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.UpdateTaskUseCase


val appModule = module {

    val roomCallback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("RoomCallback", "=== onCreate() called. The database has been created ===")
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d("RoomCallback", "=== onOpen() called. The database is open and ready ===")
        }
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            TaskDatabase::class.java,
            "task_database"
        )
            .fallbackToDestructiveMigration()
            .addCallback(roomCallback)
            .build()
    }

    // Inyectar DAOs individuales
    single<TaskDao> { get<TaskDatabase>().taskDao() }
    single<ReminderDao> { get<TaskDatabase>().reminderDao() }
    single<RepeatConfigDao> { get<TaskDatabase>().repeatConfigDao() }
    single<SubtaskDao> { get<TaskDatabase>().subtaskDao() }

    // Repositorio
    single<TaskRepository> {
        TaskRepositoryImpl(
            taskDao = get(),
            reminderDao = get(),
            repeatConfigDao = get(),
            subtaskDao = get()
        )
    }
}

// Módulo de casos de uso
val useCaseModule = module {
    single { GetTasksUseCase(get()) }
    single { AddTaskUseCase(get()) }
    single { UpdateTaskUseCase(get()) }
    single { DeleteTaskUseCase(get()) }
}

// Módulo de ViewModels
val viewModelModule = module {
    viewModel { TaskViewModel(get(), get(), get(), get()) }
}
