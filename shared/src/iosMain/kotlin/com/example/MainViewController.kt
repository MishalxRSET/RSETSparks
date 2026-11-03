package com.example

import androidx.compose.ui.window.ComposeUIViewController
import cocoapods.FirebaseCore.FIRApp
import com.example.data.AppConfig
import com.example.data.GoogleTokens
import com.example.data.PushRegistrar
import com.example.data.SignInCancelled
import com.example.data.ios.IosAuthBackend
import com.example.data.ios.IosFirestoreBackend
import com.example.ui.RSETSparksApp
import com.example.ui.viewmodel.RSETSparksViewModel
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIViewController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Sign-In, implemented in Swift with the GoogleSignIn SDK (iosApp/iosApp/GoogleSignInBridge.swift).
 */
interface GoogleSignInBridge {
    /** Shows the account picker limited to rajagiri.edu.in and reports back once through [handler]. */
    fun signIn(handler: GoogleSignInResultHandler)

    fun signOut()
}

/** Exactly one call: tokens on success, an error message on failure, or all nulls when the student cancels. */
interface GoogleSignInResultHandler {
    fun onResult(idToken: String?, accessToken: String?, errorMessage: String?)
}

/**
 * The whole app as a UIViewController, called from Swift (iosApp/iosApp/ContentView.swift).
 * Swift must call FirebaseApp.configure() first when GoogleService-Info.plist is bundled; without it the app
 * shows the Firebase setup screen.
 */
fun MainViewController(googleSignIn: GoogleSignInBridge, versionName: String): UIViewController {
    val config = AppConfig(isBlaze = SharedBuildConfig.BLAZE, versionName = versionName)
    val firebaseReady = FIRApp.defaultApp() != null
    val viewModel = RSETSparksViewModel(
        auth = if (firebaseReady) IosAuthBackend(onSignOut = googleSignIn::signOut) else null,
        repository = if (firebaseReady) IosFirestoreBackend() else null,
        push = PushRegistrar.None, // APNs needs an Apple Developer membership; see guides/IOS.md
        config = config
    )
    return ComposeUIViewController {
        RSETSparksApp(viewModel = viewModel, config = config, googleSignIn = { googleSignIn.requestTokens() })
    }
}

private suspend fun GoogleSignInBridge.requestTokens(): GoogleTokens = suspendCancellableCoroutine { cont ->
    signIn(object : GoogleSignInResultHandler {
        override fun onResult(idToken: String?, accessToken: String?, errorMessage: String?) {
            if (!cont.isActive) return
            when {
                idToken != null -> cont.resume(GoogleTokens(idToken, accessToken))
                errorMessage != null -> cont.resumeWithException(IllegalStateException(errorMessage))
                else -> cont.resumeWithException(SignInCancelled())
            }
        }
    })
}
