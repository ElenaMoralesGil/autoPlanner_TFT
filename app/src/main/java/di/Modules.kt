package com.elena.autoplanner.di

import android.util.Log
import org.koin.dsl.module
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.data.repository.TaskRepositoryImpl

import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.TaskDatabase
import org.koin.android.ext.koin.androidContext

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.domain.usecases.planner.GeneratePlanUseCase
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteAllTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.domain.usecases.tasks.UpdateTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ValidateTaskUseCase
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
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
            context = androidContext(),
            taskDao = get(),
            reminderDao = get(),
            repeatConfigDao = get(),
            subtaskDao = get()
        )
    }
}

val useCaseModule = module {
    single { GetTasksUseCase(get()) }
    single { GetTaskUseCase(get()) }
    single { UpdateTaskUseCase(get()) }
    single { DeleteTaskUseCase(get()) }
    single { AddSubtaskUseCase(get(), get()) }
    single { SaveTaskUseCase(get(), get()) }
    single { ValidateTaskUseCase() }
    single { ToggleTaskCompletionUseCase(get()) }
    single { ToggleSubtaskUseCase(get(), get()) }
    single { DeleteSubtaskUseCase(get(), get()) }
    single { DeleteAllTasksUseCase(get()) }
    single { FilterTasksUseCase() }
    single { GeneratePlanUseCase() }
}

val viewModelModule = module {
    viewModel { CalendarViewModel() }

    viewModel {
        PlannerViewModel(
            generatePlanUseCase = get(),
            getTasksUseCase = get(),
            saveTaskUseCase = get()
        )
    }

    viewModel {
        TaskListViewModel(
            getTasksUseCase = get(),
            filterTasksUseCase = get(),
            toggleTaskCompletionUseCase = get(),
            deleteTaskUseCase = get(),
            saveTaskUseCase = get()
        )
    }

    viewModel { (taskId: Int) ->
        TaskDetailViewModel(
            getTaskUseCase = get(),
            toggleTaskCompletionUseCase = get(),
            deleteTaskUseCase = get(),
            addSubtaskUseCase = get(),
            toggleSubtaskUseCase = get(),
            deleteSubtaskUseCase = get(),
            taskId = taskId
        )
    }

    viewModel {
        TaskEditViewModel(
            getTaskUseCase = get(),
            saveTaskUseCase = get()
        )
    }
}
