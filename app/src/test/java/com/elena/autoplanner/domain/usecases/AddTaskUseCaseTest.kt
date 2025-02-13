package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddTaskUseCaseTest {
    private val repository: TaskRepository = mockk()
    private val useCase = AddTaskUseCase(repository)

    @Test
    fun `invoke should save task via repository`() = runTest {
        val testTask = Task(id = 1, name = "Test Task")
        coEvery { repository.saveTask(testTask) } just Runs

        useCase(testTask)

        coVerify(exactly = 1) { repository.saveTask(testTask) }
    }
}