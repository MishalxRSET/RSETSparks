package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.data.repository.AppRepository
import com.example.security.CryptoHelper
import com.example.security.FirebaseAuthService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object AUTH : Screen()
    object DISCOVERY : Screen()
    object MATCHES : Screen()
    data class CHAT(val partner: UserEntity) : Screen()
    object ADMIN : Screen()
    object PROFILE : Screen()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RSETSparksViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AppRepository(db)

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    private val _currentScreen = MutableStateFlow<Screen>(Screen.DISCOVERY)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _newMutualMatchPopup = MutableStateFlow<UserEntity?>(null)
    val newMutualMatchPopup: StateFlow<UserEntity?> = _newMutualMatchPopup.asStateFlow()

    private val _reportingTargetUser = MutableStateFlow<UserEntity?>(null)
    val reportingTargetUser: StateFlow<UserEntity?> = _reportingTargetUser.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Discovery Deck Flow
    val discoveryCandidates: StateFlow<List<UserEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getDiscoveryCandidatesFlow(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Matches List Flow
    val matches: StateFlow<List<UserEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getMutualMatchesUsersFlow(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Messages Flow
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationMessages: StateFlow<List<ChatMessageEntity>> = _activeConversationId.flatMapLatest { convId ->
        if (convId != null) {
            repository.getConversationMessagesFlow(convId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Flows
    val reportsList: StateFlow<List<ReportEntity>> = repository.getAllReportsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminUsersList: StateFlow<List<UserEntity>> = repository.getAllUsersForAdminFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogsList: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReportsCount: StateFlow<Int> = repository.getPendingReportsCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isAppInitializing = MutableStateFlow(true)
    val isAppInitializing: StateFlow<Boolean> = _isAppInitializing.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _loadingStatusMessage = MutableStateFlow<String?>("Connecting Rajagiri Campus...")
    val loadingStatusMessage: StateFlow<String?> = _loadingStatusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            repository.initialize()
            if (currentUser.value == null) {
                _currentScreen.value = Screen.AUTH
            }
            // Ensure brand loading animation displays smoothly for at least 1200ms
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1200) {
                delay(1200 - elapsed)
            }
            _isAppInitializing.value = false
        }
    }

    private val _awaitingVerificationEmail = MutableStateFlow<String?>(null)
    val awaitingVerificationEmail: StateFlow<String?> = _awaitingVerificationEmail.asStateFlow()

    fun setAwaitingVerificationEmail(email: String?) {
        _awaitingVerificationEmail.value = email
    }

    fun checkEmailVerificationLive(onVerified: () -> Unit) {
        viewModelScope.launch {
            val authService = FirebaseAuthService.getInstance(db.userProfileDao(), db.userDao())
            val result = authService.reloadUserAndCheckVerification()
            if (result.isSuccess && result.getOrDefault(false)) {
                val email = _awaitingVerificationEmail.value
                _awaitingVerificationEmail.value = null
                _toastMessage.value = "Rajagiri campus email successfully verified!"
                if (email != null) {
                    val user = repository.currentUser.value
                    if (user?.isAdmin == true) {
                        _currentScreen.value = Screen.ADMIN
                    } else {
                        _currentScreen.value = Screen.DISCOVERY
                    }
                }
                onVerified()
            }
        }
    }

    fun resendVerificationEmail(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val authService = FirebaseAuthService.getInstance(db.userProfileDao(), db.userDao())
            val result = authService.sendEmailVerification()
            if (result.isSuccess) {
                _toastMessage.value = "Fresh verification link sent to your campus inbox."
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Could not resend verification email")
            }
        }
    }

    fun toggleAdminShowBadges(showBadges: Boolean) {
        val user = currentUser.value ?: return
        if (!user.isAdmin) return
        viewModelScope.launch {
            repository.updateAdminBadgeVisibility(user.id, showBadges)
            _toastMessage.value = if (showBadges) {
                "Officer badge & title are now visible on your profile"
            } else {
                "Officer badge & title hidden. Profile displays as standard student."
            }
        }
    }

    fun navigateTo(screen: Screen) {
        val prevConvId = _activeConversationId.value
        if (screen is Screen.ADMIN && currentUser.value?.isAdmin != true) {
            _currentScreen.value = Screen.DISCOVERY
            return
        }
        if (screen is Screen.CHAT) {
            val user = currentUser.value
            if (user != null) {
                val convId = CryptoHelper.buildConversationId(user.id, screen.partner.id)
                _activeConversationId.value = convId
                repository.attachRealtimeChatListener(convId, viewModelScope)
            }
        } else if (prevConvId != null) {
            repository.detachRealtimeChatListener(prevConvId)
            _activeConversationId.value = null
        }
        _currentScreen.value = screen
    }

    fun login(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loadingStatusMessage.value = "Authenticating with campus credentials..."
            _isActionLoading.value = true
            try {
                val result = repository.loginWithEmail(email)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user?.isAdmin == true) {
                        _currentScreen.value = Screen.ADMIN
                    } else {
                        _currentScreen.value = Screen.DISCOVERY
                    }
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Login failed")
                }
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun register(
        email: String,
        name: String,
        age: Int,
        department: String,
        year: Int,
        bio: String,
        interests: String,
        avatarEmoji: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _loadingStatusMessage.value = "Creating your Rajagiri Spark profile..."
            _isActionLoading.value = true
            try {
                val result = repository.registerStudent(
                    email = email,
                    name = name,
                    age = age,
                    department = department,
                    year = year,
                    bio = bio,
                    interests = interests,
                    avatarEmoji = avatarEmoji
                )
                if (result.isSuccess) {
                    _currentScreen.value = Screen.DISCOVERY
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Registration failed")
                }
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun registerProfile(
        profile: UserProfile,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _loadingStatusMessage.value = "Verifying student enrollment..."
            _isActionLoading.value = true
            try {
                val result = repository.registerStudentWithProfile(profile)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user?.isAdmin == true) {
                        _currentScreen.value = Screen.ADMIN
                    } else {
                        _currentScreen.value = Screen.DISCOVERY
                    }
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Registration failed")
                }
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentScreen.value = Screen.AUTH
    }

    fun loginWithGoogle(email: String, displayName: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _loadingStatusMessage.value = "Verifying Google Campus Account..."
            _isActionLoading.value = true
            try {
                val result = repository.loginOrRegisterGoogleUser(email, displayName)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user?.isAdmin == true) {
                        _currentScreen.value = Screen.ADMIN
                    } else {
                        _currentScreen.value = Screen.DISCOVERY
                    }
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Google Sign-In failed")
                }
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun toggleSparksVisibility(isVisible: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateSparksVisibility(user.id, isVisible)
            _toastMessage.value = if (isVisible) "Your profile is now visible to other campus sparks!" else "Your profile is now hidden from the sparks deck."
        }
    }

    fun swipeCard(targetUser: UserEntity, action: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val isMutual = repository.recordSwipe(user.id, targetUser.id, action)
            if (isMutual) {
                _newMutualMatchPopup.value = targetUser
            }
        }
    }

    fun dismissMatchPopup() {
        _newMutualMatchPopup.value = null
    }

    fun resetDiscoveryDeck() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.resetDiscoveryDeck(user.id)
            _toastMessage.value = "Deck refreshed! Checking for new campus sparks."
        }
    }

    fun sendMessage(plainText: String) {
        val user = currentUser.value ?: return
        val screen = currentScreen.value
        if (screen is Screen.CHAT && plainText.isNotBlank()) {
            viewModelScope.launch {
                repository.sendMessage(user.id, screen.partner.id, plainText.trim())
            }
        }
    }

    fun openReportDialog(targetUser: UserEntity) {
        _reportingTargetUser.value = targetUser
    }

    fun dismissReportDialog() {
        _reportingTargetUser.value = null
    }

    fun submitReport(reasonCategory: String, description: String) {
        val reporter = currentUser.value ?: return
        val target = _reportingTargetUser.value ?: return
        viewModelScope.launch {
            val result = repository.reportAccount(reporter.id, target.id, reasonCategory, description)
            if (result.isSuccess) {
                _toastMessage.value = "Incident reported to Campus Safety. Transcripts secured."
            } else {
                _toastMessage.value = "Could not submit report: ${result.exceptionOrNull()?.message}"
            }
            _reportingTargetUser.value = null
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // Admin Actions
    fun adminResolveReport(reportId: String, status: String, adminNote: String, suspendUser: Boolean) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            repository.adminResolveReport(reportId, status, adminNote, admin.email, suspendUser)
            _toastMessage.value = if (suspendUser) "Account suspended & incident resolved" else "Incident status updated"
        }
    }

    fun adminToggleSuspension(userId: String, isSuspended: Boolean, reason: String) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            repository.adminToggleSuspension(userId, isSuspended, admin.email, reason)
            _toastMessage.value = if (isSuspended) "Student account suspended" else "Student account reinstated"
        }
    }

    fun adminToggleVerification(userId: String, isVerified: Boolean) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            repository.adminToggleVerification(userId, isVerified, admin.email)
            _toastMessage.value = if (isVerified) "Student verified" else "Verification revoked"
        }
    }

    fun adminEmergencyAuditUser(userId: String, justification: String, onResult: (List<String>) -> Unit) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            val transcripts = repository.adminEmergencyAuditTranscripts(userId, admin.email, justification)
            onResult(transcripts)
        }
    }
}
