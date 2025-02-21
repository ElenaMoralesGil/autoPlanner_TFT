package com.elena.autoplanner.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme: Clean and soft with a cute light orange primary and dark grey secondary.
private val LightColorScheme = lightColorScheme(
    tertiary = Color(0xFFF8BBD0),      // Tertiary: Pastel pink for subtle highlights or metadata.
    onTertiary = Color(0xFF212121),    // On tertiary: Dark gray for legibility.
    surface = Color(0xFFFFFFFF),       // Surface: White background for screens, cards, and dialogs.
    onSurface = Color(0xFF424242),     // On surface: Dark gray for primary text and icons.
    surfaceVariant = Color(0xFFF5F5F5),// Surface variant: Very light gray for cards/dialogs.
    outline = Color(0xFFBDBDBD),       // Outline: Medium gray for borders and dividers.
    error = Color(0xFFEF9A9A),         // Error: Light red for error messages.
    errorContainer = Color(0xFFFFCDD2), // Error container: Very light red for error backgrounds.

    primary = Color(0xFFF89A34),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8B6),
    onPrimaryContainer = Color(0xFF212121),

    secondary = Color(0xFF424242),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBDBDBD),
    onSecondaryContainer = Color(0xFF212121),
)

// Dark Theme: Adapted for dark mode with the same primary and a complementary lighter grey secondary.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF89A34),       // Primary: Same light cute orange for consistency.
    onPrimary = Color(0xFF212121),     // On primary: Dark gray for text/icons on primary surfaces.
    secondary = Color(0xFFBDBDBD),     // Secondary: Lighter grey accent for dark mode.
    onSecondary = Color(0xFF212121),   // On secondary: Dark gray to ensure legibility.
    tertiary = Color(0xFFF8BBD0),      // Tertiary: Pastel pink for additional highlights.
    onTertiary = Color(0xFF212121),    // On tertiary: Dark gray for contrast.
    surface = Color(0xFF121212),       // Surface: Dark background for screens, cards, and dialogs.
    onSurface = Color(0xFFE0E0E0),     // On surface: Light gray for primary text and icons.
    surfaceVariant = Color(0xFF1E1E1E),// Surface variant: Medium dark gray for elevated surfaces.
    outline = Color(0xFF757575),       // Outline: Gray for borders and dividers.
    error = Color(0xFFCF6679),         // Error: Muted red for error messages.
    errorContainer = Color(0xFFB71C1C) // Error container: Dark red for error backgrounds.
)

private val AppTypography = Typography(
    // Customize typography if needed
)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
