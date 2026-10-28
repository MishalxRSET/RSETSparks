package com.example.security

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.dao.UserDao
import com.example.data.dao.UserProfileDao
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service managing Firebase Authentication with Google Sign-In and institutional domain enforcement.
 * Strictly verifies that the authenticated Google account email ends with '@rajagiri.edu.in'
 * before allowing registration or profile synchronization.
 */
class FirebaseAuthService(
    private val userProfileDao: UserProfileDao? = null,
    private val userDao: UserDao? = null
) {
    companion object {
        private const val TAG = "FirebaseAuthService"
        const val RAJAGIRI_DOMAIN = "@rajagiri.edu.in"
        const val COLLECTION_USER_PROFILES = "user_profiles"

        @Volatile
        private var instance: FirebaseAuthService? = null

        fun getInstance(userProfileDao: UserProfileDao? = null, userDao: UserDao? = null): FirebaseAuthService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthService(userProfileDao, userDao).also { instance = it }
            }
        }
    }

    private val auth: FirebaseAuth get() = Firebase.auth
    private val firestore: FirebaseFirestore get() = Firebase.firestore

    private val _currentFirebaseUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentFirebaseUser = _currentFirebaseUser.asStateFlow()

    /**
     * Strictly verifies whether an email address belongs to Rajagiri College of Social Sciences /
     * Rajagiri School of Engineering & Technology (@rajagiri.edu.in).
     */
    fun isRajagiriEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.trim().lowercase()
        return clean.endsWith(RAJAGIRI_DOMAIN) && clean.length > RAJAGIRI_DOMAIN.length
    }

    /**
     * Executes Google Sign-In using Credential Manager.
     * Enforces that the returned Google account has a valid '@rajagiri.edu.in' domain.
     * If the domain does not match, immediately signs out and returns a descriptive error.
     */
    suspend fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val activity = context as? Activity
            ?: return@withContext Result.failure(IllegalStateException("Context must be an Activity to display Google Sign-In"))

        val serverClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w(TAG, "default_web_client_id string not found: ${e.message}")
            return@withContext Result.failure(
                IllegalStateException("Google Sign-In configuration is missing. Ensure google-services.json is configured.")
            )
        }

        try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return@withContext Result.failure(IllegalStateException("Unexpected credential format received"))
            }

            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            val firebaseUser = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Firebase authentication returned no user"))

            val userEmail = firebaseUser.email ?: ""

            // STRICT DOMAIN ENFORCEMENT CHECK:
            if (!isRajagiriEmail(userEmail)) {
                // Instantly revoke session and purge credentials for non-Rajagiri emails
                auth.signOut()
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {}

                Log.w(TAG, "Rejected unauthorized sign-in attempt from non-Rajagiri domain: $userEmail")
                return@withContext Result.failure(
                    SecurityException("Access Restricted: Account ($userEmail) is not a verified Rajagiri student email. Registration requires an institutional @rajagiri.edu.in address.")
                )
            }

            _currentFirebaseUser.value = firebaseUser
            Log.i(TAG, "Successfully authenticated verified student: $userEmail (UID: ${firebaseUser?.uid})")
            return@withContext Result.success(firebaseUser)

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Student cancelled Google Sign-In")
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Synchronizes a UserProfile to Cloud Firestore and caches it in local Room database.
     */
    suspend fun saveAndSyncUserProfile(
        profile: UserProfile
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        val uid = profile.id.ifBlank { user?.uid ?: "" }
        if (uid.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Cannot save profile without authenticated Firebase UID"))
        }

        val enrichedProfile = profile.copy(
            id = uid,
            email = if (profile.email.isBlank()) (user?.email ?: "") else profile.email,
            isVerified = true,
            updatedAt = System.currentTimeMillis()
        )

        // 1. Local caching in Room DAO immediately for instant offline-first UX
        userProfileDao?.insertProfile(enrichedProfile.copy(isSyncedWithFirestore = false))
        userDao?.insertUser(enrichedProfile.toUserEntity())

        // 2. Synchronize with Firestore
        try {
            firestore.collection(COLLECTION_USER_PROFILES)
                .document(uid)
                .set(enrichedProfile.toFirestoreMap())
                .await()

            // Mark local cache as synchronized
            userProfileDao?.markSyncStatus(uid, true)
            val syncedProfile = enrichedProfile.copy(isSyncedWithFirestore = true)
            userProfileDao?.updateProfile(syncedProfile)

            Log.i(TAG, "Profile successfully synced to Firestore for $uid")
            Result.success(syncedProfile)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync failed (offline or network error), cached locally in Room: ${e.message}")
            // Return success with local cache so student registration isn't blocked by network glitches
            Result.success(enrichedProfile.copy(isSyncedWithFirestore = false))
        }
    }

    /**
     * Fetches the user profile from local Room cache, or pulls from Firestore if available.
     */
    suspend fun loadUserProfile(uid: String): UserProfile? = withContext(Dispatchers.IO) {
        // Check Room local cache first
        val cached = userProfileDao?.getProfileById(uid)
        if (cached != null) return@withContext cached

        // Fallback to Firestore
        try {
            val doc = firestore.collection(COLLECTION_USER_PROFILES).document(uid).get().await()
            if (doc.exists() && doc.data != null) {
                val profile = UserProfile.fromFirestore(uid, doc.data!!)
                userProfileDao?.insertProfile(profile)
                userDao?.insertUser(profile.toUserEntity())
                return@withContext profile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch profile from Firestore: ${e.message}")
        }
        null
    }

    /**
     * Sync any pending un-synced profiles from Room to Firestore.
     */
    suspend fun syncPendingProfiles(): Int = withContext(Dispatchers.IO) {
        val pending = userProfileDao?.getPendingSyncProfiles() ?: return@withContext 0
        var syncedCount = 0
        for (profile in pending) {
            try {
                firestore.collection(COLLECTION_USER_PROFILES)
                    .document(profile.id)
                    .set(profile.toFirestoreMap())
                    .await()
                userProfileDao.markSyncStatus(profile.id, true)
                syncedCount++
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing pending profile ${profile.id}: ${e.message}")
            }
        }
        syncedCount
    }

    /**
     * Signs out the student from Firebase and clears Credential Manager state.
     */
    suspend fun signOut(context: Context, credentialManager: CredentialManager) = withContext(Dispatchers.IO) {
        auth.signOut()
        _currentFirebaseUser.value = null
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear credential state on sign out: ${e.message}")
        }
    }

    /**
     * Authenticates an institutional student using email and password with Firebase Auth.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!isRajagiriEmail(cleanEmail)) {
            return@withContext Result.failure(
                SecurityException("Access Restricted: Only verified @rajagiri.edu.in accounts are permitted.")
            )
        }
        try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("No Firebase user returned"))
            _currentFirebaseUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase email sign-in failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Registers an institutional student with email and password and sends an official verification link.
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!isRajagiriEmail(cleanEmail)) {
            return@withContext Result.failure(
                SecurityException("Access Restricted: Only verified @rajagiri.edu.in accounts are permitted.")
            )
        }
        try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("No Firebase user created"))
            _currentFirebaseUser.value = user
            // Automatically send official Firebase email verification link
            try {
                user.sendEmailVerification().await()
                Log.i(TAG, "Sent Firebase email verification link to $cleanEmail")
            } catch (e: Exception) {
                Log.w(TAG, "Failed sending initial verification email: ${e.message}")
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase user registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Dispatches official Firebase Authentication verification email to the current authenticated student.
     */
    suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("No signed-in student found to send verification email"))
        try {
            user.sendEmailVerification().await()
            Log.i(TAG, "Dispatched verification email to ${user.email}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch email verification: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Reloads the Firebase user profile from backend servers and checks real-time email verification status.
     */
    suspend fun reloadUserAndCheckVerification(): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
            ?: return@withContext Result.failure(IllegalStateException("No signed-in student account"))
        try {
            user.reload().await()
            val isVerified = auth.currentUser?.isEmailVerified == true
            _currentFirebaseUser.value = auth.currentUser
            Log.d(TAG, "Reloaded student user: ${user.email}, isEmailVerified=$isVerified")
            Result.success(isVerified)
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading student user: ${e.message}")
            Result.failure(e)
        }
    }

    fun isCurrentUserEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }
}
