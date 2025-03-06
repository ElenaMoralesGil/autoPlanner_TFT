package com.elena.autoplanner.domain.exceptions

sealed class DomainException(message: String, cause: Throwable? = null) :
    Exception(message, cause)