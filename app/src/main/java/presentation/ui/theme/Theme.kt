package presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF9800),
    secondary = Color(0xFF03DAC5)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}
