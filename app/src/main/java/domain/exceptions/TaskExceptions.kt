package com.elena.autoplanner.domain.exceptions

import com.elena.autoplanner.domain.models.ErrorCode

class InvalidTaskException(message: String) :
    DomainException(message)

class TaskNotFoundException(id: Int) :
    DomainException("Task with id $id not found")

class TaskValidationException(errorCode: ErrorCode) : Exception(errorCode.name)