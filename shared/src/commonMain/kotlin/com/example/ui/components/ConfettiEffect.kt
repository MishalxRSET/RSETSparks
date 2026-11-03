package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }

    val particles = remember {
        val colors = listOf(
            Color(0xFFE11D48), // Crimson
            Color(0xFFFB7185), // Coral
            Color(0xFFF59E0B), // Amber Gold
            Color(0xFF3B82F6), // Sky Blue
            Color(0xFF10B981), // Emerald
            Color(0xFFFDA4AF)  // Rose
        )
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                initialY = -Random.nextFloat() * 0.3f,
                speed = 0.7f + Random.nextFloat() * 0.6f,
                size = 12f + Random.nextFloat() * 16f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = Random.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val currentY = (p.initialY + progress.value * p.speed) * height
            val currentX = p.x * width + kotlin.math.sin(progress.value * 10f + p.x * 20f) * 30f

            if (currentY in 0f..height) {
                drawRect(
                    color = p.color.copy(alpha = (1f - progress.value * 0.8f).coerceIn(0f, 1f)),
                    topLeft = Offset(currentX, currentY),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
