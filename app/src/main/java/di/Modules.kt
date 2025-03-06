package com.elena.autoplanner.di

import androidx.room.Room
import com.elena.autoplanner.data.local.TaskDatabase
import com.elena.autoplanner.data.repository.TaskRepositoryImpl
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.usecases.*
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.FilterTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.ToggleTaskCompletionUseCase
import com.elena.autoplanner.domain.usecases.tasks.ValidateTaskUseCase
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import com.elena.autoplanner.presentation.viewmodel.TaskListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            TaskDatabase::class.java,
            "task_database"
        ).fallbackToDestructiveMigration().build()
    }

    single { get<TaskDatabase>().taskDao() }
    single { get<TaskDatabase>().reminderDao() }
    single { get<TaskDatabase>().repeatConfigDao() }
    single { get<TaskDatabase>().subtaskDao() }
}

val repositoryModule = module {
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

    factory { GetTasksUseCase(get()) }
    factory { GetTaskUseCase(get()) }
    factory { ValidateTaskUseCase() }
    factory { SaveTaskUseCase(get(), get()) }
    factory { DeleteTaskUseCase(get()) }


    factory { FilterTasksUseCase() }
    factory { ToggleTaskCompletionUseCase(get(), get()) }


    factory { AddSubtaskUseCase(get(), get()) }
    factory { ToggleSubtaskUseCase(get(), get()) }
    factory { DeleteSubtaskUseCase(get(), get()) }
}

val viewModelModule = module {
    viewModel { TaskListViewModel(get(), get(), get()) }
    viewModel { (taskId: Int) -> TaskDetailViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { (taskId: Int) -> TaskEditViewModel(get(), get()) }
}

val appModule = listOf(databaseModule, repositoryModule, useCaseModule, viewModelModule)