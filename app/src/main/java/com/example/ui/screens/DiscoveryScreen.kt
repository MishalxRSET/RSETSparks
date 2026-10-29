package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CampusVerifiedGreen
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.NopeRed
import com.example.ui.theme.SuperSparkBlue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryScreen(
    candidates: List<UserEntity>,
    onSwipe: (UserEntity, String) -> Unit,
    onReportUser: (UserEntity) -> Unit,
    onResetDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember(candidates) { mutableStateOf(0) }
    var showBioDetails by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }

    val currentCandidate = candidates.getOrNull(currentIndex)
    val nextCandidate = candidates.getOrNull(currentIndex + 1)

    fun triggerSwipe(action: String) {
        val candidate = currentCandidate ?: return
        coroutineScope.launch {
            val targetX = when (action) {
                "LIKE" -> 1000f
                "PASS" -> -1000f
                else -> 0f
            }
            val targetY = if (action == "SUPER_SPARK") -1000f else 0f

            dragOffsetX.animateTo(targetX, spring(stiffness = Spring.StiffnessMedium))
            dragOffsetY.animateTo(targetY, spring(stiffness = Spring.StiffnessMedium))
            onSwipe(candidate, action)
            dragOffsetX.snapTo(0f)
            dragOffsetY.snapTo(0f)
            showBioDetails = false
            currentIndex++
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentCandidate == null) {
            // Deck Empty State
            EmptyDeckView(
                onReset = {
                    currentIndex = 0
                    onResetDeck()
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Card Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Underneath Card Preview (Next in deck)
                    if (nextCandidate != null) {
                        ProfileCardView(
                            candidate = nextCandidate,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(0.94f)
                                .offset(y = 12.dp)
                                .graphicsLayer { alpha = 0.85f },
                            onExpandClick = {}
                        )
                    }

                    // Top Active Swipable Card
                    ProfileCardView(
                        candidate = currentCandidate,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    dragOffsetX.value.roundToInt(),
                                    dragOffsetY.value.roundToInt()
                                )
                            }
                            .graphicsLayer {
                                rotationZ = (dragOffsetX.value / 25f).coerceIn(-25f, 25f)
                            }
                            .pointerInput(currentCandidate.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val x = dragOffsetX.value
                                        val y = dragOffsetY.value
                                        when {
                                            x > 260f -> triggerSwipe("LIKE")
                                            x < -260f -> triggerSwipe("PASS")
                                            y < -260f -> triggerSwipe("SUPER_SPARK")
                                            else -> {
                                                coroutineScope.launch {
                                                    dragOffsetX.animateTo(
                                                        0f,
                                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                    )
                                                    dragOffsetY.animateTo(
                                                        0f,
                                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                            dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y)
                                        }
                                    }
                                )
                            },
                        onExpandClick = { showBioDetails = !showBioDetails }
                    )

                    // Stamp Overlays based on drag gesture
                    // LIKE Stamp (Green)
                    if (dragOffsetX.value > 60f) {
                        StampOverlay(
                            text = "LIKE",
                            color = LikeGreen,
                            rotation = -14f,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(28.dp)
                                .graphicsLayer {
                                    alpha = ((dragOffsetX.value - 60f) / 140f).coerceIn(0f, 1f)
                                }
                        )
                    }

                    // NOPE Stamp (Red)
                    if (dragOffsetX.value < -60f) {
                        StampOverlay(
                            text = "NOPE",
                            color = NopeRed,
                            rotation = 14f,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(28.dp)
                                .graphicsLayer {
                                    alpha = ((-dragOffsetX.value - 60f) / 140f).coerceIn(0f, 1f)
                                }
                        )
                    }

                    // SUPER SPARK Stamp (Blue/Gold)
                    if (dragOffsetY.value < -80f) {
                        StampOverlay(
                            text = "SUPER SPARK",
                            color = SuperSparkBlue,
                            rotation = 0f,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 60.dp)
                                .graphicsLayer {
                                    alpha = ((-dragOffsetY.value - 80f) / 140f).coerceIn(0f, 1f)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons (Undo, Pass, Super Spark, Like, Report)
                SwipeActionControls(
                    onPass = { triggerSwipe("PASS") },
                    onSuperSpark = { triggerSwipe("SUPER_SPARK") },
                    onLike = { triggerSwipe("LIKE") },
                    onReport = { onReportUser(currentCandidate) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCardView(
    candidate: UserEntity,
    modifier: Modifier = Modifier,
    onExpandClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("student_profile_card_${candidate.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Visual / Avatar Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    CrimsonPrimary.copy(alpha = 0.85f),
                                    CoralAccent.copy(alpha = 0.70f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val isOfficerWithBadge = candidate.isAdmin && candidate.showBadges

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        StudentAvatar(
                            name = candidate.name,
                            emoji = candidate.avatarEmoji,
                            colorIndex = candidate.avatarColorIndex,
                            photoUri = candidate.photoUri,
                            size = 110.dp,
                            isVerified = candidate.isVerified,
                            isAdmin = candidate.isAdmin,
                            showOfficerBadge = isOfficerWithBadge
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Verified Rajagiri Student Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOfficerWithBadge) Icons.Default.Shield else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isOfficerWithBadge) AmberGold else CampusVerifiedGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOfficerWithBadge) "RSETSparks Moderator" else "Rajagiri Verified Student",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOfficerWithBadge) AmberGold else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // Student Details Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    val isOfficerWithBadge = candidate.isAdmin && candidate.showBadges

                    // Name & Age Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = candidate.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isOfficerWithBadge) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AmberGold.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold)
                                ) {
                                    Text(
                                        text = "MODERATOR",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${candidate.age}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onExpandClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "View Bio Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Academic Program / Year
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${candidate.department} • Class of '${candidate.year.toString().takeLast(2)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Campus Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CoralAccent,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = candidate.campusZone,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    candidate.identityLine().takeIf { it.isNotEmpty() }?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = it, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    candidate.lookingForLabel()?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Looking for: $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bio
                    Text(
                        text = candidate.bio,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Interests Tag Chips
                    Text(
                        text = "Campus Interests",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        candidate.getInterestsList().forEach { interest ->
                            InterestChip(label = interest)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Verified-student badge (emails are never shown to other students)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CampusVerifiedGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verified @rajagiri.edu.in student",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun InterestChip(label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CrimsonPrimary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun StampOverlay(
    text: String,
    color: Color,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .rotate(rotation)
            .border(3.5.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun SwipeActionControls(
    onPass: () -> Unit,
    onSuperSpark: () -> Unit,
    onLike: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Report / Shield Button
        IconButton(
            onClick = onReport,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("report_user_button")
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Report Account",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // Pass (NOPE) Button
        FilledIconButton(
            onClick = onPass,
            modifier = Modifier
                .size(60.dp)
                .testTag("swipe_pass_button"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NopeRed
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Pass",
                modifier = Modifier.size(30.dp)
            )
        }

        // Super Spark Button
        FilledIconButton(
            onClick = onSuperSpark,
            modifier = Modifier
                .size(52.dp)
                .testTag("swipe_super_spark_button"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SuperSparkBlue
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Super Spark",
                modifier = Modifier.size(26.dp)
            )
        }

        // Like Button (Heart)
        FilledIconButton(
            onClick = onLike,
            modifier = Modifier
                .size(66.dp)
                .testTag("swipe_like_button"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Brush.linearGradient(listOf(CrimsonPrimary, CoralAccent)).let { Color.Transparent } ?: CrimsonPrimary,
                contentColor = Color.White
            ),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(CrimsonPrimary, CoralAccent)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like Spark",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyDeckView(
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(CrimsonPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You're All Caught Up!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Only students who create a verified @rajagiri.edu.in account appear as sparks here. Once a peer signs up, their profile appears in your deck!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("reset_deck_button")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check for New Campus Sparks")
        }
    }
}
