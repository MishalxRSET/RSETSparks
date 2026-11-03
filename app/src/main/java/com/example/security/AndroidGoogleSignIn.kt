package com.example.security

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.GoogleTokens
import com.example.data.SignInCancelled
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Shows the Google account picker (Credential Manager), limited to rajagiri.edu.in, and returns its ID token. */
class AndroidGoogleSignIn(private val activity: Activity) {

    suspend fun requestTokens(): GoogleTokens {
        // Read by name so the app still compiles before app/google-services.json is added.
        val clientIdRes = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        check(clientIdRes != 0) { "Google Sign-In isn't configured yet (missing google-services.json)." }

        val option = GetSignInWithGoogleOption.Builder(serverClientId = activity.getString(clientIdRes))
            .setHostedDomainFilter("rajagiri.edu.in")
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelled()
        } catch (e: Exception) {
            Log.w("AndroidGoogleSignIn", "Google account picker failed", e)
            throw e
        }
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type."
        }
        return GoogleTokens(idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken)
    }
}
