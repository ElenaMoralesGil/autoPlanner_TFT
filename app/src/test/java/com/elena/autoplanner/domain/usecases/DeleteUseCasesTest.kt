package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.usecases.tasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteUseCaseTest {
    private val repository: TaskRepository = mockk()
    private val taskUseCase = DeleteTaskUseCase(repository)
    private val subtaskUseCase = DeleteSubtaskUseCase(repository)

    @Test
    fun `invoke should delete task via repository`() = runTest {
        val testTask = Task(id = 1, name = "Test Task")
        coEvery { repository.deleteTask(testTask) } just Runs

        taskUseCase(testTask)

        coVerify(exactly = 1) { repository.deleteTask(testTask) }
    }

    @Test
    fun `invoke should remove subtask from task`() = runTest {
        val subtask = Subtask(id = 1, name = "To Delete")
        val task = Task(id = 1, name = "Task", subtasks = listOf(subtask))
        coEvery { repository.getTask(1) } returns task
        coEvery { repository.saveTask(any()) } just Runs

        val result = subtaskUseCase(1, 1)

        assertEquals(0, result.subtasks.size)
        coVerify { repository.saveTask(any()) }
    }
}