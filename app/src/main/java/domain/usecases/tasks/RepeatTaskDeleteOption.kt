package com.elena.autoplanner.domain.usecases.tasks

/**
 * Opciones para eliminar tareas repetibles
 */
enum class RepeatTaskDeleteOption {
    /**
     * Eliminar solo esta instancia específica
     */
    THIS_INSTANCE_ONLY,

    /**
     * Eliminar esta instancia y todas las futuras
     */
    THIS_AND_FUTURE,

    /**
     * Eliminar todas las instancias (pasadas y futuras)
     */
    ALL_INSTANCES
}
