package com.aistudio.calculator.ywrbt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ThemeDarkSlate = darkColorScheme(
    primary = AccentBlue,
    secondary = ButtonSlate,
    tertiary = AccentRose,
    background = DarkSlateBg,
    surface = CardSlate,
    onPrimary = DarkSlateBg,
    onSecondary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

private val ThemeMidnightCosmic = darkColorScheme(
    primary = Color(0xFFC084FC), // Lavender / Purple
    secondary = Color(0xFF1E1B4B), // Indigo Button
    tertiary = Color(0xFFEC4899), // Pink details
    background = Color(0xFF03001C), // Galactic dark background
    surface = Color(0xFF1B1236), // Cosmic Deep Purple surface
    onPrimary = Color(0xFF03001C),
    onSecondary = Color(0xFFF8FAFC),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val ThemeForestEmerald = darkColorScheme(
    primary = Color(0xFF34D399), // Emerald Accent
    secondary = Color(0xFF064E3B), // Deep Forest Buttons
    tertiary = Color(0xFF059669),
    background = Color(0xFF022C22), // Minty Deep Blackish background
    surface = Color(0xFF064E3B), // Jungle Card Surface
    onPrimary = Color(0xFF022C22),
    onSecondary = Color(0xFFF8FAFC),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val ThemeRoyalAmethyst = darkColorScheme(
    primary = Color(0xFFE879F9), // Amethyst lavender Accent
    secondary = Color(0xFF2E0249), // Vivid purple button
    tertiary = Color(0xFFA855F7),
    background = Color(0xFF1F0033), // Deep Royal background
    surface = Color(0xFF3F0071), // Amethyst premium surface
    onPrimary = Color(0xFF1F0033),
    onSecondary = Color(0xFFF8FAFC),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val ThemeLoveVelvet = darkColorScheme(
    primary = Color(0xFFFF5277), // Soft Cupid Rose
    secondary = Color(0xFF5E1B2C), // Deep Velvet Wine button
    tertiary = Color(0xFFFF8DA1), // Soft sweet bubble pink
    background = Color(0xFF1E050B), // Silky dark cherry background
    surface = Color(0xFF3B0E1B), // Soft heart velvet surface card
    onPrimary = Color(0xFF1E050B),
    onSecondary = Color(0xFFFFF0F3),
    onBackground = Color(0xFFFFF0F3),
    onSurface = Color(0xFFFFF0F3)
)

@Composable
fun CalculatorVaultTheme(
    themeName: String = "theme_dark_slate",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "theme_midnight_cosmic" -> ThemeMidnightCosmic
        "theme_forest_emerald" -> ThemeForestEmerald
        "theme_royal_amethyst" -> ThemeRoyalAmethyst
        "theme_love_velvet" -> ThemeLoveVelvet
        else -> ThemeDarkSlate
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
