package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class MoreIntent : Intent {
    data class SelectList(val listId: Long) : MoreIntent()
    data class CreateList(val name: String, val colorHex: String) : MoreIntent()
    data class ToggleListExpansion(val listId: Long) : MoreIntent()
    data object RequestCreateList : MoreIntent()
}