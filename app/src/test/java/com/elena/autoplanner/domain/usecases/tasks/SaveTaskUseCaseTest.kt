package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class SaveTaskUseCaseTest {

    @Mock
    private lateinit var taskRepository: TaskRepository

    @Mock
    private lateinit var validateTaskUseCase: ValidateTaskUseCase

    private lateinit var saveTaskUseCase: SaveTaskUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        saveTaskUseCase = SaveTaskUseCase(taskRepository, validateTaskUseCase)
    }

    @Test
    fun `save task success when validation passes`() = runTest {
        val task = createValidTask()
        whenever(validateTaskUseCase(task)).thenReturn(TaskResult.Success(task))
        whenever(taskRepository.saveTask(task)).thenReturn(TaskResult.Success(1))

        val result = saveTaskUseCase(task)

        Assert.assertTrue(result is TaskResult.Success)
        Assert.assertEquals(1, (result as TaskResult.Success).data)
        verify(validateTaskUseCase).invoke(task)
        verify(taskRepository).saveTask(task)
    }

    @Test
    fun `save task fails when validation fails with empty name`() = runTest {
        val task = createValidTask()
        val validationError = TaskResult.Error("Task name cannot be empty")
        whenever(validateTaskUseCase(task)).thenReturn(validationError)

        val result = saveTaskUseCase(task)

        Assert.assertTrue(result is TaskResult.Error)
        Assert.assertEquals("Task name cannot be empty", (result as TaskResult.Error).message)
        verify(validateTaskUseCase).invoke(task)
        verify(taskRepository, never()).saveTask(any())
    }

    @Test
    fun `save task fails when validation fails with invalid dates`() = runTest {
        val task = createValidTask()
        val validationError = TaskResult.Error("Start date must be before end date")
        whenever(validateTaskUseCase(task)).thenReturn(validationError)

        val result = saveTaskUseCase(task)

        Assert.assertTrue(result is TaskResult.Error)
        Assert.assertEquals(
            "Start date must be before end date",
            (result as TaskResult.Error).message
        )
        verify(validateTaskUseCase).invoke(task)
        verify(taskRepository, never()).saveTask(any())
    }

    @Test
    fun `save task fails when validation fails with negative duration`() = runTest {
        val task = createValidTask()
        val validationError = TaskResult.Error("Duration cannot be negative")
        whenever(validateTaskUseCase(task)).thenReturn(validationError)

        val result = saveTaskUseCase(task)

        Assert.assertTrue(result is TaskResult.Error)
        Assert.assertEquals("Duration cannot be negative", (result as TaskResult.Error).message)
        verify(validateTaskUseCase).invoke(task)
        verify(taskRepository, never()).saveTask(any())
    }

    @Test
    fun `save task fails when repository fails`() = runTest {
        val task = createValidTask()
        whenever(validateTaskUseCase(task)).thenReturn(TaskResult.Success(task))
        whenever(taskRepository.saveTask(task)).thenReturn(TaskResult.Error("Database error"))

        val result = saveTaskUseCase(task)

        Assert.assertTrue(result is TaskResult.Error)
        Assert.assertEquals("Database error", (result as TaskResult.Error).message)
        verify(validateTaskUseCase).invoke(task)
        verify(taskRepository).saveTask(task)
    }

    private fun createValidTask() = Task.Builder()
        .name("Test Task")
        .priority(Priority.MEDIUM)
        .startDateConf(TimePlanning(LocalDateTime.now()))
        .build()
}