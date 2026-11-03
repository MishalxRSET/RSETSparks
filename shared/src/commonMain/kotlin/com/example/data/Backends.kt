package com.example.data

import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Contracts between the shared UI/ViewModel and each platform's Firebase SDK.
 * Android implements them in :app (official Firebase Android SDK); iOS in iosMain (GitLive wrapper over the
 * Firebase iOS SDK). Access control is enforced server-side by firestore.rules / storage.rules either way.
 */

/** The signed-in Firebase user as both platforms report it. */
data class AuthUser(
    val uid: String,
    val email: String,
    val isEmailVerified: Boolean,
    val displayName: String,
    val lastSignInMillis: Long?
)

/** Tokens from the platform's Google account picker (Credential Manager on Android, GoogleSignIn on iOS). */
data class GoogleTokens(val idToken: String, val accessToken: String? = null)

/** Thrown by a Google sign-in UI when the student closes the picker; never shown as an error. */
class SignInCancelled : Exception("Sign-in cancelled")

object CampusEmail {
    const val DOMAIN = "@rajagiri.edu.in"
    const val MIN_PASSWORD_LENGTH = 8

    fun isRajagiri(email: String?): Boolean {
        val clean = email?.trim()?.lowercase() ?: return false
        return clean.endsWith(DOMAIN) && clean.length > DOMAIN.length
    }
}

interface AuthBackend {
    val currentUser: AuthUser?

    /** Emits the signed-in user (or null) whenever the auth state changes. */
    fun authState(): Flow<AuthUser?>

    suspend fun signInWithGoogle(tokens: GoogleTokens): Result<AuthUser>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>

    /** Reloads the user and refreshes the ID token so security rules see email_verified. */
    suspend fun reloadAndCheckVerified(): Boolean

    suspend fun signOut()

    /** Deletes the Auth account. Firebase only allows this shortly after signing in. */
    suspend fun deleteCurrentUser(): Result<Unit>
}

/** All Firestore / Storage access. See firestore.rules for what each call is allowed to do. */
interface AppBackend {
    suspend fun isAdmin(uid: String): Boolean
    fun observeOwnProfile(uid: String, email: String, isAdmin: Boolean): Flow<UserEntity?>
    suspend fun createProfile(uid: String, email: String, profile: UserProfile, isAdmin: Boolean): Result<Unit>
    suspend fun updateProfile(uid: String, profile: UserProfile): Result<Unit>
    suspend fun getUser(id: String): UserEntity?
    suspend fun updateVisibility(uid: String, visible: Boolean)
    suspend fun updateShowBadges(uid: String, showBadges: Boolean)

    suspend fun registerPushToken(uid: String, email: String, token: String)
    suspend fun unregisterPushToken(uid: String, email: String, token: String)
    fun observePushEnabled(uid: String): Flow<Boolean>
    suspend fun setPushEnabled(uid: String, email: String, enabled: Boolean)

    fun discoveryCandidates(uid: String): Flow<List<UserEntity>>
    suspend fun recordSwipe(uid: String, targetId: String, action: String): Boolean
    suspend fun resetPasses(uid: String)

    fun matchedUsers(uid: String): Flow<List<UserEntity>>
    fun messages(uid: String, partnerId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(uid: String, partnerId: String, text: String)
    suspend fun unmatch(uid: String, partnerId: String)

    suspend fun blockUser(uid: String, targetId: String)
    suspend fun reportUser(reporter: UserEntity, target: UserEntity, reason: String, description: String): Result<Unit>
    suspend fun deleteAllUserData(uid: String)

    fun allReports(): Flow<List<ReportEntity>>
    fun allUsersWithEmails(): Flow<List<UserEntity>>
    fun auditTrail(): Flow<List<AuditLogEntity>>
    suspend fun adminResolveReport(admin: UserEntity, report: ReportEntity, status: String, note: String, suspend: Boolean)
    suspend fun adminToggleSuspension(admin: UserEntity, target: UserEntity, suspended: Boolean, reason: String)
    suspend fun adminToggleVerification(admin: UserEntity, target: UserEntity, verified: Boolean)
    suspend fun adminEmergencyAudit(admin: UserEntity, target: UserEntity, justification: String): List<String>
}

/** Attaches / detaches this device's push token for the signed-in student. */
interface PushRegistrar {
    suspend fun register()
    suspend fun unregister(uid: String, email: String)

    object None : PushRegistrar {
        override suspend fun register() = Unit
        override suspend fun unregister(uid: String, email: String) = Unit
    }
}

/** Build-time facts the shared code needs. */
data class AppConfig(
    /** Firebase project is on the Blaze plan (photos via Cloud Storage, pushes via Cloud Functions). */
    val isBlaze: Boolean,
    val versionName: String
)
