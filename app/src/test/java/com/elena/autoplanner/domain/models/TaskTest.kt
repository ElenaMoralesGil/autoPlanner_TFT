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

    @Test
    fun `task with very long name`() {
        val longName = "A".repeat(1000)
        val task = Task.Builder()
            .name(longName)
            .build()

        assertEquals(longName, task.name)
        assertEquals(1000, task.name.length)
    }

    @Test
    fun `task with unicode characters in name`() {
        val unicodeName = "📝 タスク с русским текстом और हिंदी"
        val task = Task.Builder()
            .name(unicodeName)
            .build()

        assertEquals(unicodeName, task.name)
    }

    @Test
    fun `task with extreme dates`() {
        val veryFarFuture = LocalDateTime.of(2099, 12, 31, 23, 59, 59)
        val veryFarPast = LocalDateTime.of(1900, 1, 1, 0, 0, 0)

        val futureTask = Task.Builder()
            .name("Future Task")
            .startDateConf(TimePlanning(veryFarFuture))
            .build()

        val pastTask = Task.Builder()
            .name("Past Task")
            .startDateConf(TimePlanning(veryFarPast))
            .build()

        assertEquals(veryFarFuture, futureTask.startDateConf.dateTime)
        assertEquals(veryFarPast, pastTask.startDateConf.dateTime)
        assertTrue(pastTask.isExpired())
        assertFalse(futureTask.isExpired())
    }

    @Test
    fun `task with maximum subtasks`() {
        val subtasks = (1..100).map {
            Subtask(it, "Subtask $it", false, 30)
        }

        val task = Task.Builder()
            .name("Task with many subtasks")
            .subtasks(subtasks)
            .build()

        assertEquals(100, task.subtasks.size)
        assertEquals("Subtask 1", task.subtasks.first().name)
        assertEquals("Subtask 100", task.subtasks.last().name)
    }

    @Test
    fun `task with zero duration`() {
        val task = Task.Builder()
            .name("Zero Duration Task")
            .durationConf(DurationPlan(0))
            .build()

        assertEquals(0, task.durationConf?.totalMinutes)
        assertEquals(0, task.effectiveDurationMinutes)
    }

    @Test
    fun `task with very long duration`() {
        val task = Task.Builder()
            .name("Very Long Task")
            .durationConf(DurationPlan(525600))
            .build()

        assertEquals(525600, task.durationConf?.totalMinutes)
        assertEquals(525600, task.effectiveDurationMinutes)
    }

    @Test
    fun `task at midnight boundaries - not expired`() {
        val now = LocalDateTime.now()
        val todayMidnight = now.toLocalDate().atStartOfDay()
        val futureTime = now.plusHours(1)

        val midnightTask = Task.Builder()
            .name("Midnight Task")
            .startDateConf(TimePlanning(todayMidnight))
            .endDateConf(TimePlanning(futureTime))
            .build()

        assertEquals(todayMidnight, midnightTask.startDateConf.dateTime)
        assertFalse(midnightTask.isExpired())
    }

    @Test
    fun `task is properly expired when end date is in the past`() {
        val pastTime = LocalDateTime.now().minusHours(1)
        val veryPastTime = LocalDateTime.now().minusHours(2)

        val expiredTask = Task.Builder()
            .name("Expired Task")
            .startDateConf(TimePlanning(veryPastTime))
            .endDateConf(TimePlanning(pastTime))
            .build()

        assertTrue(expiredTask.isExpired())
    }

    @Test
    fun `task at midnight boundaries`() {
        val now = LocalDateTime.now()
        val todayMidnight = now.toLocalDate().atStartOfDay()
        val tomorrowMidnight = todayMidnight.plusDays(1)

        val midnightTask = Task.Builder()
            .name("Midnight Task")
            .startDateConf(TimePlanning(todayMidnight))
            .endDateConf(TimePlanning(tomorrowMidnight.plusMinutes(1)))
            .build()

        assertEquals(todayMidnight, midnightTask.startDateConf.dateTime)
        assertFalse(midnightTask.isExpired())
    }

    @Test
    fun `task with same start and end time`() {
        val sameTime = LocalDateTime.now().plusDays(1)

        val task = Task.Builder()
            .name("Same Time Task")
            .startDateConf(TimePlanning(sameTime))
            .endDateConf(TimePlanning(sameTime))
            .build()

        assertEquals(sameTime, task.startDateConf.dateTime)
        assertEquals(sameTime, task.endDateConf?.dateTime)
    }

    @Test
    fun `task with leap year date`() {
        val leapYearDate = LocalDateTime.of(2024, 2, 29, 12, 0, 0)

        val task = Task.Builder()
            .name("Leap Year Task")
            .startDateConf(TimePlanning(leapYearDate))
            .build()

        assertEquals(leapYearDate, task.startDateConf.dateTime)
        assertEquals(29, task.startDateConf.dateTime?.dayOfMonth)
        assertEquals(2, task.startDateConf.dateTime?.monthValue)
    }
}