package com.elena.autoplanner.domain.usecases.auth

import com.elena.autoplanner.domain.repository.UserRepository

class LogoutUseCase(private val userRepository: UserRepository) {
    suspend operator fun invoke() = userRepository.logout()
}