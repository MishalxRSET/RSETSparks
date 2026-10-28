package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CoralAccent,
    onPrimary = Color.White,
    primaryContainer = DeepCrimson,
    onPrimaryContainer = RoseHighlight,
    secondary = RoseHighlight,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = RoseHighlight,
    tertiary = AmberGold,
    background = Slate900,
    onBackground = CreamIvory,
    surface = Slate800,
    onSurface = CreamIvory,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    error = SafetyFlagRed,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE9ED),
    onPrimaryContainer = CrimsonPrimary,
    secondary = CoralAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6ECEE),
    onSecondaryContainer = CrimsonPrimary,
    tertiary = AmberGold,
    background = CreamIvoryLight,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Color(0xFFF7F1EE),
    onSurfaceVariant = Slate600,
    error = SafetyFlagRed,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
