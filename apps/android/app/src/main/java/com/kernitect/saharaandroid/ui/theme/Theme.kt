package com.kernitect.saharaandroid.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SaharaColorScheme = lightColorScheme(

    primary = Color(0xFFE60000),

    onPrimary = Color.White,

    secondary = Color(0xFFFF5A5F),

    onSecondary = Color.White,

    background = Color.White,

    onBackground = Color.Black,

    surface = Color.White,

    onSurface = Color.Black,

    surfaceVariant = Color(0xFFF0F0F0),

    onSurfaceVariant = Color.Black
)

@Composable
fun SaharaAndroidTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {

        SideEffect {

            val window =
                (view.context as Activity).window

            /*
             * Force Android system bars to white too.
             */
            window.statusBarColor =
                Color.White.toArgb()

            window.navigationBarColor =
                Color.White.toArgb()

            WindowCompat
                .getInsetsController(
                    window,
                    view
                )
                .apply {

                    /*
                     * Dark icons on white background.
                     */
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
        }
    }

    MaterialTheme(
        colorScheme = SaharaColorScheme,
        typography = Typography,
        content = content
    )
}