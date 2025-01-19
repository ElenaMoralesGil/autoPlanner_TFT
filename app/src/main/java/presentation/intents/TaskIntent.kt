package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.domain.models.Reminder
import com.elena.autoplanner.domain.models.RepeatConfig
import domain.models.Subtask
import domain.models.Priority
import java.time.LocalDateTime

/**
 * Acciones que la UI puede disparar relacionadas con las Tareas.
 */
sealed class TaskIntent : BaseIntent() {

    object LoadTasks : TaskIntent()

    /**
     * Crea una nueva Tarea con la información capturada en la pantalla AddTask.
     */
    data class AddTask(
        val name: String,
        val priority: Priority,
        val startDate: LocalDateTime?,
        val endDate: LocalDateTime?,
        val durationInMinutes: Int?,
        val reminders: List<Reminder> = emptyList(),
        val repeatConfig: RepeatConfig?,
        val subtasks: List<Subtask>
    ) : TaskIntent()

}
