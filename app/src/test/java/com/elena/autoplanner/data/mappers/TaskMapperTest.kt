package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.ReminderEntity
import com.elena.autoplanner.data.entities.SubtaskEntity
import com.elena.autoplanner.data.entities.TaskEntity
import com.elena.autoplanner.domain.models.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class TaskMapperTest {

    private lateinit var taskMapper: TaskMapper

    @Before
    fun setup() {
        taskMapper = TaskMapper()
    }

    @Test
    fun `map entity to domain preserves all fields`() {
        val startTime = LocalDateTime.now()
        val taskEntity = TaskEntity(
            id = 1,
            name = "Test Task",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = startTime,
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 60,
            listId = 5L,
            sectionId = 10L,
            displayOrder = 2
        )

        val reminders = listOf(
            ReminderEntity(1, 1, "PRESET_OFFSET", 30, null)
        )

        val subtasks = listOf(
            SubtaskEntity(1, 1, "Subtask 1", false, 30)
        )
        val domainTask = taskMapper.mapToDomain(
            taskEntity = taskEntity,
            reminders = reminders,
            subtasks = subtasks,
            listName = "Test List",
            sectionName = "Test Section"
        )
        assertEquals(1, domainTask.id)
        assertEquals("Test Task", domainTask.name)
        assertFalse(domainTask.isCompleted)
        assertEquals(Priority.HIGH, domainTask.priority)
        assertEquals(startTime, domainTask.startDateConf.dateTime)
        assertEquals(DayPeriod.MORNING, domainTask.startDateConf.dayPeriod)
        assertEquals(60, domainTask.durationConf?.totalMinutes)
        assertEquals(5L, domainTask.listId)
        assertEquals(10L, domainTask.sectionId)
        assertEquals(2, domainTask.displayOrder)
        assertEquals("Test List", domainTask.listName)
        assertEquals("Test Section", domainTask.sectionName)
        assertEquals(1, domainTask.subtasks.size)
        assertEquals("Subtask 1", domainTask.subtasks.first().name)
        assertNotNull(domainTask.reminderPlan)
        assertEquals(ReminderMode.PRESET_OFFSET, domainTask.reminderPlan?.mode)
    }

    @Test
    fun `map domain to entity preserves all fields`() {
        val startTime = LocalDateTime.now()
        val domainTask = Task.Builder()
            .id(1)
            .name("Test Task")
            .isCompleted(true)
            .priority(Priority.MEDIUM)
            .startDateConf(TimePlanning(startTime, DayPeriod.EVENING))
            .durationConf(DurationPlan(90))
            .listId(3L)
            .sectionId(7L)
            .displayOrder(1)
            .build()
        val entityTask = taskMapper.mapToEntity(domainTask)
        assertEquals(1, entityTask.id)
        assertEquals("Test Task", entityTask.name)
        assertTrue(entityTask.isCompleted)
        assertEquals("MEDIUM", entityTask.priority)
        assertEquals(startTime, entityTask.startDateTime)
        assertEquals("EVENING", entityTask.startDayPeriod)
        assertEquals(90, entityTask.durationMinutes)
        assertEquals(3L, entityTask.listId)
        assertEquals(7L, entityTask.sectionId)
        assertEquals(1, entityTask.displayOrder)
        assertFalse(entityTask.isDeleted)
    }

    @Test
    fun `map priority handles invalid enum gracefully`() {
        val priority = taskMapper.mapPriority("INVALID_PRIORITY")
        assertEquals(Priority.NONE, priority)
    }

    @Test
    fun `map priority handles valid enum`() {
        val priority = taskMapper.mapPriority("HIGH")
        assertEquals(Priority.HIGH, priority)
    }
}