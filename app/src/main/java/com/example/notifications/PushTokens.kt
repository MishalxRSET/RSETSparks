package com.example.notifications

import android.content.Context
import com.example.data.repository.AppRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Keeps this device's FCM token attached to the signed-in student.
 * FCM delivers the token to [PushMessagingService.onRegistered]; we remember it locally so it can be
 * removed from the account on sign-out or account deletion.
 */
object PushTokens {
    private const val PREFS = "push"
    private const val KEY_TOKEN = "fcm_token"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)

    /** Called with every new or refreshed token. */
    suspend fun onToken(context: Context, token: String, repository: AppRepository = AppRepository()) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
        val user = Firebase.auth.currentUser ?: return
        repository.registerPushToken(user.uid, user.email.orEmpty(), token)
    }

    /** Asks FCM for a token; it arrives via onRegistered. Re-saves a known token straight away. */
    suspend fun register(context: Context, repository: AppRepository) {
        lastToken(context)?.let { onToken(context, it, repository) }
        FirebaseMessaging.getInstance().register().await()
    }

    /** Detaches this device so the next student to sign in here doesn't get these pushes. */
    suspend fun unregister(context: Context, uid: String, email: String, repository: AppRepository) {
        lastToken(context)?.let { repository.unregisterPushToken(uid, email, it) }
        FirebaseMessaging.getInstance().unregister().await()
        prefs(context).edit().remove(KEY_TOKEN).apply()
    }
}
