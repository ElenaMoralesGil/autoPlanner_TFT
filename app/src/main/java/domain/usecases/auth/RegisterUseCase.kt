package com.elena.autoplanner.domain.usecases.auth

import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.results.AuthResult
import com.elena.autoplanner.domain.repositories.UserRepository

class RegisterUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult<User> =
        userRepository.register(email, password)
}