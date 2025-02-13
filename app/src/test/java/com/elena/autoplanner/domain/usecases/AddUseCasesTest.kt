package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AddUseCasesTest {
    private val repository: TaskRepository = mockk()
    private val taskUseCase = AddTaskUseCase(repository)
    private val subtaskUseCase = AddSubtaskUseCase(repository)

    @Test
    fun `invoke should save task via repository`() = runTest {
        val testTask = Task(id = 1, name = "Test Task")
        coEvery { repository.saveTask(testTask) } just Runs

        taskUseCase(testTask)

        coVerify(exactly = 1) { repository.saveTask(testTask) }
    }

    @Test
    fun `invoke should add subtask to existing task`() = runTest {
        val taskId = 1
        val originalTask = Task(id = taskId, name = "Original Task", subtasks = emptyList())
        coEvery { repository.getTask(taskId) } returns originalTask
        coEvery { repository.saveTask(any()) } just Runs

        val result = subtaskUseCase(taskId, "New Subtask")

        assertEquals(1, result.subtasks.size)
        assertEquals("New Subtask", result.subtasks.first().name)
        coVerify { repository.saveTask(any()) }
    }

    @Test
    fun `invoke should throw when task not found`() = runTest {
        coEvery { repository.getTask(any()) } returns null

        try {
            subtaskUseCase(999, "Invalid Subtask")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Task not found", e.message)
        }
    }



}