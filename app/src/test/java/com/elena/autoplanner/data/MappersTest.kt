package com.elena.autoplanner.data

import com.elena.autoplanner.data.mappers.toDomain

import com.elena.autoplanner.data.local.entities.*
import com.elena.autoplanner.data.mappers.toTaskEntity
import com.elena.autoplanner.domain.models.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class MappersTest {

    @Test
    fun `TaskEntity toDomain maps correctly`() {

        val taskEntity = TaskEntity(
            id = 1,
            name = "Test Task",
            isCompleted = false,
            isExpired = false,
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
}
