package com.elena.autoplanner.domain.models

import androidx.compose.ui.graphics.Color
import com.elena.autoplanner.domain.exceptions.TaskValidationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

enum class ErrorCode {
    TASK_NAME_EMPTY,
    START_AFTER_END,
    NEGATIVE_DURATION,
}

data class TaskInternalFlags(
    var isOverdue: Boolean = false,
    var constraintDate: LocalDate? = null,
    var failedPeriod: Boolean = false,
    var isHardConflict: Boolean = false,
    var isPostponed: Boolean = false,
    var needsManualResolution: Boolean = false,
    var isMarkedForDeletion: Boolean = false,
)

data class Task private constructor(
    val id: Int,
    val name: String,
    val isCompleted: Boolean,
    val priority: Priority,
    val startDateConf: TimePlanning,
    val endDateConf: TimePlanning?,
    val durationConf: DurationPlan?,
    val reminderPlan: ReminderPlan?,
    val repeatPlan: RepeatPlan?,
    val subtasks: List<Subtask>,
    val completionDateTime: LocalDateTime? = null,
    @Transient var internalFlags: TaskInternalFlags? = null,
    val scheduledStartDateTime: LocalDateTime? = null,
    val scheduledEndDateTime: LocalDateTime? = null,
    val listId: Long? = null,
    val sectionId: Long? = null,
    val displayOrder: Int = 0,
    @Transient val listName: String? = null,
    @Transient val sectionName: String? = null,
    @Transient val listColor: Color? = null,
    val allowSplitting: Boolean? = null,

    ) {
    fun validate() {
        if (name.isBlank()) {
            throw TaskValidationException(ErrorCode.TASK_NAME_EMPTY)
        }

        if (endDateConf != null && startDateConf.dateTime?.isAfter(endDateConf.dateTime) == true) {
            throw TaskValidationException(ErrorCode.START_AFTER_END)
        }

        durationConf?.let {
            if (it.totalMinutes != null && it.totalMinutes < 0) {
                throw TaskValidationException(ErrorCode.NEGATIVE_DURATION)
            }
        }
    }

    fun isExpired(): Boolean {
        val now = LocalDateTime.now()
        return if (endDateConf?.dateTime != null) {

            endDateConf.dateTime.isBefore(now)
        } else if (startDateConf.dateTime != null && durationConf?.totalMinutes == null) {


            startDateConf.dateTime.toLocalDate().isBefore(now.toLocalDate())
        } else {

            false
        }
    }
    fun isDueOn(date: LocalDate): Boolean =
        startDateConf.dateTime?.toLocalDate() == date

    fun isAllDay(): Boolean =
        startDateConf.dayPeriod == DayPeriod.ALLDAY

    fun isDueToday(): Boolean =
        startDateConf.dateTime?.toLocalDate() == LocalDate.now()

    fun isDueThisWeek(): Boolean {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)
        return startDateConf.dateTime?.toLocalDate()?.let {
            !it.isBefore(startOfWeek) && !it.isAfter(endOfWeek)
        } == true
    }

    fun isDueThisMonth(): Boolean =
        startDateConf.dateTime?.toLocalDate()?.let { it.month == LocalDate.now().month } == true

    fun copyForPlanning(flags: TaskInternalFlags? = this.internalFlags): Task {
        val newTask = this.copy() 
        newTask.internalFlags = flags
        return newTask
    }

    val hasPeriod: Boolean = startDateConf.dayPeriod != DayPeriod.NONE

    val startTime: LocalTime
        get() = startDateConf.dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT

    val effectiveDurationMinutes: Int
        get() = (durationConf?.totalMinutes ?: 60).coerceAtLeast(0)

    val effectiveAllowSplitting: Boolean
        get() = when {
            allowSplitting != null -> allowSplitting
            effectiveDurationMinutes >= 30 -> true
            else -> false
        }

    val shouldShowSplittingOption: Boolean
        get() = effectiveDurationMinutes >= 30


    class Builder {
        private var id: Int = 0
        private var name: String = ""
        private var isCompleted: Boolean = false
        private var priority: Priority = Priority.NONE
        private var startDateConf: TimePlanning? = null
        private var endDateConf: TimePlanning? = null
        private var durationConf: DurationPlan? = null
        private var reminderPlan: ReminderPlan? = null
        private var repeatPlan: RepeatPlan? = null
        private var subtasks: List<Subtask> = emptyList()
        private var completionDateTime: LocalDateTime? = null
        private var scheduledStartDateTime: LocalDateTime? = null
        private var scheduledEndDateTime: LocalDateTime? = null
        private var allowSplitting: Boolean? = null
        private var listId: Long? = null
        private var sectionId: Long? = null
        private var displayOrder: Int = 0
        private var listName: String? = null
        private var sectionName: String? = null
        private var listColor: Color? = null
        private var internalFlags: TaskInternalFlags? = null

        fun id(id: Int) = apply { this.id = id }
        fun name(name: String) = apply { this.name = name }
        fun isCompleted(isCompleted: Boolean) = apply { this.isCompleted = isCompleted }
        fun priority(priority: Priority) = apply { this.priority = priority }
        fun startDateConf(startDateConf: TimePlanning?) =
            apply { this.startDateConf = startDateConf }
        fun allowSplitting(allowSplitting: Boolean?) =
            apply { this.allowSplitting = allowSplitting }
        fun endDateConf(endDateConf: TimePlanning?) = apply { this.endDateConf = endDateConf }
        fun durationConf(durationConf: DurationPlan?) = apply { this.durationConf = durationConf }
        fun reminderPlan(reminderPlan: ReminderPlan?) = apply { this.reminderPlan = reminderPlan }
        fun repeatPlan(repeatPlan: RepeatPlan?) = apply { this.repeatPlan = repeatPlan }
        fun subtasks(subtasks: List<Subtask>) = apply { this.subtasks = subtasks }
        fun completionDateTime(dateTime: LocalDateTime?) =
            apply { this.completionDateTime = dateTime }
        fun scheduledStartDateTime(dateTime: LocalDateTime?) =
            apply { this.scheduledStartDateTime = dateTime }

        fun scheduledEndDateTime(dateTime: LocalDateTime?) =
            apply { this.scheduledEndDateTime = dateTime }

        fun listId(listId: Long?) = apply { this.listId = listId }
        fun sectionId(sectionId: Long?) = apply { this.sectionId = sectionId }
        fun displayOrder(displayOrder: Int) = apply { this.displayOrder = displayOrder }
        fun listName(name: String?) = apply { this.listName = name }
        fun sectionName(name: String?) = apply { this.sectionName = name }
        fun listColor(color: Color?) = apply { this.listColor = color }
        fun internalFlags(flags: TaskInternalFlags?) = apply { this.internalFlags = flags }
        fun build(): Task {
            val finalSectionId = if (listId == null) null else sectionId
            val effectiveStartDate = startDateConf ?: TimePlanning(
                dateTime = LocalDateTime.now(),
                dayPeriod = DayPeriod.NONE
            )

            val task = Task(
                id = id,
                name = name,
                isCompleted = isCompleted,
                priority = priority,
                startDateConf = effectiveStartDate,
                endDateConf = endDateConf,
                durationConf = durationConf,
                reminderPlan = reminderPlan,
                repeatPlan = repeatPlan,
                subtasks = subtasks,
                scheduledStartDateTime = scheduledStartDateTime,
                scheduledEndDateTime = scheduledEndDateTime,
                completionDateTime = completionDateTime,
                listId = listId,
                internalFlags = internalFlags,
                sectionId = finalSectionId,
                displayOrder = displayOrder,
                listName = listName,
                sectionName = sectionName,
                listColor = listColor,
                allowSplitting = allowSplitting

            )
            task.validate()
            return task
        }
    }

    companion object {
        fun create(name: String, startDateTime: LocalDateTime? = LocalDateTime.now()): Task {
            return Builder()
                .name(name)
                .startDateConf(TimePlanning(dateTime = startDateTime))
                .build()
        }

        fun from(task: Task): Builder {
            return Builder()
                .id(task.id)
                .name(task.name)
                .isCompleted(task.isCompleted)
                .priority(task.priority)
                .startDateConf(task.startDateConf)
                .endDateConf(task.endDateConf)
                .durationConf(task.durationConf)
                .reminderPlan(task.reminderPlan)
                .repeatPlan(task.repeatPlan)
                .subtasks(task.subtasks)
                .scheduledStartDateTime(task.scheduledStartDateTime)
                .scheduledEndDateTime(task.scheduledEndDateTime)
                .completionDateTime(task.completionDateTime)
                .listId(task.listId)
                .sectionId(task.sectionId)
                .displayOrder(task.displayOrder)
                .internalFlags(task.internalFlags)
                .listName(task.listName)
                .sectionName(task.sectionName)
                .listColor(task.listColor)
        }
    }
}

fun LocalDate.isToday(): Boolean = this == LocalDate.now()

enum class Priority {
    HIGH, MEDIUM, LOW, NONE
}

data class TimePlanning(
    val dateTime: LocalDateTime?,
    val dayPeriod: DayPeriod = DayPeriod.NONE,
) {
    init {
        if (dayPeriod == DayPeriod.ALLDAY && dateTime != null) {
            dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0)
        }
    }
}

enum class DayPeriod {
    MORNING, EVENING, NIGHT, ALLDAY, NONE
}

data class DurationPlan(
    val totalMinutes: Int?,
)