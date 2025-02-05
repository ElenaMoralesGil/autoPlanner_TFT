package com.elena.autoplanner.data

import app.cash.turbine.test
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.repository.TaskRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

import com.elena.autoplanner.data.local.entities.*
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.IntervalUnit
import java.time.LocalDateTime



class TaskRepositoryImplTest {

    private lateinit var taskDao: TaskDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var repeatConfigDao: RepeatConfigDao
    private lateinit var subtaskDao: SubtaskDao

    private lateinit var repository: TaskRepositoryImpl

    // Dispatcher de prueba para ejecutar en un entorno controlado
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        taskDao = mockk(relaxed = true)
        reminderDao = mockk(relaxed = true)
        repeatConfigDao = mockk(relaxed = true)
        subtaskDao = mockk(relaxed = true)

        repository = TaskRepositoryImpl(
            taskDao = taskDao,
            reminderDao = reminderDao,
            repeatConfigDao = repeatConfigDao,
            subtaskDao = subtaskDao
        )
    }

    /** ----------------------------- GET TASKS TESTS  -----------------------------------------**/

    @Test
    fun `getTasks returns empty list when no tasks are available`() = runTest(testDispatcher) {
        // Configuración: el DAO retorna una lista vacía
        coEvery { taskDao.getAllTasks() } returns flowOf(emptyList())

        // Ejecución y aserción utilizando Turbine
        repository.getTasks().test {
            val tasks = awaitItem()
            assertEquals(0, tasks.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTasks returns list of tasks with associated entities`() = runTest(testDispatcher) {
        // Se crea un taskEntity simulado, suponiendo que el método toDomain se encarga de mapear el DAO a Task
        val taskEntity = /* Instanciar o simular un taskEntity con identificador 1 */
        // Se recomienda crear un objeto de tipo TaskEntity o utilizar un stub adecuado
            // Aquí se usa una función ficticia para representar dicha creación:
            createFakeTaskEntity(id = 1)

        // Se simula que el DAO devuelve un flujo con la lista que contiene el taskEntity
        coEvery { taskDao.getAllTasks() } returns flowOf(listOf(taskEntity))

        // Se simulan los valores de las demás entidades relacionadas: reminders, repeats y subtasks
        coEvery { reminderDao.getRemindersForTask(1) } returns flowOf(listOf(createFakeReminder( taskId = 1)))
        coEvery { repeatConfigDao.getRepeatConfigsForTask(1) } returns flowOf(listOf(createFakeRepeatConfig( taskId = 1)))
        coEvery { subtaskDao.getSubtasksForTask(1) } returns flowOf(listOf(createFakeSubtask( taskId = 1)))

        // Ejecución de la prueba del Flow
        repository.getTasks().test {
            val tasks = awaitItem()
            // Se asume que la función toDomain de taskEntity genera un objeto Task con los datos proporcionados
            assertEquals(1, tasks.size)
            val task = tasks.first()
            // Realizar aserciones sobre el objeto Task
            // Por ejemplo, verificar que las listas de entidades asociadas se han mapeado correctamente
            assertEquals(1, task.subtasks.size)
            // Se pueden agregar más aserciones según el mapeo esperado
            cancelAndConsumeRemainingEvents()
        }
    }




    private fun createFakeTaskEntity(id: Int): TaskEntity {
        return TaskEntity(
            id = id,
            name = "Tarea $id",
            isCompleted = false,
            isExpired = false,
            priority = "MEDIUM",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = LocalDateTime.now().plusHours(2),
            endDayPeriod = "EVENING",
            durationMinutes = 120
        )
    }


    private fun createFakeReminder(taskId: Int): ReminderEntity {
        return ReminderEntity(
            id = taskId,
            taskId = taskId,
            mode = "PRESET_OFFSET",
            offsetMinutes = 15,
            exactDateTime = null
        )
    }


    private fun createFakeRepeatConfig(taskId: Int): RepeatConfigEntity {
        return RepeatConfigEntity(
            id = taskId,
            taskId = taskId,
            frequencyType = "WEEKLY",
            interval = 1,
            intervalUnit = IntervalUnit.WEEK,
            selectedDays = setOf(DayOfWeek.MON, DayOfWeek.WED)
        )
    }


    private fun createFakeSubtask(taskId: Int): SubtaskEntity {
        return SubtaskEntity(
            id = taskId,
            parentTaskId = taskId,
            name = "Subtarea para la tarea $taskId",
            isCompleted = false,
            estimatedDurationInMinutes = 30
        )
    }

}
