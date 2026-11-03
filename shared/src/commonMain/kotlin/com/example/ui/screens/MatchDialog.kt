package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserEntity
import com.example.ui.components.ConfettiEffect
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CrimsonPrimary

@Composable
fun MatchDialog(
    currentUser: UserEntity,
    matchedUser: UserEntity,
    onSendMessageClick: () -> Unit,
    onKeepSwipingClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onKeepSwipingClick,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            // Confetti canvas animation in background
            ConfettiEffect()

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = com.example.ui.theme.Slate800,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "It's a Spark!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = CoralAccent,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "You and ${matchedUser.name} both liked each other's campus profile",
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Dual Avatar Spark Bridge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        StudentAvatar(
                            name = currentUser.name,
                            emoji = currentUser.avatarEmoji,
                            colorIndex = currentUser.avatarColorIndex,
                            photoUri = currentUser.photoUri,
                            size = 80.dp,
                            isVerified = currentUser.isVerified,
                            isAdmin = currentUser.isAdmin,
                            showOfficerBadge = currentUser.isAdmin && currentUser.showBadges
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(CrimsonPrimary, CoralAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        StudentAvatar(
                            name = matchedUser.name,
                            emoji = matchedUser.avatarEmoji,
                            colorIndex = matchedUser.avatarColorIndex,
                            photoUri = matchedUser.photoUri,
                            size = 80.dp,
                            isVerified = matchedUser.isVerified,
                            isAdmin = matchedUser.isAdmin,
                            showOfficerBadge = matchedUser.isAdmin && matchedUser.showBadges
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${matchedUser.department} • Class of '${matchedUser.year.toString().takeLast(2)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CoralAccent
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action Buttons
                    Button(
                        onClick = onSendMessageClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("match_send_message_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Say Hello",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onKeepSwipingClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("match_keep_swiping_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Keep Browsing Sparks",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
