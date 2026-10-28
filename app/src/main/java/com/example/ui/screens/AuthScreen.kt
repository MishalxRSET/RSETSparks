package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.CampusVerifiedGreen
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DeepCrimson
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onLogin: (String, (Boolean, String?) -> Unit) -> Unit,
    onRegister: (
        email: String,
        name: String,
        age: Int,
        department: String,
        year: Int,
        bio: String,
        interests: String,
        avatarEmoji: String,
        callback: (Boolean, String?) -> Unit
    ) -> Unit,
    onRegisterProfile: ((UserProfile, (Boolean, String?) -> Unit) -> Unit)? = null,
    onGoogleSignIn: () -> Unit = {},
    awaitingVerificationEmail: String? = null,
    onCheckEmailVerificationLive: ((Boolean) -> Unit) -> Unit = {},
    onResendVerificationLink: ((Boolean, String?) -> Unit) -> Unit = {},
    onClearAwaitingVerification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Register

    // Login state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var localAwaitingEmail by remember { mutableStateOf<String?>(null) }
    var resendCooldownSeconds by remember { mutableIntStateOf(0) }
    var isCheckingVerification by remember { mutableStateOf(false) }

    val activeVerificationEmail = awaitingVerificationEmail ?: localAwaitingEmail

    // Periodic cooldown timer countdown
    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            delay(1000)
            resendCooldownSeconds--
        }
    }

    // Real-time periodic reload check when awaiting verification
    LaunchedEffect(activeVerificationEmail) {
        if (activeVerificationEmail != null) {
            while (true) {
                delay(3000)
                onCheckEmailVerificationLive { verified ->
                    if (verified) {
                        localAwaitingEmail = null
                    }
                }
            }
        }
    }

    // Register state
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regAge by remember { mutableStateOf("21") }
    var regDept by remember { mutableStateOf("B.Tech Computer Science") }
    var regYear by remember { mutableStateOf("2026") }
    var regBio by remember { mutableStateOf("") }
    var regInterests by remember { mutableStateOf("Coding, Music, Filter Coffee") }
    var regEmoji by remember { mutableStateOf("🎓") }
    var regError by remember { mutableStateOf<String?>(null) }

    val departments = listOf(
        "B.Tech Computer Science",
        "B.Tech AI & Data Science",
        "B.Tech Electronics & Comm",
        "B.Tech Mechanical Engg",
        "B.Tech Information Tech",
        "MBA International Business",
        "MCA (Cloud & Cyber Security)"
    )

    val emojiChoices = listOf("🎓", "✨", "🚀", "🌸", "⚡", "🎨", "🎸", "📚", "🏎️")

    val isLoginDomainValid = loginEmail.trim().lowercase().endsWith("@rajagiri.edu.in")
    val isRegDomainValid = regEmail.trim().lowercase().endsWith("@rajagiri.edu.in")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Header with Official Emblem
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CrimsonPrimary,
                            DeepCrimson,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    color = Color.Transparent
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.ic_rset_sparks_logo),
                        contentDescription = "RSET Sparks Official Emblem",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RSET",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Sparks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = CoralAccent
                    )
                }

                Text(
                    text = "Learn • Connect • Excel — Rajagiri Dating & Network",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.92f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CampusVerifiedGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Institutional Access Restricted to .rajagiri.edu.in",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Active Real-Time Email Verification View
        if (activeVerificationEmail != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Verify Your College Email",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "An official verification link has been dispatched to your Rajagiri institutional mailbox:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeVerificationEmail,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Real-time status banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CampusVerifiedGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CampusVerifiedGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = CampusVerifiedGreen
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Listening for verification in real-time... Click the link in your email and access will unlock automatically.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Manual Check Button
                        Button(
                            onClick = {
                                isCheckingVerification = true
                                onCheckEmailVerificationLive { verified ->
                                    isCheckingVerification = false
                                    if (verified) {
                                        localAwaitingEmail = null
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("check_verification_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            enabled = !isCheckingVerification
                        ) {
                            if (isCheckingVerification) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking status...", fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("I've Clicked The Verification Link", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Resend verification link
                        OutlinedButton(
                            onClick = {
                                if (resendCooldownSeconds == 0) {
                                    resendCooldownSeconds = 30
                                    onResendVerificationLink { _, _ -> }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("resend_verification_button"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = resendCooldownSeconds == 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (resendCooldownSeconds > 0) "Resend link in ${resendCooldownSeconds}s" else "Resend Verification Link",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                localAwaitingEmail = null
                                onClearAwaitingVerification()
                            },
                            modifier = Modifier.testTag("back_to_signin_button")
                        ) {
                            Text(
                                text = "Use a different email / Back to Sign In",
                                fontSize = 12.sp,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CrimsonPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Register", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content per Tab
            when (selectedTab) {
                0 -> {
                    // Sign In Form
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Student Sign In",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use your official college institutional email address",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Prominent Google Sign-In with Rajagiri domain restriction
                        Button(
                            onClick = onGoogleSignIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_signin_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google (@rajagiri.edu.in)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "  OR DIRECT RAJAGIRI LOGIN  ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = {
                                loginEmail = it
                                loginError = null
                            },
                            label = { Text("College Email (@rajagiri.edu.in)") },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                            trailingIcon = {
                                if (isLoginDomainValid) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Valid Domain", tint = CampusVerifiedGreen)
                                } else if (loginEmail.isNotBlank()) {
                                    Icon(Icons.Default.Error, contentDescription = "Invalid Domain", tint = Color(0xFFEF4444))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonPrimary
                            )
                        )

                        // Domain Validation warning
                        if (loginEmail.isNotBlank() && !isLoginDomainValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Restricted: Email must end with @rajagiri.edu.in",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        if (loginError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = loginError!!,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (!isLoginDomainValid) {
                                    loginError = "Registration and login strictly require an @rajagiri.edu.in address."
                                    return@Button
                                }
                                onLogin(loginEmail) { success, err ->
                                    if (!success) {
                                        if (err?.contains("verify", ignoreCase = true) == true) {
                                            localAwaitingEmail = loginEmail
                                            onResendVerificationLink { _, _ -> }
                                        } else {
                                            loginError = err
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Text(
                                text = "Continue with Rajagiri ID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                1 -> {
                    RegistrationScreen(
                        initialEmail = loginEmail,
                        onRegisterComplete = { profile ->
                            if (onRegisterProfile != null) {
                                onRegisterProfile(profile) { _, _ -> }
                            } else {
                                onRegister(
                                    profile.email,
                                    profile.name,
                                    profile.age,
                                    profile.department,
                                    profile.year,
                                    profile.bio,
                                    profile.interests.joinToString(", "),
                                    "🎓"
                                ) { _, _ -> }
                            }
                        },
                        onGoogleSignInRequest = onGoogleSignIn
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
