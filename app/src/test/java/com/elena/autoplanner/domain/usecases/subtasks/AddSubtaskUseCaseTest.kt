package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class AddSubtaskUseCaseTest {

    @Mock
    private lateinit var getTaskUseCase: GetTaskUseCase

    @Mock
    private lateinit var saveTaskUseCase: SaveTaskUseCase

    private lateinit var addSubtaskUseCase: AddSubtaskUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        addSubtaskUseCase = AddSubtaskUseCase(getTaskUseCase, saveTaskUseCase)
    }

    @Test
    fun `add subtask success`() = runTest {

        val taskId = 1
        val subtaskName = "New Subtask"
        val originalTask = Task.Builder().id(taskId).name("Test Task").build()
        val updatedTask = Task.from(originalTask)
            .subtasks(listOf(Subtask(1, subtaskName, false)))
            .build()

        whenever(getTaskUseCase(taskId)).thenReturn(TaskResult.Success(originalTask))
        whenever(saveTaskUseCase(any())).thenReturn(TaskResult.Success(taskId))
        whenever(getTaskUseCase(taskId)).thenReturn(TaskResult.Success(updatedTask))

        val result = addSubtaskUseCase(taskId, subtaskName)

        assertTrue(result is TaskResult.Success)
        val returnedTask = (result as TaskResult.Success).data
        assertEquals(1, returnedTask.subtasks.size)
        assertEquals(subtaskName, returnedTask.subtasks.first().name)
    }

    @Test
    fun `add subtask fails with empty name`() = runTest {

        val result = addSubtaskUseCase(1, "")

        assertTrue(result is TaskResult.Error)
        assertEquals("Subtask name cannot be empty", (result as TaskResult.Error).message)
        verify(getTaskUseCase, never()).invoke(any())
    }

    @Test
    fun `add subtask fails when task not found`() = runTest {

        whenever(getTaskUseCase(1)).thenReturn(TaskResult.Error("Task not found"))

        val result = addSubtaskUseCase(1, "Subtask")

        assertTrue(result is TaskResult.Error)
        assertEquals("Task not found", (result as TaskResult.Error).message)
    }
}