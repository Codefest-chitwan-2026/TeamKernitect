package com.kernitect.sahararesponder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ResponderColors = lightColorScheme(
    primary = Color(0xFFE60000), onPrimary = Color.White,
    secondary = Color(0xFFFF5A5F), onSecondary = Color.White,
    background = Color.White, onBackground = Color(0xFF151515),
    surface = Color.White, onSurface = Color(0xFF151515),
    surfaceVariant = Color(0xFFF3F4F6), onSurfaceVariant = Color(0xFF555B64),
    error = Color(0xFFE60000), errorContainer = Color(0xFFFFE7E7),
    onErrorContainer = Color(0xFF650000), outline = Color(0xFFD8DADF),
)

@Composable
fun SaharaResponderTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = Color.White.toArgb()
        window.navigationBarColor = Color.White.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(colorScheme = ResponderColors, typography = Typography, content = content)
}
