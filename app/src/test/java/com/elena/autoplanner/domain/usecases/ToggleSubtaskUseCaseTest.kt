package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ToggleSubtaskUseCaseTest {
    private val repository: TaskRepository = mockk()
    private val useCase = ToggleSubtaskUseCase(repository)

    @Test
    fun `invoke should toggle subtask completion`() = runTest {
        val subtask = Subtask(id = 1, name = "Subtask", isCompleted = false)
        val task = Task(id = 1, name = "Task", subtasks = listOf(subtask))
        coEvery { repository.getTask(1) } returns task
        coEvery { repository.saveTask(any()) } just Runs

        val result = useCase(1, 1, true)

        assertTrue(result.subtasks.first().isCompleted)
        coVerify { repository.saveTask(any()) }
    }
}