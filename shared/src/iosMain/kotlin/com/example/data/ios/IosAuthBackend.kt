package com.example.data.ios

import com.example.data.AuthBackend
import com.example.data.AuthUser
import com.example.data.CampusEmail
import com.example.data.CampusEmail.MIN_PASSWORD_LENGTH
import com.example.data.GoogleTokens
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import platform.Foundation.NSTimeIntervalSince1970

/**
 * Firebase Authentication on iOS (GitLive wrapper over the Firebase iOS SDK). Mirrors the Android
 * AuthService: @rajagiri.edu.in only, Google Sign-In tokens from the Swift GoogleSignIn bridge, or
 * email + password with a verification link.
 *
 * @param onSignOut also signs out of GoogleSignIn so the account picker shows again next time.
 */
class IosAuthBackend(
    private val onSignOut: () -> Unit = {},
    private val auth: FirebaseAuth = Firebase.auth
) : AuthBackend {

    override val currentUser: AuthUser? get() = auth.currentUser?.toAuthUser()

    override fun authState(): Flow<AuthUser?> = auth.authStateChanged.map { it?.toAuthUser() }

    override suspend fun signInWithGoogle(tokens: GoogleTokens): Result<AuthUser> = runCatching {
        val user = auth.signInWithCredential(GoogleAuthProvider.credential(tokens.idToken, tokens.accessToken)).user
            ?: error("Sign-in returned no user.")
        if (!CampusEmail.isRajagiri(user.email)) {
            signOut()
            throw IllegalStateException("${user.email} isn't a Rajagiri account. Please use your @rajagiri.edu.in Google account.")
        }
        user.toAuthUser()
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> {
        val clean = email.trim().lowercase()
        if (!CampusEmail.isRajagiri(clean)) return Result.failure(IllegalArgumentException("Use your @rajagiri.edu.in email address."))
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters."))
        }
        return runCatching {
            val user = auth.createUserWithEmailAndPassword(clean, password).user ?: error("No user created.")
            user.sendEmailVerification()
            user.toAuthUser()
        }.mapError()
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        val clean = email.trim().lowercase()
        if (!CampusEmail.isRajagiri(clean)) return Result.failure(IllegalArgumentException("Use your @rajagiri.edu.in email address."))
        return runCatching {
            (auth.signInWithEmailAndPassword(clean, password).user ?: error("No user returned.")).toAuthUser()
        }.mapError()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val clean = email.trim().lowercase()
        if (!CampusEmail.isRajagiri(clean)) return Result.failure(IllegalArgumentException("Use your @rajagiri.edu.in email address."))
        return runCatching { auth.sendPasswordResetEmail(clean) }.mapError()
    }

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        (auth.currentUser ?: error("Not signed in.")).sendEmailVerification()
    }.mapError()

    override suspend fun reloadAndCheckVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return runCatching {
            user.reload()
            val verified = auth.currentUser?.isEmailVerified == true
            if (verified) auth.currentUser?.getIdToken(true)
            verified
        }.getOrDefault(false)
    }

    override suspend fun signOut() {
        auth.signOut()
        runCatching(onSignOut)
    }

    override suspend fun deleteCurrentUser(): Result<Unit> = runCatching {
        (auth.currentUser ?: error("Not signed in.")).delete()
    }.recoverCatching { e ->
        if (e is FirebaseAuthRecentLoginRequiredException) {
            throw IllegalStateException("For your security, please sign out and sign in again, then retry deleting your account.")
        }
        throw e
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        isEmailVerified = isEmailVerified,
        displayName = displayName.orEmpty(),
        // The iOS SDK reports seconds since 2001-01-01 (Apple's reference date), not the Unix epoch.
        lastSignInMillis = metaData?.lastSignInTime?.let { ((it + NSTimeIntervalSince1970) * 1000).toLong() }
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
