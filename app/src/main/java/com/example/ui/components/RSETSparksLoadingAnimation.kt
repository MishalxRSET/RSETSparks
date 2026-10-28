package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.RoseHighlight
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium brand loading animation featuring the authentic, unwarped RSET Sparks emblem,
 * dynamic pulsing auras, orbiting celestial spark particles, and animated brand copy.
 */
@Composable
fun RSETSparksLoadingAnimation(
    modifier: Modifier = Modifier,
    statusText: String = "Connecting Rajagiri Campus...",
    logoSize: Dp = 110.dp,
    showBrandTitle: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rset_sparks_loading")

    // Pulse scale for ambient aura
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_1"
    )

    // Pulse alpha for ambient aura
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_2"
    )

    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_2"
    )

    // Continuous 360-degree rotation for orbiting sparks
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Breathing scale for logo core
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // Animated dots stagger
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha_1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha_2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha_3"
    )

    Column(
        modifier = modifier
            .testTag("rset_sparks_loading_animation")
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(logoSize + 60.dp),
            contentAlignment = Alignment.Center
        ) {
            // First expanding aura wave
            Box(
                modifier = Modifier
                    .size(logoSize + 30.dp)
                    .scale(pulseScale1)
                    .alpha(pulseAlpha1)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(CoralAccent.copy(alpha = 0.6f), CrimsonPrimary.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Second expanding aura wave
            Box(
                modifier = Modifier
                    .size(logoSize + 15.dp)
                    .scale(pulseScale2)
                    .alpha(pulseAlpha2)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(RoseHighlight.copy(alpha = 0.7f), CoralAccent.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Orbiting sweep gradient ring with orbiting spark particles
            Box(
                modifier = Modifier
                    .size(logoSize + 36.dp)
                    .drawBehind {
                        val strokeWidth = 3.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val centerOffset = Offset(size.width / 2, size.height / 2)

                        // Outer dashed/smooth accent track
                        drawCircle(
                            color = CoralAccent.copy(alpha = 0.15f),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeWidth)
                        )

                        // Orbiting arc
                        val angleRad = Math.toRadians(rotationAngle.toDouble())
                        val sparkX = centerOffset.x + radius * cos(angleRad).toFloat()
                        val sparkY = centerOffset.y + radius * sin(angleRad).toFloat()
                        val oppAngleRad = Math.toRadians((rotationAngle + 180.0) % 360.0)
                        val oppSparkX = centerOffset.x + radius * cos(oppAngleRad).toFloat()
                        val oppSparkY = centerOffset.y + radius * sin(oppAngleRad).toFloat()

                        // Leading orbiting spark
                        drawCircle(
                            color = CoralAccent,
                            radius = 4.5.dp.toPx(),
                            center = Offset(sparkX, sparkY)
                        )
                        drawCircle(
                            color = AmberGold,
                            radius = 2.5.dp.toPx(),
                            center = Offset(sparkX, sparkY)
                        )

                        // Secondary counter spark
                        drawCircle(
                            color = CrimsonPrimary,
                            radius = 3.5.dp.toPx(),
                            center = Offset(oppSparkX, oppSparkY)
                        )
                    }
            )

            // Core Emblem Container with breathing effect
            Surface(
                modifier = Modifier
                    .size(logoSize)
                    .scale(breathScale)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = CrimsonPrimary)
                    .clip(CircleShape),
                shape = CircleShape,
                color = CreamIvory
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_rset_sparks_logo),
                    contentDescription = "RSET Sparks Official Emblem",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (showBrandTitle) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "RSET",
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    letterSpacing = 1.sp,
                    color = CrimsonPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sparks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = 0.5.sp,
                    color = CoralAccent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Status text with pulsating dots
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = statusText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Animated pulsing dots
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .alpha(dotAlpha1)
                        .background(CoralAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .alpha(dotAlpha2)
                        .background(CoralAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .alpha(dotAlpha3)
                        .background(CrimsonPrimary, CircleShape)
                )
            }
        }
    }
}

/**
 * Fullscreen Brand Splash & Loading Screen displayed during app initialization.
 */
@Composable
fun RSETSparksSplashScreen(
    modifier: Modifier = Modifier,
    statusText: String = "Connecting Rajagiri Campus..."
) {
    val isDark = isSystemInDarkTheme()
    val bgGradient = if (isDark) {
        listOf(Slate900, Color(0xFF280712), Slate900)
    } else {
        listOf(CreamIvory, Color(0xFFFDF5F6), CreamIvory)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("rset_sparks_splash_screen")
            .background(Brush.verticalGradient(bgGradient)),
        contentAlignment = Alignment.Center
    ) {
        RSETSparksLoadingAnimation(
            statusText = statusText,
            logoSize = 130.dp,
            showBrandTitle = true
        )

        // Bottom campus motto tagline
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LEARN • CONNECT • EXCEL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = CrimsonPrimary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Rajagiri School of Engineering & Technology",
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Semi-transparent modal loading dialog for background actions (login, registration, data sync).
 */
@Composable
fun RSETSparksLoadingOverlay(
    isLoading: Boolean,
    statusText: String = "Please wait...",
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("rset_sparks_loading_overlay")
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier.padding(32.dp)
                ) {
                    RSETSparksLoadingAnimation(
                        statusText = statusText,
                        logoSize = 85.dp,
                        showBrandTitle = false,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp)
                    )
                }
            }
        }
    }
}
