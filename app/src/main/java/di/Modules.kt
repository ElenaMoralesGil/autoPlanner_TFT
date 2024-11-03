import org.koin.dsl.module
import domain.repository.TaskRepository
import domain.usecases.GetTasksUseCase
import data.repository.TaskRepositoryImpl
import data.local.TaskDao
import data.local.TaskDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import presentation.viewmodel.TaskViewModel
import androidx.room.Room

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            TaskDatabase::class.java,
            "task_database"
        ).build()
    }
    single<TaskDao> { get<TaskDatabase>().taskDao() }
    single<TaskRepository> { TaskRepositoryImpl(get()) }
}

val useCaseModule = module {
    single { GetTasksUseCase(get()) }
}

val viewModelModule = module {
    viewModel { TaskViewModel(get()) }
}
