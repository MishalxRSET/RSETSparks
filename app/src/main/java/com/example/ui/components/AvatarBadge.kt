package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CampusVerifiedGreen
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.CoralAccent

val avatarGradientPalettes = listOf(
    listOf(Color(0xFFE11D48), Color(0xFFFB7185)), // Crimson to Coral
    listOf(Color(0xFF8B5CF6), Color(0xFFC084FC)), // Violet to Purple
    listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8)), // Sky to Cyan
    listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)), // Amber to Yellow
    listOf(Color(0xFF10B981), Color(0xFF34D399)), // Emerald to Mint
    listOf(Color(0xFFEC4899), Color(0xFFF472B6)), // Pink to Rose
    listOf(Color(0xFF6366F1), Color(0xFFA5B4FC)), // Indigo to Periwinkle
    listOf(Color(0xFFEF4444), Color(0xFFF87171)), // Red to Light Red
    listOf(Color(0xFF0F172A), Color(0xFF334155))  // Slate Dark
)

@Composable
fun StudentAvatar(
    name: String,
    emoji: String = "🎓",
    colorIndex: Int = 0,
    photoUri: String? = null,
    size: Dp = 56.dp,
    isVerified: Boolean = true,
    isAdmin: Boolean = false,
    showOfficerBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveColorIndex = if (isAdmin && !showOfficerBadge && colorIndex == 8) 0 else colorIndex
    val effectiveEmoji = if (isAdmin && !showOfficerBadge && emoji == "🛡️") "🎓" else emoji
    val palette = avatarGradientPalettes.getOrElse(effectiveColorIndex.coerceAtLeast(0) % avatarGradientPalettes.size) {
        listOf(CrimsonPrimary, CoralAccent)
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(palette))
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "$name's Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (effectiveEmoji.isNotBlank()) {
                Text(
                    text = effectiveEmoji,
                    fontSize = (size.value * 0.45f).sp
                )
            } else {
                Text(
                    text = name.take(2).uppercase(),
                    color = Color.White,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }

        // Only show campus safety officer shield if isAdmin AND showOfficerBadge is explicitly enabled
        if (isAdmin && showOfficerBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size((size.value * 0.38f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "RSETSparks moderator",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size((size.value * 0.25f).dp)
                )
            }
        } else if (isVerified) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size((size.value * 0.38f).dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified Rajagiri Student",
                    tint = CampusVerifiedGreen,
                    modifier = Modifier.size((size.value * 0.34f).dp)
                )
            }
        }
    }
}
