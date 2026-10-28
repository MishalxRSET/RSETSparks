package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CampusTopHeader
import com.example.ui.components.RSETSparksBottomNav
import com.example.ui.components.RSETSparksLoadingOverlay
import com.example.ui.components.RSETSparksSplashScreen
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DiscoveryScreen
import com.example.ui.screens.FirebaseSetupScreen
import com.example.ui.screens.MatchDialog
import com.example.ui.screens.MatchesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReportDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RSETSparksViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SessionState

class MainActivity : ComponentActivity() {
    private val viewModel: RSETSparksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the branded system splash up until we know whether the student is signed in.
        splash.setKeepOnScreenCondition { viewModel.session.value is SessionState.Loading }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RSETSparksApp(viewModel)
            }
        }
    }
}

@Composable
fun RSETSparksApp(viewModel: RSETSparksViewModel) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val session by viewModel.session.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isActionLoading by viewModel.isActionLoading.collectAsStateWithLifecycle()
    val loadingStatusMessage by viewModel.loadingStatusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    RSETSparksLoadingOverlay(
        isLoading = isActionLoading,
        statusText = loadingStatusMessage ?: "Please wait..."
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(
                    targetState = session,
                    contentKey = { it::class }, // Profile updates within a state shouldn't re-animate
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "session"
                ) { s ->
                    when (s) {
                        SessionState.Loading -> RSETSparksSplashScreen(statusText = "Connecting to campus...")
                        SessionState.FirebaseNotConfigured -> FirebaseSetupScreen()
                        SessionState.SignedOut, is SessionState.AwaitingEmailVerification -> AuthScreen(
                            awaitingVerificationEmail = (s as? SessionState.AwaitingEmailVerification)?.email,
                            onGoogleSignIn = { viewModel.signInWithGoogle(context, credentialManager) },
                            onEmailSignIn = viewModel::signInWithEmail,
                            onEmailSignUp = viewModel::signUpWithEmail,
                            onForgotPassword = viewModel::sendPasswordReset,
                            onCheckVerification = viewModel::checkEmailVerificationNow,
                            onResendVerification = viewModel::resendVerificationEmail,
                            onSignOut = { viewModel.signOut(credentialManager) }
                        )
                        is SessionState.NeedsProfile -> OnboardingScreen(
                            email = s.email,
                            suggestedName = s.suggestedName,
                            onSubmit = viewModel::completeProfile,
                            onSignOut = { viewModel.signOut(credentialManager) }
                        )
                        is SessionState.Ready -> SignedInApp(viewModel, credentialManager)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedInApp(viewModel: RSETSparksViewModel, credentialManager: CredentialManager) {
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

    val me = currentUser ?: return

    BackHandler(enabled = currentScreen !is Screen.DISCOVERY) {
        when (currentScreen) {
            is Screen.CHAT -> viewModel.navigateTo(Screen.MATCHES)
            else -> viewModel.navigateTo(Screen.DISCOVERY)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen !is Screen.CHAT) {
                CampusTopHeader(
                    currentUser = me,
                    pendingReportsCount = pendingReportsCount,
                    onAdminClick = { viewModel.navigateTo(Screen.ADMIN) },
                    onProfileClick = { viewModel.navigateTo(Screen.PROFILE) }
                )
            }
        },
        bottomBar = {
            if (currentScreen !is Screen.CHAT) {
                RSETSparksBottomNav(
                    currentScreen = currentScreen,
                    pendingReportsCount = pendingReportsCount,
                    isAdminUser = me.isAdmin,
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
                    is Screen.AUTH, is Screen.DISCOVERY -> DiscoveryScreen(
                        candidates = discoveryCandidates,
                        onSwipe = { target, action -> viewModel.swipeCard(target, action) },
                        onReportUser = { target -> viewModel.openReportDialog(target) },
                        onResetDeck = { viewModel.resetDiscoveryDeck() }
                    )
                    is Screen.MATCHES -> MatchesScreen(
                        matches = matches,
                        onSelectPartner = { partner -> viewModel.navigateTo(Screen.CHAT(partner)) },
                        onExploreSparksClick = { viewModel.navigateTo(Screen.DISCOVERY) }
                    )
                    is Screen.CHAT -> ChatScreen(
                        currentUser = me,
                        partner = screen.partner,
                        messages = chatMessages,
                        onSendMessage = { text -> viewModel.sendMessage(text) },
                        onReportUser = { target -> viewModel.openReportDialog(target) },
                        onBlockUser = { target -> viewModel.blockUser(target) },
                        onUnmatch = { target -> viewModel.unmatch(target) },
                        onBack = { viewModel.navigateTo(Screen.MATCHES) }
                    )
                    is Screen.ADMIN -> if (me.isAdmin) {
                        AdminConsoleScreen(
                            currentAdmin = me,
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
                            onToggleAdminBadges = { showBadges -> viewModel.toggleAdminShowBadges(showBadges) }
                        )
                    }
                    is Screen.PROFILE -> ProfileScreen(
                        currentUser = me,
                        onAdminClick = { viewModel.navigateTo(Screen.ADMIN) },
                        onLogout = { viewModel.signOut(credentialManager) },
                        onDeleteAccount = { viewModel.deleteAccount(credentialManager) },
                        onToggleSparksVisibility = { isVisible -> viewModel.toggleSparksVisibility(isVisible) },
                        onToggleAdminBadges = { showBadges -> viewModel.toggleAdminShowBadges(showBadges) }
                    )
                }
            }
        }
    }

    newMatchUser?.let { matched ->
        MatchDialog(
            currentUser = me,
            matchedUser = matched,
            onSendMessageClick = {
                viewModel.dismissMatchPopup()
                viewModel.navigateTo(Screen.CHAT(matched))
            },
            onKeepSwipingClick = { viewModel.dismissMatchPopup() }
        )
    }

    reportingTargetUser?.let { target ->
        ReportDialog(
            targetUser = target,
            onSubmitReport = { cat, desc, alsoBlock -> viewModel.submitReport(cat, desc, alsoBlock) },
            onDismiss = { viewModel.dismissReportDialog() }
        )
    }
}
