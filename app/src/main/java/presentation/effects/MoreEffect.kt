package com.elena.autoplanner.presentation.effects

import android.content.ComponentName
import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class MoreEffect : UiEffect {
    data class NavigateToTasks(val listId: Long) : MoreEffect()
    data class ShowSnackbar(val message: String) : MoreEffect()
    data object ShowCreateListDialog : MoreEffect()
    data class TriggerWidgetPinRequest(val componentName: ComponentName) : MoreEffect()
}