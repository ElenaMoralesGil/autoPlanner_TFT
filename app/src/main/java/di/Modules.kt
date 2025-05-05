package com.elena.autoplanner.di

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.data.local.MIGRATION_6_7
import com.elena.autoplanner.data.local.MIGRATION_7_8
import com.elena.autoplanner.data.local.MIGRATION_8_9
import com.elena.autoplanner.data.local.TaskDatabase
import com.elena.autoplanner.data.local.dao.ListDao
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SectionDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.repositories.ListRepositoryImpl
import com.elena.autoplanner.data.repositories.TaskRepositoryImpl
import com.elena.autoplanner.data.repositories.UserRepositoryImpl
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.usecases.auth.DeleteAccountUseCase
import com.elena.autoplanner.domain.usecases.auth.GetCurrentUserUseCase
import com.elena.autoplanner.domain.usecases.auth.LoginUseCase
import com.elena.autoplanner.domain.usecases.auth.LogoutUseCase
import com.elena.autoplanner.domain.usecases.auth.ReauthenticateUseCase
import com.elena.autoplanner.domain.usecases.auth.RegisterUseCase
import com.elena.autoplanner.domain.usecases.lists.GetAllListsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetAllSectionsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetListsInfoUseCase
import com.elena.autoplanner.domain.usecases.lists.GetSectionsUseCase
import com.elena.autoplanner.domain.usecases.lists.GetTasksByListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveListUseCase
import com.elena.autoplanner.domain.usecases.lists.SaveSectionUseCase
import com.elena.autoplanner.domain.usecases.planner.GeneratePlanUseCase
import com.elena.autoplanner.domain.usecases.planner.OverdueTaskHandler
import com.elena.autoplanner.domain.usecases.planner.RecurrenceExpander
import com.elena.autoplanner.domain.usecases.planner.TaskCategorizer
import com.elena.autoplanner.domain.usecases.planner.TaskPlacer
import com.elena.autoplanner.domain.usecases.planner.TaskPrioritizer
import com.elena.autoplanner.domain.usecases.planner.TimelineManager
import com.elena.autoplanner.domain.usecases.profile.GetProfileStatsUseCase
import com.elena.autoplanner.domain.usecases.profile.UpdateProfileUseCase
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
import com.elena.autoplanner.presentation.viewmodel.EditProfileViewModel
import com.elena.autoplanner.presentation.viewmodel.LoginViewModel
import com.elena.autoplanner.presentation.viewmodel.MoreViewModel
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import com.elena.autoplanner.presentation.viewmodel.ProfileViewModel
import com.elena.autoplanner.presentation.viewmodel.RegisterViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .addCallback(roomCallback)
            .build()
    }

    single<TaskDao> { get<TaskDatabase>().taskDao() }
    single<ReminderDao> { get<TaskDatabase>().reminderDao() }
    single<RepeatConfigDao> { get<TaskDatabase>().repeatConfigDao() }
    single<SubtaskDao> { get<TaskDatabase>().subtaskDao() }
    single<ListDao> { get<TaskDatabase>().listDao() }
    single<SectionDao> { get<TaskDatabase>().sectionDao() }

    single<TaskRepository> {
        TaskRepositoryImpl(
            context = androidContext(),
            taskDao = get(),
            reminderDao = get(),
            repeatConfigDao = get(),
            subtaskDao = get(),
            userRepository = get(),
            firestore = get(),
            repoScope = get()
        )
    }
    single { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { Firebase.firestore }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single<UserRepository> { UserRepositoryImpl(firebaseAuth = get()) }
    single<ListRepository> {
        ListRepositoryImpl(
            context = androidContext(),
            listDao = get(),
            sectionDao = get()
        )
    } // Add ListRepository
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
    single { ReauthenticateUseCase(get()) }
    single { GetProfileStatsUseCase() }
    single { UpdateProfileUseCase(get()) }

    single { GetTasksByListUseCase(get(), get()) }
    single { GetListsInfoUseCase(get()) }
    single { GetAllListsUseCase(get()) }
    single { SaveListUseCase(get()) }
    single { GetSectionsUseCase(get()) }
    single { GetAllSectionsUseCase(get()) }
    single { SaveSectionUseCase(get()) }
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
    viewModel { MoreViewModel(get(), get(), get(), get(), get()) } // Add MoreViewModel
    viewModel { (handle: SavedStateHandle) -> // Koin provides SavedStateHandle
        TaskListViewModel(
            getTasksByListUseCase = get(),
            filterTasksUseCase = get(),
            toggleTaskCompletionUseCase = get(),
            deleteTaskUseCase = get(),
            saveTaskUseCase = get(),
            saveListUseCase = get(),
            saveSectionUseCase = get(),
            getAllSectionsUseCase = get(),
            savedStateHandle = handle // Pass the handle
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

    viewModel { (taskId: Int) -> // TaskEditViewModel might also need SavedStateHandle if it depends on nav args
        TaskEditViewModel(
            getTaskUseCase = get(),
            saveTaskUseCase = get(),
            getAllListsUseCase = get(),
            getAllSectionsUseCase = get(),
            saveListUseCase = get(),      // Add this
            saveSectionUseCase = get()
            // savedStateHandle = get() // Add if needed
        )
    }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { EditProfileViewModel(get(), get(), get()) }
}