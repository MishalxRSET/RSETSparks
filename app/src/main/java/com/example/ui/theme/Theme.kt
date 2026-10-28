package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE4E9),
    onPrimaryContainer = DeepCrimson,
    secondary = CoralAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDE8EC),
    onSecondaryContainer = CrimsonPrimary,
    tertiary = CrownWine,
    onTertiary = Color.White,
    background = CreamIvory,
    onBackground = Slate900,
    surface = CreamIvoryLight,
    onSurface = Slate900,
    surfaceVariant = Color(0xFFF3EAE6),
    onSurfaceVariant = Slate600,
    surfaceContainer = Color(0xFFF6EFEB),
    surfaceContainerHigh = Color(0xFFF1E8E4),
    outline = Color(0xFFB7959E),
    outlineVariant = Color(0xFFE6D6DA),
    error = SafetyFlagRed,
    onError = Color.White
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = CoralAccent,
    onPrimary = Color.White,
    primaryContainer = CrimsonPrimary,
    onPrimaryContainer = RoseHighlight,
    secondary = SparkCoral,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = RoseHighlight,
    tertiary = AmberGold,
    onTertiary = Slate900,
    background = Slate900,
    onBackground = CreamIvory,
    surface = Slate800,
    onSurface = CreamIvory,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    surfaceContainer = Color(0xFF331019),
    surfaceContainerHigh = Color(0xFF3B1420),
    outline = Slate400,
    outlineVariant = Slate600,
    error = Color(0xFFF87171),
    onError = Slate900
  )

private val RSETShapes =
  Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = RSETShapes, content = content)
}
