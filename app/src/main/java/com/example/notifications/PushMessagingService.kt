package com.example.notifications

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives data-only pushes sent by the Cloud Functions in /functions and keeps this device's FCM
 * token registered for the signed-in student.
 */
class PushMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRegistered(token: String) {
        scope.launch { runCatching { PushTokens.onToken(applicationContext, token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val partnerId = data["partnerId"] ?: return
        // Ignore pushes meant for a student who has since signed out on this device.
        if (data["recipientId"] != null && data["recipientId"] != Firebase.auth.currentUser?.uid) return
        Notifications.show(
            context = this,
            type = data["type"] ?: "message",
            partnerId = partnerId,
            title = data["title"] ?: "RSETSparks",
            body = data["body"] ?: ""
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
