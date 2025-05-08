package com.elena.autoplanner.widgets

import android.content.Context
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

// Action to refresh a widget
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters
    ) {
        // This will trigger the widget to update itself
        // For DailyWidget
        DailyWidget().update(context, glanceId)
        // For WeeklyWidget
        WeeklyWidget().update(context, glanceId)
    }
}