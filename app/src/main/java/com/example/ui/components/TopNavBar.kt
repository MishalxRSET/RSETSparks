package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CrimsonPrimary

@Composable
fun CampusTopHeader(
    currentUser: UserEntity?,
    pendingReportsCount: Int = 0,
    onAdminClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Campus Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onProfileClick() }
                    .padding(4.dp)
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_rset_sparks_logo),
                    contentDescription = "RSET Sparks Official Emblem",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RSET",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sparks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = CoralAccent
                        )
                    }
                    Text(
                        text = "Rajagiri Verified Network",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right Actions: Admin Button with Badge, Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Admin Button (Strictly visible to admin only)
                if (currentUser?.isAdmin == true) {
                    BadgedBox(
                        badge = {
                            if (pendingReportsCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ) {
                                    Text("$pendingReportsCount")
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onAdminClick,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("admin_portal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Admin Safety Console",
                                tint = AmberGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (currentUser != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onProfileClick() }
                            .testTag("profile_avatar_button")
                    ) {
                        StudentAvatar(
                            name = currentUser.name,
                            emoji = currentUser.avatarEmoji,
                            colorIndex = currentUser.avatarColorIndex,
                            photoUri = currentUser.photoUri,
                            size = 36.dp,
                            isVerified = currentUser.isVerified,
                            isAdmin = currentUser.isAdmin,
                            showOfficerBadge = currentUser.isAdmin && currentUser.showBadges
                        )
                    }
                }
            }
        }
    }
}
