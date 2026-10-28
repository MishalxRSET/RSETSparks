package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.data.repository.AppRepository
import com.example.security.AuthService
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.example.notifications.PushTokens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen {
    object AUTH : Screen()
    object DISCOVERY : Screen()
    object MATCHES : Screen()
    data class CHAT(val partner: UserEntity) : Screen()
    object ADMIN : Screen()
    object PROFILE : Screen()
    object EDIT_PROFILE : Screen()
}

/** Where the signed-in student is in the sign-up funnel. */
sealed interface SessionState {
    object Loading : SessionState
    object FirebaseNotConfigured : SessionState
    object SignedOut : SessionState
    data class AwaitingEmailVerification(val email: String) : SessionState
    data class NeedsProfile(val email: String, val suggestedName: String) : SessionState
    data class Ready(val user: UserEntity) : SessionState
}

@OptIn(ExperimentalCoroutinesApi::class)
class RSETSparksViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseReady = FirebaseApp.getApps(application).isNotEmpty()
    private val auth: AuthService? = if (firebaseReady) AuthService() else null
    private val repository: AppRepository? = if (firebaseReady) AppRepository() else null

    private val _session = MutableStateFlow<SessionState>(
        if (firebaseReady) SessionState.Loading else SessionState.FirebaseNotConfigured
    )
    val session: StateFlow<SessionState> = _session.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _session
        .map { (it as? SessionState.Ready)?.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.DISCOVERY)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _newMutualMatchPopup = MutableStateFlow<UserEntity?>(null)
    val newMutualMatchPopup: StateFlow<UserEntity?> = _newMutualMatchPopup.asStateFlow()

    private val _reportingTargetUser = MutableStateFlow<UserEntity?>(null)
    val reportingTargetUser: StateFlow<UserEntity?> = _reportingTargetUser.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _loadingStatusMessage = MutableStateFlow<String?>(null)
    val loadingStatusMessage: StateFlow<String?> = _loadingStatusMessage.asStateFlow()

    // ------------------------------------------------------------------ data streams

    private fun <T> forUser(default: T, block: (UserEntity, AppRepository) -> Flow<T>): StateFlow<T> =
        currentUser.map { it?.id }
            .distinctUntilChanged() // Profile edits shouldn't restart the listeners
            .flatMapLatest { id ->
                val user = currentUser.value
                if (id == null || user == null || repository == null) flowOf(default)
                else block(user, repository).catch { emit(default) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)

    val discoveryCandidates: StateFlow<List<UserEntity>> = forUser(emptyList()) { user, repo -> repo.discoveryCandidates(user.id) }

    val matches: StateFlow<List<UserEntity>> = forUser(emptyList()) { user, repo -> repo.matchedUsers(user.id) }

    val activeConversationMessages: StateFlow<List<ChatMessage>> =
        combine(currentUser, _currentScreen) { user, screen -> user?.id to (screen as? Screen.CHAT)?.partner?.id }
            .distinctUntilChanged()
            .flatMapLatest { (uid, partnerId) ->
                if (uid == null || partnerId == null || repository == null) flowOf(emptyList())
                else repository.messages(uid, partnerId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun <T> forAdmin(default: T, block: (AppRepository) -> Flow<T>): StateFlow<T> =
        currentUser.map { it?.isAdmin == true }
            .distinctUntilChanged()
            .flatMapLatest { admin -> if (admin && repository != null) block(repository).catch { emit(default) } else flowOf(default) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)

    val adminUsersList: StateFlow<List<UserEntity>> = forAdmin(emptyList()) { it.allUsersWithEmails() }

    val reportsList: StateFlow<List<ReportEntity>> =
        combine(forAdmin(emptyList()) { it.allReports() }, adminUsersList) { reports, people ->
            val emails = people.associate { it.id to it.email }
            reports.map { it.copy(reportedEmail = emails[it.reportedUserId] ?: it.reportedEmail) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogsList: StateFlow<List<AuditLogEntity>> = forAdmin(emptyList()) { it.auditTrail() }

    val pendingReportsCount: StateFlow<Int> = reportsList
        .map { list -> list.count { it.status == "PENDING" || it.status == "INVESTIGATING" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ------------------------------------------------------------------ session

    private var profileJob: Job? = null
    private var verificationJob: Job? = null

    init {
        if (auth != null) {
            viewModelScope.launch {
                auth.authState().collect { onAuthChanged(it) }
            }
        }
    }

    private fun onAuthChanged(firebaseUser: FirebaseUser?) {
        profileJob?.cancel()
        verificationJob?.cancel()
        val repo = repository ?: return
        when {
            firebaseUser == null -> {
                _session.value = SessionState.SignedOut
                _currentScreen.value = Screen.AUTH
            }
            !AuthService.isRajagiriEmail(firebaseUser.email) -> {
                viewModelScope.launch { auth?.signOut(null) }
            }
            !firebaseUser.isEmailVerified -> {
                _session.value = SessionState.AwaitingEmailVerification(firebaseUser.email.orEmpty())
                _currentScreen.value = Screen.AUTH
                verificationJob = viewModelScope.launch { pollEmailVerification() }
            }
            else -> {
                val email = firebaseUser.email.orEmpty()
                profileJob = viewModelScope.launch {
                    val isAdmin = repo.isAdmin(firebaseUser.uid)
                    repo.observeOwnProfile(firebaseUser.uid, email, isAdmin)
                        .catch {
                            _toastMessage.value = "Couldn't load your profile. Check your connection and sign in again."
                            auth?.signOut(null)
                        }
                        .collect { profile ->
                            val wasReady = _session.value is SessionState.Ready
                            _session.value = when {
                                profile == null -> SessionState.NeedsProfile(email, firebaseUser.displayName.orEmpty())
                                else -> SessionState.Ready(profile)
                            }
                            if (profile != null && !wasReady) {
                                _currentScreen.value = Screen.DISCOVERY
                                registerPushToken()
                                pendingChatPartnerId?.let { openChatWith(it) }
                            }
                            if (profile?.isSuspended == true) {
                                _toastMessage.value = "Your account has been suspended by campus moderators."
                            }
                        }
                }
            }
        }
    }

    /** Checks every few seconds whether the student clicked the link in their inbox. */
    private suspend fun pollEmailVerification() {
        while (viewModelScope.isActive) {
            delay(4000)
            if (auth?.reloadAndCheckVerified() == true) {
                _toastMessage.value = "Email verified — welcome to RSETSparks!"
                onAuthChanged(auth.currentUser)
                return
            }
        }
    }

    fun checkEmailVerificationNow() {
        viewModelScope.launch {
            if (auth?.reloadAndCheckVerified() == true) onAuthChanged(auth.currentUser)
            else _toastMessage.value = "Not verified yet. Open the link in your Rajagiri inbox, then try again."
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            val result = auth?.sendEmailVerification() ?: return@launch
            _toastMessage.value = if (result.isSuccess) "Verification link sent again. Check your inbox and spam folder."
            else result.exceptionOrNull()?.message
        }
    }

    private fun runAction(status: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _loadingStatusMessage.value = status
            _isActionLoading.value = true
            try {
                block()
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Something went wrong. Please try again."
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        val service = auth ?: return
        runAction("Signing in with Google...") {
            service.signInWithGoogle(context, credentialManager).onFailure { e ->
                if (e !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    _toastMessage.value = e.message ?: "Google Sign-In failed."
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String, onError: (String) -> Unit) {
        val service = auth ?: return
        runAction("Signing in...") {
            service.signInWithEmail(email, password).onFailure { onError(it.message ?: "Sign-in failed.") }
        }
    }

    fun signUpWithEmail(email: String, password: String, onError: (String) -> Unit) {
        val service = auth ?: return
        runAction("Creating your account...") {
            service.signUpWithEmail(email, password).onFailure { onError(it.message ?: "Sign-up failed.") }
        }
    }

    fun sendPasswordReset(email: String, onResult: (String) -> Unit) {
        val service = auth ?: return
        viewModelScope.launch {
            val result = service.sendPasswordReset(email)
            onResult(if (result.isSuccess) "Password reset link sent to $email." else result.exceptionOrNull()?.message ?: "Couldn't send reset link.")
        }
    }

    fun completeProfile(profile: UserProfile, onError: (String) -> Unit) {
        val repo = repository ?: return
        val firebaseUser = auth?.currentUser ?: return
        UserProfile.validate(profile)?.let { onError(it); return }
        runAction("Setting up your profile...") {
            val isAdmin = repo.isAdmin(firebaseUser.uid)
            repo.createProfile(firebaseUser.uid, firebaseUser.email.orEmpty(), profile, isAdmin)
                .onFailure { onError("Couldn't save your profile: ${it.message}") }
        }
    }

    fun signOut(credentialManager: CredentialManager?) {
        viewModelScope.launch {
            unregisterPushToken()
            auth?.signOut(credentialManager)
        }
    }

    fun saveProfileEdits(profile: UserProfile, onError: (String) -> Unit) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        UserProfile.validate(profile)?.let { onError(it); return }
        runAction("Saving your profile...") {
            repo.updateProfile(user.id, profile)
                .onSuccess {
                    _toastMessage.value = "Profile updated."
                    _currentScreen.value = Screen.PROFILE
                }
                .onFailure { onError("Couldn't save your profile: ${it.message}") }
        }
    }

    // ------------------------------------------------------------------ push notifications

    val pushEnabled: StateFlow<Boolean> = forUser(true) { user, repo -> repo.observePushEnabled(user.id) }

    private fun registerPushToken() {
        val repo = repository ?: return
        viewModelScope.launch { runCatching { PushTokens.register(getApplication(), repo) } }
    }

    private suspend fun unregisterPushToken() {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        runCatching { PushTokens.unregister(getApplication(), user.id, user.email, repo) }
    }

    fun setPushEnabled(enabled: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching { repository?.setPushEnabled(user.id, user.email, enabled) }
                .onFailure { _toastMessage.value = "Couldn't update notification settings." }
        }
    }

    // Set when the app is opened from a notification before the session is ready.
    private var pendingChatPartnerId: String? = null

    /** Opens the chat with [partnerId], e.g. after tapping a match or message notification. */
    fun openChatWith(partnerId: String) {
        if (currentUser.value == null) {
            pendingChatPartnerId = partnerId
            return
        }
        pendingChatPartnerId = null
        viewModelScope.launch {
            val partner = repository?.getUser(partnerId)
            if (partner != null) navigateTo(Screen.CHAT(partner)) else navigateTo(Screen.MATCHES)
        }
    }

    fun deleteAccount(credentialManager: CredentialManager?) {
        val user = currentUser.value ?: return
        val repo = repository ?: return
        val service = auth ?: return
        // Check before touching any data so a stale session can't leave a half-deleted account.
        if (!service.hasRecentLogin()) {
            _toastMessage.value = "For your security, sign out and sign back in, then delete your account within a few minutes."
            return
        }
        runAction("Deleting your account and data...") {
            unregisterPushToken()
            repo.deleteAllUserData(user.id)
            service.deleteCurrentUser()
                .onSuccess {
                    service.signOut(credentialManager)
                    _toastMessage.value = "Your account and data have been deleted."
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    // ------------------------------------------------------------------ navigation

    fun navigateTo(screen: Screen) {
        if (screen is Screen.ADMIN && currentUser.value?.isAdmin != true) {
            _currentScreen.value = Screen.DISCOVERY
            return
        }
        _currentScreen.value = screen
    }

    // ------------------------------------------------------------------ student actions

    fun toggleSparksVisibility(isVisible: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching { repository?.updateVisibility(user.id, isVisible) }
                .onSuccess {
                    _toastMessage.value = if (isVisible) "You're visible in the Sparks deck again." else "You're hidden from the Sparks deck."
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun toggleAdminShowBadges(showBadges: Boolean) {
        val user = currentUser.value ?: return
        if (!user.isAdmin) return
        viewModelScope.launch {
            runCatching { repository?.updateShowBadges(user.id, showBadges) }
                .onSuccess {
                    _toastMessage.value = if (showBadges) "Officer badge is now visible on your profile."
                    else "Officer badge hidden."
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun swipeCard(targetUser: UserEntity, action: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching { repository?.recordSwipe(user.id, targetUser.id, action) == true }
                .onSuccess { matched -> if (matched) _newMutualMatchPopup.value = targetUser }
                .onFailure { _toastMessage.value = "Couldn't save that swipe. Check your connection." }
        }
    }

    fun dismissMatchPopup() {
        _newMutualMatchPopup.value = null
    }

    fun resetDiscoveryDeck() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching { repository?.resetPasses(user.id) }
            _toastMessage.value = "Deck refreshed! Students you passed on will appear again."
        }
    }

    fun sendMessage(plainText: String) {
        val user = currentUser.value ?: return
        val screen = currentScreen.value as? Screen.CHAT ?: return
        if (plainText.isBlank()) return
        viewModelScope.launch {
            runCatching { repository?.sendMessage(user.id, screen.partner.id, plainText) }
                .onFailure { _toastMessage.value = "Message not sent. You may no longer be matched." }
        }
    }

    fun unmatch(partner: UserEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching { repository?.unmatch(user.id, partner.id) }
            _toastMessage.value = "You unmatched ${partner.name}."
            _currentScreen.value = Screen.MATCHES
        }
    }

    fun blockUser(target: UserEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            runCatching { repository?.blockUser(user.id, target.id) }
                .onSuccess {
                    _toastMessage.value = "${target.name} is blocked. You won't see each other again."
                    if (_currentScreen.value is Screen.CHAT) _currentScreen.value = Screen.MATCHES
                }
                .onFailure { _toastMessage.value = "Couldn't block: ${it.message}" }
        }
    }

    fun openReportDialog(targetUser: UserEntity) {
        _reportingTargetUser.value = targetUser
    }

    fun dismissReportDialog() {
        _reportingTargetUser.value = null
    }

    fun submitReport(reasonCategory: String, description: String, alsoBlock: Boolean) {
        val reporter = currentUser.value ?: return
        val target = _reportingTargetUser.value ?: return
        _reportingTargetUser.value = null
        viewModelScope.launch {
            val result = repository?.reportUser(reporter, target, reasonCategory, description) ?: return@launch
            _toastMessage.value = if (result.isSuccess) "Report sent to campus moderators. Thank you for keeping RSETSparks safe."
            else "Could not submit report: ${result.exceptionOrNull()?.message}"
            if (alsoBlock) blockUser(target)
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ------------------------------------------------------------------ admin actions

    private fun adminAction(success: String, block: suspend (AppRepository, UserEntity) -> Unit) {
        val admin = currentUser.value?.takeIf { it.isAdmin } ?: return
        val repo = repository ?: return
        viewModelScope.launch {
            runCatching { block(repo, admin) }
                .onSuccess { _toastMessage.value = success }
                .onFailure { _toastMessage.value = "Action failed: ${it.message}" }
        }
    }

    fun adminResolveReport(reportId: String, status: String, adminNote: String, suspendUser: Boolean) {
        val report = reportsList.value.firstOrNull { it.id == reportId } ?: return
        adminAction(if (suspendUser) "Account suspended & incident resolved" else "Incident status updated") { repo, admin ->
            repo.adminResolveReport(admin, report, status, adminNote, suspendUser)
        }
    }

    fun adminToggleSuspension(userId: String, isSuspended: Boolean, reason: String) {
        val target = adminUsersList.value.firstOrNull { it.id == userId } ?: return
        adminAction(if (isSuspended) "Student account suspended" else "Student account reinstated") { repo, admin ->
            repo.adminToggleSuspension(admin, target, isSuspended, reason)
        }
    }

    fun adminToggleVerification(userId: String, isVerified: Boolean) {
        val target = adminUsersList.value.firstOrNull { it.id == userId } ?: return
        adminAction(if (isVerified) "Student verified" else "Verification revoked") { repo, admin ->
            repo.adminToggleVerification(admin, target, isVerified)
        }
    }

    fun adminEmergencyAuditUser(userId: String, justification: String, onResult: (List<String>) -> Unit) {
        val admin = currentUser.value?.takeIf { it.isAdmin } ?: return
        val target = adminUsersList.value.firstOrNull { it.id == userId } ?: return
        viewModelScope.launch {
            runCatching { repository?.adminEmergencyAudit(admin, target, justification).orEmpty() }
                .onSuccess(onResult)
                .onFailure { _toastMessage.value = "Audit failed: ${it.message}" }
        }
    }
}
