package com.example.security

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.AuthBackend
import com.example.data.AuthUser
import com.example.data.CampusEmail
import com.example.data.CampusEmail.MIN_PASSWORD_LENGTH
import com.example.data.GoogleTokens
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication (Android SDK) restricted to @rajagiri.edu.in accounts.
 * Supports Google Sign-In (Rajagiri runs on Google Workspace; the account picker is [AndroidGoogleSignIn])
 * and email + password with a verification link.
 */
class AuthService(context: Context, private val auth: FirebaseAuth = Firebase.auth) : AuthBackend {

    private companion object {
        const val TAG = "AuthService"
    }

    private val credentialManager = CredentialManager.create(context.applicationContext)

    override val currentUser: AuthUser? get() = auth.currentUser?.toAuthUser()

    override fun authState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(tokens: GoogleTokens): Result<AuthUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(tokens.idToken, tokens.accessToken)
        val user = auth.signInWithCredential(credential).await().user ?: error("Sign-in returned no user.")
        if (!CampusEmail.isRajagiri(user.email)) {
            signOut()
            throw SecurityException("${user.email} isn't a Rajagiri account. Please use your @rajagiri.edu.in Google account.")
        }
        user.toAuthUser()
    }.onFailure { Log.w(TAG, "Google sign-in failed", it) }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> {
        val clean = email.trim().lowercase()
        if (!CampusEmail.isRajagiri(clean)) return Result.failure(SecurityException("Use your @rajagiri.edu.in email address."))
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters."))
        }
        return runCatching {
            val user = auth.createUserWithEmailAndPassword(clean, password).await().user ?: error("No user created.")
            user.sendEmailVerification().await()
            user.toAuthUser()
        }.mapError()
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        val clean = email.trim().lowercase()
        if (!CampusEmail.isRajagiri(clean)) return Result.failure(SecurityException("Use your @rajagiri.edu.in email address."))
        return runCatching {
            (auth.signInWithEmailAndPassword(clean, password).await().user ?: error("No user returned.")).toAuthUser()
        }.mapError()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        if (!CampusEmail.isRajagiri(clean)) return Result.failure(SecurityException("Use your @rajagiri.edu.in email address."))
        return runCatching { auth.sendPasswordResetEmail(clean).await(); Unit }.mapError()
    }

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        user.sendEmailVerification().await()
        Unit
    }.mapError()

    override suspend fun reloadAndCheckVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return runCatching {
            user.reload().await()
            val verified = auth.currentUser?.isEmailVerified == true
            if (verified) auth.currentUser?.getIdToken(true)?.await()
            verified
        }.getOrDefault(false)
    }

    override suspend fun signOut() {
        auth.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    override suspend fun deleteCurrentUser(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        user.delete().await()
        Unit
    }.recoverCatching { e ->
        if (e is FirebaseAuthRecentLoginRequiredException) throw RecentLoginRequired() else throw e
    }

    class RecentLoginRequired : Exception("For your security, please sign out and sign in again, then retry deleting your account.")

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        isEmailVerified = isEmailVerified,
        displayName = displayName.orEmpty(),
        lastSignInMillis = metadata?.lastSignInTimestamp
    )

    private fun <T> Result<T>.mapError(): Result<T> = recoverCatching { e ->
        throw IllegalStateException(
            when (e) {
                is FirebaseAuthUserCollisionException -> "An account already exists for this email. Try signing in instead."
                is FirebaseAuthWeakPasswordException -> "That password is too weak. Use at least $MIN_PASSWORD_LENGTH characters."
                is FirebaseAuthInvalidUserException,
                is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
                else -> e.message ?: "Something went wrong. Please try again."
            },
            e
        )
    }
}
