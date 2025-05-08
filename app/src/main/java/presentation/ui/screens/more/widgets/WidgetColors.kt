package com.elena.autoplanner.presentation.ui.screens.more.widgets

import android.annotation.SuppressLint
import androidx.compose.ui.graphics.Color // Jetpack Compose Color
import androidx.glance.unit.ColorProvider // Glance ColorProvider

@SuppressLint("RestrictedApi")
// Removed @SuppressLint("RestrictedApi") - not standard for object declarations
object WidgetColors {

    // For main titles and prominent accents
    val titleText = ColorProvider(Color(0xFFF89A34))        // AppTheme.primary (Cute Light Orange)
    val accent = ColorProvider(Color(0xFFF89A34))           // AppTheme.primary

    // For general text content
    val primaryText = ColorProvider(Color(0xFF424242))      // AppTheme.onSurface (Dark Gray)
    val secondaryText =
        ColorProvider(Color(0xFF757575))    // A slightly lighter gray than onSurface, good for secondary info (custom)

    // or ColorProvider(Color(0xFFBDBDBD)) // AppTheme.secondaryContainer (if it fits)
    val tertiaryText =
        ColorProvider(Color(0xFFBDBDBD))     // AppTheme.outline or AppTheme.secondaryContainer (Medium Gray)

    // Widget Backgrounds
    // Option 1: Using a color from your theme like surfaceVariant
    val widgetBackground =
        ColorProvider(Color(0xFFF5F5F5)) // AppTheme.surfaceVariant (Very Light Gray)
    // Option 2: Using the peach color you had before if it's a specific widget branding
    // val widgetBackground = ColorProvider(Color(0xFFECA692))

    // Item Backgrounds
    val itemBackground = ColorProvider(Color(0xFFFFFFFF))   // AppTheme.surface (White)
    val itemStroke =
        ColorProvider(Color(0xFFE0E0E0))       // A lighter outline than AppTheme.outline (custom light gray)
    // or ColorProvider(Color(0xFFBDBDBD)) // AppTheme.outline

    // Daily/Weekly Specific Backgrounds (if different from general widgetBackground)
    // For simplicity, let's assume they use the same widgetBackground for now.
    // If you need them distinct:
    // val dailyWidgetBackground = ColorProvider(Color(0xFFF5F5F5))  // e.g., AppTheme.surfaceVariant
    // val weeklyWidgetBackground = ColorProvider(Color(0xFFF0F8FF)) // e.g., a custom very light blue

    // Weekly Widget Day Headers and Highlights
    val dayHeaderBackground =
        ColorProvider(Color(0x66FFD8B6)) // AppTheme.primaryContainer (Light Orange) with ~40% alpha
    val dayHeaderText =
        ColorProvider(Color(0xFF212121))        // AppTheme.onPrimaryContainer (Dark Gray)

    val todayHighlightBackground =
        ColorProvider(Color(0xFFFFD8B6)) // AppTheme.primaryContainer (Light Orange)
    val todayHighlightText =
        ColorProvider(Color(0xFF212121))    // AppTheme.onPrimaryContainer (Dark Gray)

    // Specific backgrounds for items under "today" or empty states (using alpha)
    val todayTaskItemBackground =
        ColorProvider(Color(0x99FFD8B6)) // AppTheme.primaryContainer with ~60% alpha
    val todayEmptyDayBackground =
        ColorProvider(Color(0x4DFFD8B6)) // AppTheme.primaryContainer with ~30% alpha
    val emptyDayBackground =
        ColorProvider(Color(0x80FFFFFF))      // AppTheme.surface (White) with 50% alpha
}