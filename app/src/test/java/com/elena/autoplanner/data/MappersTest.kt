package com.elena.autoplanner.data

import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.data.mappers.toDomain
import com.elena.autoplanner.data.mappers.toEntity
import com.elena.autoplanner.data.mappers.toTaskEntity
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.IntervalUnit
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MappersTest {

    @Test
    fun `TaskEntity toDomain maps correctly`() {

        val taskEntity = TaskEntity(
            id = 1,
            name = "Test Task",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = LocalDateTime.of(2023, 1, 1, 10, 0),
            startDayPeriod = "MORNING",
            endDateTime = LocalDateTime.of(2023, 1, 1, 12, 0),
            endDayPeriod = "EVENING",
            durationMinutes = 120
        )

        val reminderEntity = ReminderEntity(
            id = 10,
            taskId = 1,
            mode = "PRESET_OFFSET",
            offsetMinutes = 15,
            exactDateTime = null
        )
        val repeatConfigEntity = RepeatConfigEntity(
            id = 11,
            taskId = 1,
            frequencyType = "WEEKLY",
            interval = 1,
            intervalUnit = IntervalUnit.WEEK,
            selectedDays = setOf(DayOfWeek.MON, DayOfWeek.FRI)
        )
        val subtaskEntity = SubtaskEntity(
            id = 12,
            parentTaskId = 1,
            name = "Subtask 1",
            isCompleted = false,
            estimatedDurationInMinutes = 30
        )

        val taskDomain = taskEntity.toDomain(
            reminders = listOf(reminderEntity),
            repeatConfigs = listOf(repeatConfigEntity),
            subtasks = listOf(subtaskEntity)
        )


        assertEquals(1, taskDomain.id)
        assertEquals("Test Task", taskDomain.name)
        assertEquals(Priority.HIGH, taskDomain.priority)
        assertNotNull(taskDomain.startDateConf)
        assertEquals(LocalDateTime.of(2023, 1, 1, 10, 0), taskDomain.startDateConf?.dateTime)

        assertNotNull(taskDomain.reminderPlan)

    }

    @Test
    fun `ReminderEntity toDomain returns valid ReminderPlan`() {

        val reminderEntity = ReminderEntity(
            id = 10,
            taskId = 1,
            mode = "PRESET_OFFSET",
            offsetMinutes = 15,
            exactDateTime = null
        )


        val reminderPlan = reminderEntity.toDomain()

        assertEquals(ReminderMode.PRESET_OFFSET, reminderPlan.mode)
        assertEquals(15, reminderPlan.offsetMinutes)
        assertNull(reminderPlan.exactDateTime)
    }

    @Test
    fun `RepeatConfigEntity toDomain returns valid RepeatPlan`() {

        val repeatConfigEntity = RepeatConfigEntity(
            id = 11,
            taskId = 1,
            frequencyType = "WEEKLY",
            interval = 1,
            intervalUnit = IntervalUnit.WEEK,
            selectedDays = setOf(DayOfWeek.TUE, DayOfWeek.THU)
        )

        val repeatPlan = repeatConfigEntity.toDomain()

        assertEquals(FrequencyType.WEEKLY, repeatPlan.frequencyType)
        assertEquals(1, repeatPlan.interval)
        assertEquals(IntervalUnit.WEEK, repeatPlan.intervalUnit)
        assertEquals(setOf(DayOfWeek.TUE, DayOfWeek.THU), repeatPlan.selectedDays)
    }

    @Test
    fun `SubtaskEntity toDomain returns valid Subtask`() {

        val subtaskEntity = SubtaskEntity(
            id = 12,
            parentTaskId = 1,
            name = "Subtask 1",
            isCompleted = false,
            estimatedDurationInMinutes = 30
        )

        val subtask = subtaskEntity.toDomain()

        assertEquals(12, subtask.id)
        assertEquals("Subtask 1", subtask.name)
        assertFalse(subtask.isCompleted)
        assertEquals(30, subtask.estimatedDurationInMinutes)
    }

    @Test
    fun `TaskEntity with all nullable fields as null converts to domain object with null configurations`() {
        val taskEntity = TaskEntity(
            id = 1,
            name = "Test Task",
            isCompleted = false,
            priority = "NONE",
            startDateTime = null,
            startDayPeriod = null,
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = null
        )

        val task = taskEntity.toDomain(emptyList(), emptyList(), emptyList())
        assertNull(task.startDateConf)
        assertNull(task.endDateConf)
        assertEquals(Priority.NONE, task.priority)
        assertNull(task.durationConf)
        assertNull(task.reminderPlan)
        assertNull(task.repeatPlan)
        assertTrue(task.subtasks.isEmpty())

    }

    @Test
    fun `Task to TaskEntity maps correctly`() {
        val task = Task(
            id = 1,
            name = "Test Task",
            priority = Priority.HIGH,
            startDateConf = TimePlanning(
                dateTime = LocalDateTime.of(2023, 1, 1, 10, 0),
                dayPeriod = DayPeriod.MORNING
            ),
            endDateConf = TimePlanning(
                dateTime = LocalDateTime.of(2023, 1, 1, 12, 0),
                dayPeriod = DayPeriod.EVENING
            ),
            durationConf = DurationPlan(120),
            reminderPlan = ReminderPlan(ReminderMode.PRESET_OFFSET, 15, null),
            repeatPlan = RepeatPlan(FrequencyType.WEEKLY, 1, IntervalUnit.WEEK, setOf(DayOfWeek.MON)),
            subtasks = listOf(Subtask(12, "Subtask 1", false, 30))
        )

        val entity = task.toTaskEntity()

        assertEquals(1, entity.id)
        assertEquals("Test Task", entity.name)
        assertEquals("HIGH", entity.priority)
        assertEquals(LocalDateTime.of(2023, 1, 1, 10, 0), entity.startDateTime)
        assertEquals("MORNING", entity.startDayPeriod)
        assertEquals(120, entity.durationMinutes)
    }

    @Test
    fun `TaskEntity with empty reminders converts correctly`() {
        val taskEntity = TaskEntity(
            id = 1,
            name = "Test Task",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = LocalDateTime.of(2023, 1, 1, 10, 0),
            startDayPeriod = "MORNING",
            endDateTime = LocalDateTime.of(2023, 1, 1, 12, 0),
            endDayPeriod = "EVENING",
            durationMinutes = 120
        )

        val taskDomain = taskEntity.toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )

        assertNull(taskDomain.reminderPlan)
        assertNull(taskDomain.repeatPlan)
        assertTrue(taskDomain.subtasks.isEmpty())
    }

    @Test
    fun `ReminderPlan toEntity maps correctly`() {
        val plan = ReminderPlan(
            mode = ReminderMode.EXACT,
            offsetMinutes = null,
            exactDateTime = LocalDateTime.of(2023, 1, 1, 9, 30)
        )

        val entity = plan.toEntity(taskId = 1)

        assertEquals(1, entity.taskId)
        assertEquals("EXACT", entity.mode)
        assertNull(entity.offsetMinutes)
        assertEquals(LocalDateTime.of(2023, 1, 1, 9, 30), entity.exactDateTime)
    }

    @Test
    fun `RepeatPlan toEntity maps correctly`() {
        val plan = RepeatPlan(
            frequencyType = FrequencyType.MONTHLY,
            interval = 2,
            intervalUnit = IntervalUnit.MONTH,
            selectedDays = setOf(DayOfWeek.TUE)
        )

        val entity = plan.toEntity(taskId = 1)

        assertEquals(1, entity.taskId)
        assertEquals("MONTHLY", entity.frequencyType)
        assertEquals(2, entity.interval)
        assertEquals(IntervalUnit.MONTH, entity.intervalUnit)
        assertEquals(setOf(DayOfWeek.TUE), entity.selectedDays)
    }

    @Test
    fun `Subtask toEntity maps correctly`() {
        val subtask = Subtask(
            id = 12,
            name = "Subtask 1",
            isCompleted = true,
            estimatedDurationInMinutes = 45
        )

        val entity = subtask.toEntity(taskId = 1)

        assertEquals(1, entity.parentTaskId)
        assertEquals("Subtask 1", entity.name)
        assertTrue(entity.isCompleted)
        assertEquals(45, entity.estimatedDurationInMinutes)
    }

    @Test
    fun `Task with all nullable fields as null converts to taskEntity object with null configurations`(){
        val task = Task(
            id = 1,
            name = "Test Task",
            priority = Priority.NONE,
            startDateConf = null,
            endDateConf = null,
            durationConf = null,
            reminderPlan = null,
            repeatPlan = null,
            subtasks = emptyList()
        )

        val entity = task.toTaskEntity()

        assertEquals(1, entity.id)
        assertEquals("Test Task", entity.name)
        assertEquals(Priority.NONE, task.priority)
        assertNull(entity.startDateTime)
        assertNull(entity.startDayPeriod)
        assertNull(entity.endDateTime)
        assertNull(entity.endDayPeriod)
        assertNull(entity.durationMinutes)
    }
}
