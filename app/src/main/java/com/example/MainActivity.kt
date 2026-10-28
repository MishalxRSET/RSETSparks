package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import com.example.data.AppDatabase
import com.example.security.FirebaseAuthHelper
import com.example.security.FirebaseAuthService
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.Crossfade
import com.example.ui.components.CampusTopHeader
import com.example.ui.components.RSETSparksBottomNav
import com.example.ui.components.RSETSparksLoadingOverlay
import com.example.ui.components.RSETSparksSplashScreen
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DiscoveryScreen
import com.example.ui.screens.MatchDialog
import com.example.ui.screens.MatchesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportDialog
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RSETSparksViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RSETSparksApp()
            }
        }
    }
}

@Composable
fun RSETSparksApp(viewModel: RSETSparksViewModel = viewModel()) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val discoveryCandidates by viewModel.discoveryCandidates.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val chatMessages by viewModel.activeConversationMessages.collectAsStateWithLifecycle()
    val newMatchUser by viewModel.newMutualMatchPopup.collectAsStateWithLifecycle()
    val reportingTargetUser by viewModel.reportingTargetUser.collectAsStateWithLifecycle()
    val reportsList by viewModel.reportsList.collectAsStateWithLifecycle()
    val adminUsersList by viewModel.adminUsersList.collectAsStateWithLifecycle()
    val auditLogsList by viewModel.auditLogsList.collectAsStateWithLifecycle()
    val pendingReportsCount by viewModel.pendingReportsCount.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isAppInitializing by viewModel.isAppInitializing.collectAsStateWithLifecycle()
    val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
    val loadingStatusMessage by viewModel.loadingStatusMessage.collectAsStateWithLifecycle()
    val awaitingVerificationEmail by viewModel.awaitingVerificationEmail.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Secondary screen back handler
    BackHandler(enabled = !isAppInitializing && currentScreen !is Screen.DISCOVERY && currentScreen !is Screen.AUTH) {
        when (currentScreen) {
            is Screen.CHAT -> viewModel.navigateTo(Screen.MATCHES)
            else -> viewModel.navigateTo(Screen.DISCOVERY)
        }
    }

    Crossfade(
        targetState = isAppInitializing,
        label = "splash_crossfade"
    ) { initializing ->
        if (initializing) {
            RSETSparksSplashScreen(
                statusText = loadingStatusMessage ?: "Connecting Rajagiri Campus..."
            )
        } else {
            RSETSparksLoadingOverlay(
                isLoading = isActionLoading,
                statusText = loadingStatusMessage ?: "Please wait..."
            ) {
                Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentScreen !is Screen.AUTH && currentScreen !is Screen.CHAT) {
                CampusTopHeader(
                    currentUser = currentUser,
                    pendingReportsCount = pendingReportsCount,
                    onAdminClick = { viewModel.navigateTo(Screen.ADMIN) },
                    onProfileClick = { viewModel.navigateTo(Screen.PROFILE) }
                )
            }
        },
        bottomBar = {
            if (currentScreen !is Screen.AUTH && currentScreen !is Screen.CHAT) {
                RSETSparksBottomNav(
                    currentScreen = currentScreen,
                    pendingReportsCount = pendingReportsCount,
                    isAdminUser = currentUser?.isAdmin == true,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    is Screen.AUTH -> {
                        AuthScreen(
                            onLogin = { email, cb -> viewModel.login(email, cb) },
                            onRegister = { email, name, age, dept, year, bio, interests, emoji, cb ->
                                viewModel.register(email, name, age, dept, year, bio, interests, emoji, cb)
                            },
                            onRegisterProfile = { profile, cb ->
                                viewModel.registerProfile(profile, cb)
                            },
                            awaitingVerificationEmail = awaitingVerificationEmail,
                            onCheckEmailVerificationLive = { onComplete ->
                                viewModel.checkEmailVerificationLive {
                                    onComplete(true)
                                }
                            },
                            onResendVerificationLink = { cb ->
                                viewModel.resendVerificationEmail(cb)
                            },
                            onClearAwaitingVerification = {
                                viewModel.setAwaitingVerificationEmail(null)
                            },
                            onGoogleSignIn = {
                                coroutineScope.launch {
                                    val db = AppDatabase.getDatabase(context)
                                    val authService = FirebaseAuthService.getInstance(db.userProfileDao(), db.userDao())
                                    val result = authService.signInWithGoogle(context, credentialManager)
                                    if (result.isSuccess) {
                                        val firebaseUser = result.getOrNull()
                                        val verifiedEmail = firebaseUser?.email ?: ""
                                        viewModel.loginWithGoogle(verifiedEmail, firebaseUser?.displayName) { success, err ->
                                            if (!success) {
                                                Toast.makeText(context, err ?: "Authentication failed", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Google Sign-In failed"
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                    is Screen.DISCOVERY -> {
                        DiscoveryScreen(
                            candidates = discoveryCandidates,
                            onSwipe = { target, action -> viewModel.swipeCard(target, action) },
                            onReportUser = { target -> viewModel.openReportDialog(target) },
                            onResetDeck = {
                                viewModel.resetDiscoveryDeck()
                            }
                        )
                    }
                    is Screen.MATCHES -> {
                        MatchesScreen(
                            matches = matches,
                            onSelectPartner = { partner -> viewModel.navigateTo(Screen.CHAT(partner)) },
                            onExploreSparksClick = { viewModel.navigateTo(Screen.DISCOVERY) }
                        )
                    }
                    is Screen.CHAT -> {
                        if (currentUser != null) {
                            ChatScreen(
                                currentUser = currentUser!!,
                                partner = screen.partner,
                                messages = chatMessages,
                                onSendMessage = { text -> viewModel.sendMessage(text) },
                                onReportUser = { target -> viewModel.openReportDialog(target) },
                                onBack = { viewModel.navigateTo(Screen.MATCHES) }
                            )
                        }
                    }
                    is Screen.ADMIN -> {
                        if (currentUser != null && currentUser!!.isAdmin) {
                            AdminConsoleScreen(
                                currentAdmin = currentUser!!,
                                reports = reportsList,
                                allUsers = adminUsersList,
                                auditLogs = auditLogsList,
                                onResolveReport = { id, status, note, suspend ->
                                    viewModel.adminResolveReport(id, status, note, suspend)
                                },
                                onToggleSuspension = { userId, suspend, reason ->
                                    viewModel.adminToggleSuspension(userId, suspend, reason)
                                },
                                onToggleVerification = { userId, verified ->
                                    viewModel.adminToggleVerification(userId, verified)
                                },
                                onEmergencyAuditUser = { userId, justification, cb ->
                                    viewModel.adminEmergencyAuditUser(userId, justification, cb)
                                },
                                onToggleAdminBadges = { showBadges ->
                                    viewModel.toggleAdminShowBadges(showBadges)
                                }
                            )
                        } else {
                            viewModel.navigateTo(Screen.DISCOVERY)
                        }
                    }
                    is Screen.PROFILE -> {
                        if (currentUser != null) {
                            ProfileScreen(
                                currentUser = currentUser!!,
                                onAdminClick = { viewModel.navigateTo(Screen.ADMIN) },
                                onLogout = {
                                    FirebaseAuthHelper.signOut(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = { viewModel.logout() },
                                        scope = coroutineScope
                                    )
                                },
                                onToggleSparksVisibility = { isVisible ->
                                    viewModel.toggleSparksVisibility(isVisible)
                                },
                                onToggleAdminBadges = { showBadges ->
                                    viewModel.toggleAdminShowBadges(showBadges)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
}

    // Mutual Match Celebration Dialog
    if (newMatchUser != null && currentUser != null) {
        MatchDialog(
            currentUser = currentUser!!,
            matchedUser = newMatchUser!!,
            onSendMessageClick = {
                val partner = newMatchUser!!
                viewModel.dismissMatchPopup()
                viewModel.navigateTo(Screen.CHAT(partner))
            },
            onKeepSwipingClick = {
                viewModel.dismissMatchPopup()
            }
        )
    }

    // Incident Reporting Dialog
    if (reportingTargetUser != null) {
        ReportDialog(
            targetUser = reportingTargetUser!!,
            onSubmitReport = { cat, desc ->
                viewModel.submitReport(cat, desc)
            },
            onDismiss = {
                viewModel.dismissReportDialog()
            }
        )
    }
}
