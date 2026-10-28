package com.example.security

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
 * Firebase Authentication restricted to @rajagiri.edu.in accounts.
 * Supports Google Sign-In (Rajagiri runs on Google Workspace) and email + password with a
 * verification link.
 */
class AuthService(private val auth: FirebaseAuth = Firebase.auth) {

    companion object {
        private const val TAG = "AuthService"
        const val CAMPUS_DOMAIN = "@rajagiri.edu.in"
        const val MIN_PASSWORD_LENGTH = 8

        fun isRajagiriEmail(email: String?): Boolean {
            val clean = email?.trim()?.lowercase() ?: return false
            return clean.endsWith(CAMPUS_DOMAIN) && clean.length > CAMPUS_DOMAIN.length
        }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Emits the signed-in Firebase user (or null) whenever the auth state changes. */
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(context: Context, credentialManager: CredentialManager): Result<FirebaseUser> {
        val activity = context as? Activity
            ?: return Result.failure(IllegalStateException("Google Sign-In needs an Activity."))
        // Read by name so the app still compiles before app/google-services.json is added.
        val clientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (clientIdRes == 0) {
            return Result.failure(IllegalStateException("Google Sign-In isn't configured yet (missing google-services.json)."))
        }
        return runCatching {
            val option = GetSignInWithGoogleOption.Builder(serverClientId = context.getString(clientIdRes))
                .setHostedDomainFilter("rajagiri.edu.in")
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = credentialManager.getCredential(activity, request).credential
            check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                "Unexpected credential type."
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
                ?: error("Sign-in returned no user.")
            if (!isRajagiriEmail(user.email)) {
                signOut(credentialManager)
                throw SecurityException("${user.email} isn't a Rajagiri account. Please use your @rajagiri.edu.in Google account.")
            }
            user
        }.onFailure { Log.w(TAG, "Google sign-in failed", it) }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> {
        val clean = email.trim().lowercase()
        if (!isRajagiriEmail(clean)) return Result.failure(SecurityException("Use your @rajagiri.edu.in email address."))
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters."))
        }
        return runCatching {
            val user = auth.createUserWithEmailAndPassword(clean, password).await().user ?: error("No user created.")
            user.sendEmailVerification().await()
            user
        }.mapError()
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        val clean = email.trim().lowercase()
        if (!isRajagiriEmail(clean)) return Result.failure(SecurityException("Use your @rajagiri.edu.in email address."))
        return runCatching {
            auth.signInWithEmailAndPassword(clean, password).await().user ?: error("No user returned.")
        }.mapError()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        if (!isRajagiriEmail(clean)) return Result.failure(SecurityException("Use your @rajagiri.edu.in email address."))
        return runCatching { auth.sendPasswordResetEmail(clean).await(); Unit }.mapError()
    }

    suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        user.sendEmailVerification().await()
        Unit
    }.mapError()

    /** Reloads the user from Firebase and refreshes the ID token so security rules see email_verified. */
    suspend fun reloadAndCheckVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return runCatching {
            user.reload().await()
            val verified = auth.currentUser?.isEmailVerified == true
            if (verified) auth.currentUser?.getIdToken(true)?.await()
            verified
        }.getOrDefault(false)
    }

    suspend fun signOut(credentialManager: CredentialManager?) {
        auth.signOut()
        runCatching { credentialManager?.clearCredentialState(ClearCredentialStateRequest()) }
    }

    /** Firebase only lets an account delete itself shortly after signing in. */
    fun hasRecentLogin(maxAgeMillis: Long = 4 * 60 * 1000L): Boolean {
        val lastSignIn = auth.currentUser?.metadata?.lastSignInTimestamp ?: return false
        return System.currentTimeMillis() - lastSignIn < maxAgeMillis
    }

    /** Deletes the Firebase Auth account. Fails with [RecentLoginRequired] if the session is too old. */
    suspend fun deleteCurrentUser(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in.")
        user.delete().await()
        Unit
    }.recoverCatching { e ->
        if (e is FirebaseAuthRecentLoginRequiredException) throw RecentLoginRequired() else throw e
    }

    class RecentLoginRequired : Exception("For your security, please sign out and sign in again, then retry deleting your account.")

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
