package com.elena.autoplanner.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightColorScheme = lightColorScheme(
    tertiary = Color(0xFFF8BBD0),
    onTertiary = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF424242),
    surfaceVariant = Color(0xFFF5F5F5),
    outline = Color(0xFFBDBDBD),
    error = Color(0xFFEF9A9A),
    errorContainer = Color(0xFFFFCDD2), 

    primary = Color(0xFFF89A34),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8B6),
    onPrimaryContainer = Color(0xFF212121),

    secondary = Color(0xFF424242),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBDBDBD),
    onSecondaryContainer = Color(0xFF212121),
)


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF89A34),
    onPrimary = Color(0xFF212121),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF212121),
    tertiary = Color(0xFFF8BBD0),
    onTertiary = Color(0xFF212121),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    outline = Color(0xFF757575),
    error = Color(0xFFCF6679),
    errorContainer = Color(0xFFB71C1C) 
)

private val AppTypography = Typography(

)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
