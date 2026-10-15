package com.mini.recorder.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

const val DEFAULT_THEME_HUE = 226
const val PRESET_SATURATION = 0.62f
const val PRESET_LIGHTNESS = 0.50f

data class ThemePreset(val name: String, val hue: Int)

val ThemePresets = listOf(
    ThemePreset("藍", 226),
    ThemePreset("青綠", 176),
    ThemePreset("綠", 142),
    ThemePreset("琥珀橙", 38),
    ThemePreset("玫瑰粉", 338),
    ThemePreset("紫", 270)
)

fun hueColor(
    hue: Int,
    saturation: Float = PRESET_SATURATION,
    lightness: Float = PRESET_LIGHTNESS
): Color = Color.hsl(hue.toFloat(), saturation, lightness)

fun lightSchemeFor(hue: Int): ColorScheme {
    val h = hue.toFloat()
    val secondaryHue = (hue + 150) % 360
    val tertiaryHue = (hue + 60) % 360
    return lightColorScheme(
        primary = Color.hsl(h, PRESET_SATURATION, 0.45f),
        onPrimary = Color.White,
        primaryContainer = Color.hsl(h, 0.70f, 0.90f),
        onPrimaryContainer = Color.hsl(h, 0.60f, 0.20f),
        secondary = Color.hsl(secondaryHue.toFloat(), 0.35f, 0.45f),
        onSecondary = Color.White,
        secondaryContainer = Color.hsl(h, 0.45f, 0.90f),
        onSecondaryContainer = Color.hsl(h, 0.50f, 0.22f),
        tertiary = Color.hsl(tertiaryHue.toFloat(), 0.55f, 0.48f),
        onTertiary = Color.White,
        error = Danger
    )
}

fun darkSchemeFor(hue: Int): ColorScheme {
    val h = hue.toFloat()
    val secondaryHue = (hue + 150) % 360
    val tertiaryHue = (hue + 60) % 360
    return darkColorScheme(
        primary = Color.hsl(h, 0.60f, 0.72f),
        onPrimary = Color.hsl(h, 0.70f, 0.18f),
        primaryContainer = Color.hsl(h, 0.45f, 0.32f),
        onPrimaryContainer = Color.hsl(h, 0.80f, 0.92f),
        secondary = Color.hsl(secondaryHue.toFloat(), 0.30f, 0.72f),
        onSecondary = Color.hsl(secondaryHue.toFloat(), 0.60f, 0.18f),
        secondaryContainer = Color.hsl(h, 0.35f, 0.30f),
        onSecondaryContainer = Color.hsl(h, 0.70f, 0.90f),
        tertiary = Color.hsl(tertiaryHue.toFloat(), 0.45f, 0.72f),
        onTertiary = Color.hsl(tertiaryHue.toFloat(), 0.60f, 0.18f),
        error = Danger
    )
}
