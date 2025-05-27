package com.elena.autoplanner.domain.usecases.subtasks

import android.util.Log
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class DeleteSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) {
    suspend operator fun invoke(taskId: Int, subtaskIdToDelete: Int): TaskResult<Task> {

        val taskResult = getTaskUseCase(taskId)
        if (taskResult !is TaskResult.Success) {
            return taskResult 
        }
        val task = taskResult.data


        val updatedSubtasks = task.subtasks.filter { it.id != subtaskIdToDelete }


        if (updatedSubtasks.size == task.subtasks.size) {


            Log.w(
                "DeleteSubtaskUseCase",
                "Subtask with ID $subtaskIdToDelete not found in task $taskId for deletion."
            )

            return TaskResult.Success(task) 
        }


        val updatedTaskObject = Task.from(task).subtasks(updatedSubtasks).build()


        return when (val saveResult = saveTaskUseCase(updatedTaskObject)) {
            is TaskResult.Success -> {

                getTaskUseCase(taskId)
            }

            is TaskResult.Error -> saveResult 
        }
    }
}