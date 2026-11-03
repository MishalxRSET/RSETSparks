package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The crest is set in a classical serif, so display/headline/title styles use the platform serif
// while body and label text stay in the default sans for readability.
private val BrandSerif = FontFamily.Serif
private val Base = Typography()

val Typography =
  Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = BrandSerif, fontWeight = FontWeight.Bold),
    displayMedium = Base.displayMedium.copy(fontFamily = BrandSerif, fontWeight = FontWeight.Bold),
    displaySmall = Base.displaySmall.copy(fontFamily = BrandSerif, fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = BrandSerif, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = BrandSerif, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = BrandSerif, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = BrandSerif, fontWeight = FontWeight.Bold),
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      ),
  )
