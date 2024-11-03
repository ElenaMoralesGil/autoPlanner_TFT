package data.mappers

import data.local.TaskEntity
import domain.models.Task

fun TaskEntity.toDomain(): Task {
    return Task(
        id = this.id,
        name = this.name,
        deadline = this.deadline,
        isCompleted = this.isCompleted,
        isExpired = this.isExpired
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        name = this.name,
        deadline = this.deadline,
        isCompleted = this.isCompleted,
        isExpired = this.isExpired
    )
}
