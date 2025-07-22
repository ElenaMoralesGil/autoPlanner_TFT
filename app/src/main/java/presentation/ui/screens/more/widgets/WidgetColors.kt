package com.elena.autoplanner.presentation.ui.screens.more.widgets

import android.annotation.SuppressLint
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider 

@SuppressLint("RestrictedApi")

object WidgetColors {

    val titleText = ColorProvider(Color(0xFFF89A34))
    val accent = ColorProvider(Color(0xFFF89A34))

    val primaryText = ColorProvider(Color(0xFF424242))      
    val secondaryText =
        ColorProvider(Color(0xFF757575))

    val tertiaryText =
        ColorProvider(Color(0xFFBDBDBD))     

    val widgetBackground =
        ColorProvider(Color(0xFFF5F5F5))

    val itemBackground = ColorProvider(Color(0xFFFFFFFF))   
    val itemStroke =
        ColorProvider(Color(0xFFE0E0E0))

    val dayHeaderBackground =
        ColorProvider(Color(0x66FFD8B6)) 
    val dayHeaderText =
        ColorProvider(Color(0xFF212121))        

    val todayHighlightBackground =
        ColorProvider(Color(0xFFFFD8B6)) 
    val todayHighlightText =
        ColorProvider(Color(0xFF212121))

    val todayTaskItemBackground =
        ColorProvider(Color(0x99FFD8B6)) 
    val todayEmptyDayBackground =
        ColorProvider(Color(0x4DFFD8B6)) 
    val emptyDayBackground =
        ColorProvider(Color(0x80FFFFFF))      
}