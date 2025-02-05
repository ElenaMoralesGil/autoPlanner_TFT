package com.elena.autoplanner.domain.usecases;

import com.elena.autoplanner.domain.models.Task;
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.usecases.AddTaskUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class AddTaskUseCaseTest {

    @MockK
    private lateinit var repository: TaskRepository

    private lateinit var addTaskUseCase: AddTaskUseCase;

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        addTaskUseCase = AddTaskUseCase(repository)
    }

    @Test
    fun testAddNewTaskWithIdZero_callsSaveTask() = runTest {
        val t1 = Task(id = 1, name = "Task 1")

        coEvery { repository.saveTask(t1) } returns Unit

        addTaskUseCase.invoke(t1)

        coVerify(exactly = 1) { repository.saveTask(t1) }
    }
}

