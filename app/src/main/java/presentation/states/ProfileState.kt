package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.ProfileStats
import com.elena.autoplanner.domain.models.User

data class ProfileState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val stats: ProfileStats? = null,
    val error: String? = null,
    val showDeleteConfirmDialog: Boolean = false,
) {
    val isLoggedIn: Boolean get() = user != null
}