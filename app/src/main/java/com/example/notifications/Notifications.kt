package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/** Notification channels and builders for match and message pushes. */
object Notifications {
    const val CHANNEL_MATCHES = "matches"
    const val CHANNEL_MESSAGES = "messages"
    const val EXTRA_OPEN_CHAT_WITH = "com.rsetsparks.OPEN_CHAT_WITH"

    /** Partner whose chat is open on screen; their message pushes are skipped. */
    @Volatile
    var activeChatPartnerId: String? = null

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_MATCHES, "New matches", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "When you and another student like each other" },
                NotificationChannel(CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "New chat messages from your matches" }
            )
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Shows a push. Message text is hidden on a secure lock screen (VISIBILITY_PRIVATE), where only
     * "New message" is shown.
     */
    fun show(context: Context, type: String, partnerId: String, title: String, body: String) {
        if (!canPost(context)) return
        if (type == "message" && partnerId == activeChatPartnerId) return

        val channel = if (type == "match") CHANNEL_MATCHES else CHANNEL_MESSAGES
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_OPEN_CHAT_WITH, partnerId)
        val pending = PendingIntent.getActivity(
            context, partnerId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicVersion = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_spark)
            .setColor(ContextCompat.getColor(context, R.color.brand_rose))
            .setContentTitle(if (type == "match") "New Spark!" else "New message")
            .build()
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_spark)
            .setColor(ContextCompat.getColor(context, R.color.brand_rose))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(if (type == "match") NotificationCompat.CATEGORY_SOCIAL else NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        // One notification per conversation; newer messages replace older ones.
        @Suppress("MissingPermission") // checked by canPost()
        NotificationManagerCompat.from(context).notify("$type:$partnerId", 0, notification)
    }
}
