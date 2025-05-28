package com.elena.autoplanner.domain.usecases.tasks

import app.cash.turbine.test
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class GetTasksUseCaseTest {

    @Mock
    private lateinit var taskRepository: TaskRepository

    private lateinit var getTasksUseCase: GetTasksUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getTasksUseCase = GetTasksUseCase(taskRepository)
    }

    @Test
    fun `get tasks returns success data`() = runTest {

        val tasks = listOf(createValidTask(), createValidTask())
        whenever(taskRepository.getTasks()).thenReturn(flowOf(TaskResult.Success(tasks)))

        getTasksUseCase().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            awaitComplete()
        }
    }

    @Test
    fun `get tasks returns empty list on error`() = runTest {

        whenever(taskRepository.getTasks()).thenReturn(flowOf(TaskResult.Error("Database error")))

        getTasksUseCase().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            awaitComplete()
        }
    }

    private fun createValidTask() = Task.Builder()
        .name("Test Task")
        .build()
}