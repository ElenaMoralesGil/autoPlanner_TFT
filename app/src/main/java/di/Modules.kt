package com.elena.autoplanner.di

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.data.MIGRATION_10_11
import com.elena.autoplanner.data.MIGRATION_11_12
import com.elena.autoplanner.data.MIGRATION_12_13
import com.elena.autoplanner.data.MIGRATION_13_14 // Agregar nueva migración
import com.elena.autoplanner.data.MIGRATION_6_7
import com.elena.autoplanner.data.MIGRATION_7_8
import com.elena.autoplanner.data.MIGRATION_8_9
import com.elena.autoplanner.data.MIGRATION_9_10
import com.elena.autoplanner.data.TaskDatabase
import com.elena.autoplanner.data.dao.ListDao
import com.elena.autoplanner.data.dao.ReminderDao
import com.elena.autoplanner.data.dao.RepeatConfigDao
import com.elena.autoplanner.data.dao.SectionDao
import com.elena.autoplanner.data.dao.SubtaskDao
import com.elena.autoplanner.data.dao.TaskDao
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
import com.elena.autoplanner.domain.usecases.lists.DeleteListUseCase
import com.elena.autoplanner.domain.usecases.lists.DeleteSectionUseCase
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
import com.elena.autoplanner.domain.usecases.tasks.RepeatableTaskGenerator
import com.elena.autoplanner.domain.usecases.tasks.GetExpandedTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.CompleteRepeatableTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteRepeatableTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.GenerateRepeatableTaskInstancesUseCase
import com.elena.autoplanner.domain.usecases.tasks.RepeatableTaskInstanceManager
import com.elena.autoplanner.notifications.NotificationScheduler
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
            .addMigrations(
                MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14
            )
            .addCallback(roomCallback)
            .build()
    }

    single<TaskDao> { get<TaskDatabase>().taskDao() }
    single<ReminderDao> { get<TaskDatabase>().reminderDao() }
    single<RepeatConfigDao> { get<TaskDatabase>().repeatConfigDao() }
    single<SubtaskDao> { get<TaskDatabase>().subtaskDao() }
    single<ListDao> { get<TaskDatabase>().listDao() }
    single<SectionDao> { get<TaskDatabase>().sectionDao() }

    // DAO para instancias de tareas repetibles
    single { get<TaskDatabase>().repeatableTaskInstanceDao() }

    // Usecase para instancias de tareas repetibles
    single { RepeatableTaskInstanceManager(get()) }

    single<TaskRepository> {
        TaskRepositoryImpl(
            context = androidContext(),
            taskDao = get(),
            reminderDao = get(),
            repeatConfigDao = get(),
            subtaskDao = get(),
            listDao = get(),
            sectionDao = get(),
            userRepository = get(),
            firestore = get(),
            repoScope = get(),
            listRepository = get(),
            notificationScheduler = get()
        )
    }
    single { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { Firebase.firestore }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single<UserRepository> { UserRepositoryImpl(firebaseAuth = get()) }
    single<ListRepository> {
        ListRepositoryImpl(
            listDao = get(),
            sectionDao = get(),
            taskDao = get(),
            userRepository = get(),
            firestore = get(),           
            dispatcher = Dispatchers.IO,
            repoScope = get()
        )
    }
    single {
        NotificationScheduler(
            context = androidContext(),
            alarmManager = androidContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        )
    }

}

val useCaseModule = module {
    single { GetTasksUseCase(get()) }
    single { GetTaskUseCase(get(), get()) } // Actualizado para incluir GetExpandedTasksUseCase
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

    single { RepeatableTaskGenerator(get()) }
    single {
        GetExpandedTasksUseCase(
            get(),
            get()
        )
    } // Agregar RepeatableTaskGenerator como segunda dependencia
    single { CompleteRepeatableTaskUseCase(get(), get()) }
    single { DeleteRepeatableTaskUseCase(get(), get()) }

    single { TaskPrioritizer() }
    single { TaskCategorizer() }
    single { RecurrenceExpander() }
    single { TimelineManager() }
    single { OverdueTaskHandler() }
    single { TaskPlacer() }
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
    single { DeleteListUseCase(get()) } 
    single { DeleteSectionUseCase(get()) }

    single { GenerateRepeatableTaskInstancesUseCase(get()) }
}

val viewModelModule = module {
    viewModel {
        CalendarViewModel(
            getExpandedTasksUseCase = get()
        )
    }

    viewModel {
        PlannerViewModel(
            generatePlanUseCase = get(),
            getTasksUseCase = get(),
            saveTaskUseCase = get()
        )
    }
    viewModel { MoreViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) ->
        TaskListViewModel(
            getTasksByListUseCase = get(),
            filterTasksUseCase = get(),
            toggleTaskCompletionUseCase = get(),
            deleteTaskUseCase = get(),
            saveTaskUseCase = get(),
            saveListUseCase = get(),
            saveSectionUseCase = get(),
            getAllSectionsUseCase = get(),
            getAllListsUseCase = get(),
            getExpandedTasksUseCase = get(),
            completeRepeatableTaskUseCase = get(),
            deleteRepeatableTaskUseCase = get(),
            repeatableTaskGenerator = get<RepeatableTaskGenerator>(),
            savedStateHandle = handle,
            taskRepository = get()
        )
    }

    viewModel { parameters ->
        val taskId = parameters.getOrNull<Int>() ?: 0
        val instanceIdentifier = parameters.getOrNull<String>()
        TaskDetailViewModel(
            getTaskUseCase = get(),
            toggleTaskCompletionUseCase = get(),
            completeRepeatableTaskUseCase = get(),
            deleteTaskUseCase = get(),
            deleteRepeatableTaskUseCase = get(),
            addSubtaskUseCase = get(),
            toggleSubtaskUseCase = get(),
            deleteSubtaskUseCase = get(),
            taskId = taskId,
            instanceIdentifier = instanceIdentifier,
            repeatableTaskInstanceManager = get()
        )
    }

    viewModel { (taskId: Int) -> 
        TaskEditViewModel(
            getTaskUseCase = get(),
            saveTaskUseCase = get(),
            getAllListsUseCase = get(),
            getAllSectionsUseCase = get(),
            saveListUseCase = get(),
            saveSectionUseCase = get(),
            repeatableTaskGenerator = get(),
            generateRepeatableTaskInstancesUseCase = get()
        )
    }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { EditProfileViewModel(get(), get(), get()) }
}