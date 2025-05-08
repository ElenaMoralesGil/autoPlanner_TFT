package com.elena.autoplanner.presentation.ui.screens.more.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll // Import updateAll

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        // This will re-compose all instances of both widget types.
        DailyWidget().updateAll(context)
        WeeklyWidget().updateAll(context)
    }
}