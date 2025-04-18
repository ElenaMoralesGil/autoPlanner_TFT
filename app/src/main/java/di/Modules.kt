package com.elena.autoplanner.di

import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.data.local.TaskDatabase
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.repository.TaskRepositoryImpl
import com.elena.autoplanner.data.repository.UserRepositoryImpl
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.repository.UserRepository
import com.elena.autoplanner.domain.usecases.auth.DeleteAccountUseCase
import com.elena.autoplanner.domain.usecases.auth.GetCurrentUserUseCase
import com.elena.autoplanner.domain.usecases.auth.LoginUseCase
import com.elena.autoplanner.domain.usecases.auth.LogoutUseCase
import com.elena.autoplanner.domain.usecases.auth.RegisterUseCase
import com.elena.autoplanner.domain.usecases.planner.GeneratePlanUseCase
import com.elena.autoplanner.domain.usecases.planner.OverdueTaskHandler
import com.elena.autoplanner.domain.usecases.planner.RecurrenceExpander
import com.elena.autoplanner.domain.usecases.planner.TaskCategorizer
import com.elena.autoplanner.domain.usecases.planner.TaskPlacer
import com.elena.autoplanner.domain.usecases.planner.TaskPrioritizer
import com.elena.autoplanner.domain.usecases.planner.TimelineManager
import com.elena.autoplanner.domain.usecases.profile.GetProfileStatsUseCase
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteAllTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.domain.usecases.tasks.UpdateTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ValidateTaskUseCase
import com.elena.autoplanner.presentation.viewmodel.CalendarViewModel
import com.elena.autoplanner.presentation.viewmodel.LoginViewModel
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import com.elena.autoplanner.presentation.viewmodel.ProfileViewModel
import com.elena.autoplanner.presentation.viewmodel.RegisterViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


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
            .fallbackToDestructiveMigration(false)
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
    single { FirebaseAuth.getInstance() }
    single<UserRepository> { UserRepositoryImpl(firebaseAuth = get()) }
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

    single { TaskPrioritizer() }
    single { TaskCategorizer() }
    single { RecurrenceExpander() }
    single { TimelineManager() }
    single { OverdueTaskHandler() }
    single { TaskPlacer(taskPrioritizer = get()) }
    single {
        GeneratePlanUseCase(
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single { GetCurrentUserUseCase(get()) }
    single { LoginUseCase(get()) }
    single { RegisterUseCase(get()) }
    single { LogoutUseCase(get()) }
    single { DeleteAccountUseCase(get()) }
    // Profile Use Cases
    single { GetProfileStatsUseCase(get()) }
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

    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
}
