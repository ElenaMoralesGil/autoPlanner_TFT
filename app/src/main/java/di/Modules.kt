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

import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.domain.usecases.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.AddTaskUseCase
import com.elena.autoplanner.domain.usecases.DeleteAllTasksUseCase
import com.elena.autoplanner.domain.usecases.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.UpdateTaskUseCase
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import org.koin.core.module.dsl.viewModel


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

    single<TaskDao> { get<TaskDatabase>().taskDao() }
    single<ReminderDao> { get<TaskDatabase>().reminderDao() }
    single<RepeatConfigDao> { get<TaskDatabase>().repeatConfigDao() }
    single<SubtaskDao> { get<TaskDatabase>().subtaskDao() }

    single<TaskRepository> {
        TaskRepositoryImpl(
            taskDao = get(),
            reminderDao = get(),
            repeatConfigDao = get(),
            subtaskDao = get()
        )
    }
}

val useCaseModule = module {
    single { GetTasksUseCase(get()) }
    single { AddTaskUseCase(get()) }
    single { UpdateTaskUseCase(get()) }
    single { DeleteTaskUseCase(get()) }
    single { AddSubtaskUseCase(get()) }
    single { ToggleSubtaskUseCase(get()) }
    single { DeleteSubtaskUseCase(get()) }
    single { DeleteAllTasksUseCase(get()) }
}

// Módulo de ViewModels
val viewModelModule = module {
    viewModel { CalendarViewModel() }
    viewModel {
        TaskViewModel(get(), get(), get(), get(), get(), get(), get(), get())
    }


}
