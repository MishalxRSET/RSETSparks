package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.security.AuthService
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.DeepCrimson
import kotlinx.coroutines.delay

/**
 * Sign-in / sign-up for verified Rajagiri students, plus the "check your inbox" state shown until
 * the student clicks the email verification link.
 */
@Composable
fun AuthScreen(
    awaitingVerificationEmail: String?,
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: (email: String, password: String, onError: (String) -> Unit) -> Unit,
    onEmailSignUp: (email: String, password: String, onError: (String) -> Unit) -> Unit,
    onForgotPassword: (email: String, onResult: (String) -> Unit) -> Unit,
    onCheckVerification: () -> Unit,
    onResendVerification: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        BrandHero()

        if (awaitingVerificationEmail != null) {
            VerifyEmailCard(
                email = awaitingVerificationEmail,
                onCheckVerification = onCheckVerification,
                onResendVerification = onResendVerification,
                onSignOut = onSignOut
            )
        } else {
            SignInCard(onGoogleSignIn, onEmailSignIn, onEmailSignUp, onForgotPassword)
        }

        Spacer(modifier = Modifier.height(24.dp))
        LegalFooter()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BrandHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(CrimsonPrimary, DeepCrimson)))
            .padding(top = 40.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(CircleShape)
                    .background(CreamIvory),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_rset_sparks_logo),
                    contentDescription = "RSETSparks crest",
                    modifier = Modifier.size(124.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RSET", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 30.sp, color = CreamIvory)
                Text("Sparks", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = CoralAccent)
            }
            Text(
                text = "LEARN • CONNECT • EXCEL",
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                color = CreamIvory.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Meet verified Rajagiri students",
                fontSize = 13.sp,
                color = CreamIvory.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SignInCard(
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: (String, String, (String) -> Unit) -> Unit,
    onEmailSignUp: (String, String, (String) -> Unit) -> Unit,
    onForgotPassword: (String, (String) -> Unit) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Sign in, 1: Create account
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var ageConsent by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val emailValid = AuthService.isRajagiriEmail(email)
    val creating = selectedTab == 1

    Column(modifier = Modifier.padding(20.dp)) {
        // Google Workspace sign-in is the fastest path for Rajagiri accounts.
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("google_signin_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
        ) {
            Text("G", fontWeight = FontWeight.Black, fontSize = 18.sp, color = CreamIvory)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Continue with Rajagiri Google account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "  OR USE EMAIL  ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = !creating, onClick = { selectedTab = 0; error = null }, text = { Text("Sign In", fontWeight = FontWeight.Bold) })
            Tab(
                selected = creating,
                onClick = { selectedTab = 1; error = null },
                text = { Text("Create Account", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("create_account_tab")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim(); error = null; info = null },
            label = { Text("Rajagiri email") },
            placeholder = { Text("you@rajagiri.edu.in") },
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
            isError = email.isNotBlank() && !emailValid,
            supportingText = if (email.isNotBlank() && !emailValid) {
                { Text("Must end with @rajagiri.edu.in") }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        val visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            supportingText = if (creating) {
                { Text("At least ${AuthService.MIN_PASSWORD_LENGTH} characters") }
            } else null,
            visualTransformation = visual,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        if (creating) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("Confirm password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = visual,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_password_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = ageConsent,
                    onCheckedChange = { ageConsent = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("age_consent_checkbox")
                )
                Text(
                    "I'm 18 or older and agree to the Terms of Use and Privacy Policy.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            TextButton(
                onClick = {
                    if (!emailValid) error = "Enter your @rajagiri.edu.in email first."
                    else onForgotPassword(email) { info = it }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("forgot_password_button")
            ) { Text("Forgot password?", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
        }
        info?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                error = when {
                    !emailValid -> "Use your @rajagiri.edu.in email address."
                    password.isBlank() -> "Enter your password."
                    creating && password.length < AuthService.MIN_PASSWORD_LENGTH ->
                        "Password must be at least ${AuthService.MIN_PASSWORD_LENGTH} characters."
                    creating && password != confirmPassword -> "Passwords don't match."
                    creating && !ageConsent -> "Please confirm you're 18+ and accept the Terms."
                    else -> null
                }
                if (error == null) {
                    if (creating) onEmailSignUp(email, password) { error = it }
                    else onEmailSignIn(email, password) { error = it }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_submit_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
        ) {
            Text(if (creating) "Create Account" else "Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun VerifyEmailCard(
    email: String,
    onCheckVerification: () -> Unit,
    onResendVerification: () -> Unit,
    onSignOut: () -> Unit
) {
    var cooldown by remember { mutableIntStateOf(30) }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown--
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CrimsonPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Verify your email", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We sent a verification link to",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(email, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Waiting for you to tap the link… (check spam too)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onCheckVerification,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("check_verification_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) { Text("I've verified my email", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onResendVerification(); cooldown = 60 },
                enabled = cooldown == 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("resend_verification_button"),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (cooldown > 0) "Resend link in ${cooldown}s" else "Resend verification link") }
            TextButton(onClick = onSignOut, modifier = Modifier.testTag("back_to_signin_button")) {
                Text("Use a different email", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun LegalFooter() {
    val context = LocalContext.current
    fun open(resId: Int) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(resId)))) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = { open(R.string.terms_url) }) { Text("Terms of Use", fontSize = 12.sp) }
        TextButton(onClick = { open(R.string.privacy_policy_url) }) { Text("Privacy Policy", fontSize = 12.sp) }
    }
    Text(
        "RSETSparks is a student-run app for Rajagiri students aged 18+.",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shown only in builds made without app/google-services.json. */
@Composable
fun FirebaseSetupScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.ic_rset_sparks_logo), contentDescription = null, modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text("Firebase isn't configured", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This build was made without app/google-services.json. Add it from the Firebase console and rebuild. See guides/FIREBASE_SETUP.md.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
