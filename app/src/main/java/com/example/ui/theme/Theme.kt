package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF4338CA),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = TealAccent,
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = DarkSurface,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkCard,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoOnContainer,
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealOnContainer,
    background = LightSurface,
    onBackground = Color(0xFF0F172A),
    surface = LightCard,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
