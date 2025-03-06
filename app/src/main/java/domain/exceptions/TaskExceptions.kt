package com.elena.autoplanner.domain.exceptions

class InvalidTaskException(message: String) :
    DomainException(message)

class TaskNotFoundException(id: Int) :
    DomainException("Task with id $id not found")

class TaskValidationException(message: String) :
    DomainException(message)