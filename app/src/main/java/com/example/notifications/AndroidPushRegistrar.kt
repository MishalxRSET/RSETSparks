package com.example.notifications

import android.content.Context
import com.example.data.AppBackend
import com.example.data.PushRegistrar

/** FCM token handling for the shared ViewModel (see [PushTokens]). */
class AndroidPushRegistrar(private val context: Context, private val repository: AppBackend) : PushRegistrar {
    override suspend fun register() = PushTokens.register(context, repository)
    override suspend fun unregister(uid: String, email: String) = PushTokens.unregister(context, uid, email, repository)
}
