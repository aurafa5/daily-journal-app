package com.example.projectutsmscindy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue, onPrimary = Color.White, primaryContainer = BlueLight, onPrimaryContainer = Charcoal,
    secondary = Color(0xFF4C76B8), background = BlueBackground, onBackground = Charcoal,
    surface = SurfaceWhite, onSurface = Charcoal, surfaceVariant = Color(0xFFF0F2ED),
    onSurfaceVariant = MutedGray, outline = Color(0xFFBFC5BD), outlineVariant = SoftOutline, error = ErrorRed
)
private val DarkColorScheme = darkColorScheme(
    primary = BlueDark, onPrimary = DarkBackground, primaryContainer = Color(0xFF183C73), onPrimaryContainer = Color(0xFFDCEBFF),
    background = DarkBackground, onBackground = Color(0xFFE4E8E3), surface = DarkSurface, onSurface = Color(0xFFE4E8E3),
    surfaceVariant = Color(0xFF303A34), onSurfaceVariant = Color(0xFFC0C8C1), outlineVariant = DarkOutline, error = Color(0xFFFFB4AB)
)

@Composable
fun ProjectUTSMsCindyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
}
