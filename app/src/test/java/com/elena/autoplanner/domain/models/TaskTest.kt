package com.elena.autoplanner.domain.models

import com.elena.autoplanner.domain.exceptions.TaskValidationException
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class TaskTest {

    @Test
    fun `task builder creates valid task`() {
        val startTime = LocalDateTime.now()
        val task = Task.Builder()
            .name("Test Task")
            .priority(Priority.HIGH)
            .startDateConf(TimePlanning(startTime, DayPeriod.MORNING))
            .build()

        assertEquals("Test Task", task.name)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals(startTime, task.startDateConf.dateTime)
        assertEquals(DayPeriod.MORNING, task.startDateConf.dayPeriod)
    }

    @Test(expected = TaskValidationException::class)
    fun `task with empty name throws validation exception`() {
        Task.Builder()
            .name("")
            .build()
    }

    @Test(expected = TaskValidationException::class)
    fun `task with end date before start date throws exception`() {
        val startTime = LocalDateTime.now()
        val endTime = startTime.minusHours(1)

        Task.Builder()
            .name("Test Task")
            .startDateConf(TimePlanning(startTime))
            .endDateConf(TimePlanning(endTime))
            .build()
    }

    @Test(expected = TaskValidationException::class)
    fun `task with negative duration throws exception`() {
        Task.Builder()
            .name("Test Task")
            .durationConf(DurationPlan(-30))
            .build()
    }

    @Test
    fun `task is expired when end date is in past`() {
        val pastTime = LocalDateTime.now().minusDays(1)
        val task = Task.Builder()
            .name("Test Task")
            .startDateConf(TimePlanning(LocalDateTime.now().minusDays(2)))
            .endDateConf(TimePlanning(pastTime))
            .build()
        assertTrue(task.isExpired())
    }

    @Test
    fun `task is not expired when end date is in future`() {
        val futureTime = LocalDateTime.now().plusDays(1)
        val task = Task.Builder()
            .name("Test Task")
            .endDateConf(TimePlanning(futureTime))
            .build()

        assertFalse(task.isExpired())
    }

    @Test
    fun `task copy builder preserves all properties`() {
        val original = Task.Builder()
            .name("Original Task")
            .priority(Priority.HIGH)
            .startDateConf(TimePlanning(LocalDateTime.now()))
            .build()

        val copy = Task.from(original)
            .name("Modified Task")
            .build()

        assertEquals("Modified Task", copy.name)
        assertEquals(original.priority, copy.priority)
        assertEquals(original.startDateConf, copy.startDateConf)
    }
}