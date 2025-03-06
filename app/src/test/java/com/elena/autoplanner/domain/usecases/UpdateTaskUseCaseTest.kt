package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.usecases.tasks.UpdateTaskUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateTaskUseCaseTest {
    private val repository: TaskRepository = mockk()
    private val useCase = UpdateTaskUseCase(repository)

    @Test
    fun `invoke should update task via repository`() = runTest {
        val testTask = Task(id = 1, name = "Updated Task")
        coEvery { repository.saveTask(testTask) } just Runs

        useCase(testTask)

        coVerify(exactly = 1) { repository.saveTask(testTask) }
    }
}