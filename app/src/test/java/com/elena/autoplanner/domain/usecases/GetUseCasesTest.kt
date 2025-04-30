package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test


class GetUseCaseTest {
    private val repository: TaskRepository = mockk()
    private val useCase = GetTasksUseCase(repository)

    @Test
    fun `invoke should return flow of tasks`() = runTest {
        val mockTasks = listOf(Task(id = 1, name = "Task 1"), Task(id = 2, name = "Task 2"))
        every { repository.getTasks() } returns flowOf(mockTasks)

        val resultFlow = useCase()

        resultFlow.collect { tasks ->
            assertEquals(2, tasks.size)
        }
    }
}