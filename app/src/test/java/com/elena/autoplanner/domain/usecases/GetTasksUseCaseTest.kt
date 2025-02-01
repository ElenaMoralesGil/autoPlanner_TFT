package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.MockKAnnotations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class GetTasksUseCaseTest {

    @MockK
    lateinit var repository: TaskRepository

    private lateinit var getTasksUseCase: GetTasksUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        getTasksUseCase = GetTasksUseCase(repository)
    }

    @Test
    fun `test repository returns 3 tasks`() = runBlockingTest {
        val t1 = Task(id = 1, name = "Task 1")
        val t2 = Task(id = 2, name = "Task 2")
        val t3 = Task(id = 3, name = "Task 3")
        val fakeFlow = flowOf(listOf(t1, t2, t3))

        coEvery { repository.getTasks() } returns fakeFlow

        val actual = getTasksUseCase()
            .take(1)
            .toList()

        assertEquals(1, actual.size)
        assertEquals(listOf(t1, t2, t3), actual[0])
    }

    @Test
    fun `test repository returns empty list`() = runBlockingTest {
        coEvery { repository.getTasks() } returns flowOf(emptyList())

        val emissions = getTasksUseCase()
            .take(1)
            .toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0].isEmpty())
    }

    @Test
    fun `test exception in repository`() = runBlockingTest {
        coEvery { repository.getTasks() } throws IOException("Simulated error")

        try {
            getTasksUseCase().take(1).toList()
            fail("Expected IOException was not thrown")
        } catch (e: IOException) {
            assertEquals("Simulated error", e.message)
        }
    }

    @Test
    fun `test repository emits changes`() = runBlockingTest {
        val mutableFlow = MutableSharedFlow<List<Task>>(replay = 1)
        coEvery { repository.getTasks() } returns mutableFlow

        val collectedLists = mutableListOf<List<Task>>()
        val job = launch {
            getTasksUseCase().collect {
                collectedLists.add(it)
            }
        }

        val t1 = Task(id = 1, name = "Task 1")
        mutableFlow.emit(listOf(t1))
        val t2 = Task(id = 2, name = "Task 2")
        mutableFlow.emit(listOf(t1, t2))

        advanceUntilIdle()

        assertEquals(2, collectedLists.size)
        assertEquals(listOf(t1), collectedLists[0])
        assertEquals(listOf(t1, t2), collectedLists[1])

        job.cancel()
    }

    @Test
    fun `test repository allows duplicate tasks`() = runBlockingTest {
        val t1 = Task(id = 1, name = "Task1")
        val t2 = Task(id = 1, name = "Task1")

        coEvery { repository.getTasks() } returns flowOf(listOf(t1, t2))

        val emissions = getTasksUseCase()
            .take(1)
            .toList()

        assertEquals(1, emissions.size)
        assertEquals(listOf(t1, t2), emissions[0])
    }
}
