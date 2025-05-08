package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class MoreIntent : Intent {
    data class SelectList(val listId: Long) : MoreIntent()
    data class CreateList(val name: String, val colorHex: String) : MoreIntent()
    data class ToggleListExpansion(val listId: Long) : MoreIntent()
    data object RequestCreateList : MoreIntent()
    data class CreateSection(val listId: Long, val sectionName: String) : MoreIntent()
    data class LoadSections(val listId: Long) : MoreIntent()
    data class RequestDeleteList(val listId: Long) : MoreIntent()
    data object ConfirmDeleteList : MoreIntent()
    data object CancelDeleteList : MoreIntent()

    data class RequestDeleteSection(val sectionId: Long, val listId: Long) : MoreIntent() // listId for context/reload
    data object ConfirmDeleteSection : MoreIntent()
    data object CancelDeleteSection : MoreIntent()

    data class UpdateList(val listId: Long, val newName: String, val newColorHex: String) : MoreIntent()
    data class UpdateSection(val sectionId: Long, val listId: Long, val newName: String) : MoreIntent()

    data object RequestAddDailyWidget : MoreIntent()
    data object RequestAddWeeklyWidget : MoreIntent()
}