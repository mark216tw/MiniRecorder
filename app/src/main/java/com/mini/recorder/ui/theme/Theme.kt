package com.mini.recorder.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mini.recorder.data.prefs.ThemeMode

@Suppress("DEPRECATION")
@Composable
fun MiniRecorderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeHue: Int = DEFAULT_THEME_HUE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = remember(themeHue, darkTheme) {
        if (darkTheme) darkSchemeFor(themeHue) else lightSchemeFor(themeHue)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
