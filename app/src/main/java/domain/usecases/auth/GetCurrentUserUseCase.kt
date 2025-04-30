package com.elena.autoplanner.domain.usecases.auth

import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.domain.repositories.UserRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase(private val userRepository: UserRepository) {
    operator fun invoke(): Flow<User?> = userRepository.getCurrentUser()
}