package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.usecases.GetTasksUseCase
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.MockKAnnotations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.time.LocalDateTime

class GetTasksUseCaseTest {

    @MockK
    private lateinit var repository: TaskRepository

    private lateinit var getTasksUseCase: GetTasksUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        getTasksUseCase = GetTasksUseCase(repository)
    }

    @Test
    fun `should get single list of tasks`() = runTest {

        val task = listOf(
            Task(
                id = 1,
                name = "Task 1",
                isCompleted = false,
                isExpired = false,
                priority = Priority.HIGH,
                startDateConf = TimePlanning(LocalDateTime.now(), DayPeriod.MORNING),
                endDateConf = TimePlanning(LocalDateTime.now().plusHours(1), DayPeriod.EVENING),
                durationConf = DurationPlan(60),
            )
        )
        coEvery { repository.getTasks() } returns flowOf(task)


        val result = getTasksUseCase().first()


        assertEquals(task, result)
    }

    @Test
    fun `should emit list of 3 tasks when repository returns 3 tasks`() = runTest {
        // Given
        val tasks = listOf(
            Task(id = 1, name = "Task 1"),
            Task(id = 2, name = "Task 2"),
            Task(id = 3, name = "Task 3")
        )
        coEvery { repository.getTasks() } returns flowOf(tasks)

        // When
        val result = getTasksUseCase().first()

        // Then
        assertEquals(tasks, result)
    }

    @Test
    fun `should emit empty list when repository returns no tasks`() = runTest {
        // Given
        coEvery { repository.getTasks() } returns flowOf(emptyList())

        // When
        val result = getTasksUseCase().first()

        // Then
        assertTrue(result.isEmpty())
    }


    @Test
    fun `should throw IOException when repository fails`() = runTest {

        val errorMessage = "Simulated error"
        coEvery { repository.getTasks() } throws IOException(errorMessage)


        val exception = runBlocking {
            assertThrows(IOException::class.java) {
                runBlocking { getTasksUseCase().first() }
            }
        }


        assertEquals(errorMessage, exception.message)
    }

    @Test
    fun `should allow duplicate tasks when repository provides duplicates`() = runTest {
        // Given
        val t1 = Task(id = 1, name = "Task1")
        val t2 = Task(id = 1, name = "Task1")
        coEvery { repository.getTasks() } returns flowOf(listOf(t1, t2))

        // When
        val result = getTasksUseCase().first()

        // Then
        assertEquals(listOf(t1, t2), result)
    }
}