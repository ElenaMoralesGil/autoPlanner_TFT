package com.elena.autoplanner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.elena.autoplanner.domain.models.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDateTime

class NotificationSchedulerTest {

    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var alarmManager: AlarmManager
    @Mock
    private lateinit var mockPendingIntent: PendingIntent

    private lateinit var notificationScheduler: NotificationScheduler

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        notificationScheduler = NotificationScheduler(context, alarmManager)
    }

    @Test
    fun `calculate reminder time for preset offset`() {
        val startTime = LocalDateTime.now().plusHours(2)
        val task = Task.Builder()
            .name("Test Task")
            .startDateConf(TimePlanning(startTime))
            .reminderPlan(
                ReminderPlan(
                    mode = ReminderMode.PRESET_OFFSET,
                    offsetMinutes = 30
                )
            )
            .build()

        val reminderTime = notificationScheduler.calculateReminderTime(task)

        assertEquals(startTime.minusMinutes(30), reminderTime)
    }

    @Test
    fun `calculate reminder time for exact datetime`() {
        val exactTime = LocalDateTime.now().plusHours(1)
        val task = Task.Builder()
            .name("Test Task")
            .reminderPlan(
                ReminderPlan(
                    mode = ReminderMode.EXACT,
                    exactDateTime = exactTime
                )
            )
            .build()

        val reminderTime = notificationScheduler.calculateReminderTime(task)

        assertEquals(exactTime, reminderTime)
    }

    @Test
    fun `calculate reminder time for custom reminder`() {
        val startTime = LocalDateTime.now().plusDays(2)
        val task = Task.Builder()
            .name("Test Task")
            .startDateConf(TimePlanning(startTime))
            .reminderPlan(
                ReminderPlan(
                    mode = ReminderMode.CUSTOM,
                    customDayOffset = 1,
                    customHour = 9,
                    customMinute = 30
                )
            )
            .build()

        val reminderTime = notificationScheduler.calculateReminderTime(task)
        val expectedTime =
            startTime.minusDays(1).withHour(9).withMinute(30).withSecond(0).withNano(0)

        assertEquals(expectedTime, reminderTime)
    }

    @Test
    fun `calculate reminder time returns null for no reminder`() {
        val task = Task.Builder()
            .name("Test Task")
            .reminderPlan(ReminderPlan(mode = ReminderMode.NONE))
            .build()

        val reminderTime = notificationScheduler.calculateReminderTime(task)

        assertNull(reminderTime)
    }

    @Test
    fun `calculate reminder time handles null start time for preset offset`() {

        val task = Task.Builder()
            .name("Test Task")
            .reminderPlan(
                ReminderPlan(
                    mode = ReminderMode.PRESET_OFFSET,
                    offsetMinutes = 30
                )
            )
            .build()
        val startDate = task.startDateConf.dateTime
        if (startDate == null) {
            val reminderTime = notificationScheduler.calculateReminderTime(task)
            assertNull("Should return null when start date is null", reminderTime)
        } else {
            val reminderTime = notificationScheduler.calculateReminderTime(task)
            assertNotNull(
                "Should calculate reminder time when start date is provided",
                reminderTime
            )
            assertEquals(
                "Should subtract offset from start time",
                startDate.minusMinutes(30),
                reminderTime
            )
        }
    }

    @Test
    fun `calculate reminder time handles null offset minutes`() {
        val startTime = LocalDateTime.now().plusHours(2)
        val task = Task.Builder()
            .name("Test Task")
            .startDateConf(TimePlanning(startTime))
            .reminderPlan(
                ReminderPlan(
                    mode = ReminderMode.PRESET_OFFSET
                )
            )
            .build()

        val reminderTime = notificationScheduler.calculateReminderTime(task)
        assertEquals(startTime, reminderTime)
    }

    @Test
    fun `schedule notification calls alarm manager method`() {
        mockStatic(PendingIntent::class.java).use { mockedStatic ->
            mockedStatic.`when`<PendingIntent> {
                PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
            }.thenReturn(mockPendingIntent)

            val task = Task.Builder()
                .id(123)
                .name("Test Task")
                .startDateConf(TimePlanning(LocalDateTime.now().plusHours(2)))
                .reminderPlan(
                    ReminderPlan(
                        mode = ReminderMode.PRESET_OFFSET,
                        offsetMinutes = 60
                    )
                )
                .build()

            notificationScheduler.scheduleNotification(task)
            verify(alarmManager).set(
                eq(AlarmManager.RTC_WAKEUP),
                any<Long>(),
                eq(mockPendingIntent)
            )
        }
    }

    @Test
    fun `schedule notification with exact alarms permission`() {
        mockStatic(PendingIntent::class.java).use { mockedStatic ->
            mockedStatic.`when`<PendingIntent> {
                PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
            }.thenReturn(mockPendingIntent)

            val task = Task.Builder()
                .id(123)
                .name("Test Task")
                .startDateConf(TimePlanning(LocalDateTime.now().plusHours(2)))
                .reminderPlan(
                    ReminderPlan(
                        mode = ReminderMode.PRESET_OFFSET,
                        offsetMinutes = 60
                    )
                )
                .build()
            whenever(alarmManager.canScheduleExactAlarms()).thenReturn(true)

            notificationScheduler.scheduleNotification(task)
            verify(alarmManager).set(
                eq(AlarmManager.RTC_WAKEUP),
                any<Long>(),
                eq(mockPendingIntent)
            )
        }
    }

    @Test
    fun `schedule notification without exact alarms permission`() {
        mockStatic(PendingIntent::class.java).use { mockedStatic ->
            mockedStatic.`when`<PendingIntent> {
                PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
            }.thenReturn(mockPendingIntent)

            val task = Task.Builder()
                .id(456)
                .name("Test Task")
                .startDateConf(TimePlanning(LocalDateTime.now().plusHours(2)))
                .reminderPlan(
                    ReminderPlan(
                        mode = ReminderMode.PRESET_OFFSET,
                        offsetMinutes = 30
                    )
                )
                .build()
            whenever(alarmManager.canScheduleExactAlarms()).thenReturn(false)

            notificationScheduler.scheduleNotification(task)
            verify(alarmManager).set(
                eq(AlarmManager.RTC_WAKEUP),
                any<Long>(),
                eq(mockPendingIntent)
            )
        }
    }

    @Test
    fun `cancel notification with mocked pending intent`() {
        mockStatic(PendingIntent::class.java).use { mockedStatic ->
            mockedStatic.`when`<PendingIntent> {
                PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
            }.thenReturn(mockPendingIntent)

            val taskId = 456

            notificationScheduler.cancelNotification(taskId)

            verify(alarmManager).cancel(eq(mockPendingIntent))
        }
    }

    @Test
    fun `schedule notification skips past reminder times`() {
        val pastTime = LocalDateTime.now().minusHours(2)
        val task = Task.Builder()
            .id(789)
            .name("Past Task")
            .startDateConf(TimePlanning(pastTime))
            .reminderPlan(
                ReminderPlan(
                    mode = ReminderMode.PRESET_OFFSET,
                    offsetMinutes = 30
                )
            )
            .build()

        notificationScheduler.scheduleNotification(task)
        verify(alarmManager, never()).setExactAndAllowWhileIdle(any(), any(), any())
        verify(alarmManager, never()).setAndAllowWhileIdle(any(), any(), any())
        verify(alarmManager, never()).set(any(), any(), any())
    }

    @Test
    fun `schedule notification shows immediate notification for recent past times`() {
        mockStatic(PendingIntent::class.java).use { mockedStatic ->
            mockedStatic.`when`<PendingIntent> {
                PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
            }.thenReturn(mockPendingIntent)

            val recentPastTime = LocalDateTime.now().minusMinutes(2)
            val task = Task.Builder()
                .id(999)
                .name("Recently Past Task")
                .startDateConf(TimePlanning(recentPastTime))
                .reminderPlan(
                    ReminderPlan(
                        mode = ReminderMode.PRESET_OFFSET,
                        offsetMinutes = 0
                    )
                )
                .build()

            notificationScheduler.scheduleNotification(task)
            verify(context).sendBroadcast(any<Intent>())
        }
    }

    @Test
    fun `schedule notification handles no reminder plan`() {
        mockStatic(PendingIntent::class.java).use { mockedStatic ->
            mockedStatic.`when`<PendingIntent> {
                PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
            }.thenReturn(mockPendingIntent)

            val task = Task.Builder()
                .id(101)
                .name("No Reminder Task")
                .startDateConf(TimePlanning(LocalDateTime.now().plusHours(1)))
                .build()

            notificationScheduler.scheduleNotification(task)
            verify(alarmManager).cancel(eq(mockPendingIntent))
            verify(alarmManager, never()).setExactAndAllowWhileIdle(any(), any(), any())
            verify(alarmManager, never()).setAndAllowWhileIdle(any(), any(), any())
            verify(alarmManager, never()).set(any(), any(), any())
        }
    }
}