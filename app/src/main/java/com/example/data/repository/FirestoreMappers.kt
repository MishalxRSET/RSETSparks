package com.example.data.repository

import com.example.data.model.AuditLogEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp

/**
 * Conversions between Firestore documents (plain maps) and app models.
 * Kept free of Firebase SDK types other than [Timestamp] so they can be unit-tested on the JVM.
 */
object FirestoreMappers {

    fun pairId(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"

    fun swipeId(from: String, to: String): String = "${from}_$to"

    fun blockId(blocker: String, blocked: String): String = "${blocker}_$blocked"

    fun userToMap(id: String, profile: UserProfile, photoUrl: String?, colorIndex: Int, visible: Boolean): Map<String, Any?> = mapOf(
        "name" to profile.name.trim(),
        "age" to profile.age.toLong(),
        "department" to profile.department,
        "year" to profile.year.toLong(),
        "bio" to profile.bio.trim(),
        "interests" to profile.interests,
        "photoUrl" to (photoUrl ?: ""),
        "avatarEmoji" to "🎓",
        "avatarColorIndex" to colorIndex.toLong(),
        "verified" to true,
        "suspended" to false,
        "warningCount" to 0L,
        "visible" to visible,
        "showBadges" to false,
        "uid" to id
    ) + profileDetailsToMap(profile)

    /** The gender / "Show me" / optional dating fields, shared by profile creation and edits. */
    fun profileDetailsToMap(profile: UserProfile): Map<String, Any?> = mapOf(
        "gender" to profile.gender,
        "showMe" to profile.showMe,
        "orientation" to profile.orientation,
        "lookingFor" to profile.lookingFor,
        "pronouns" to profile.pronouns.trim()
    )

    fun userFromMap(id: String, data: Map<String, Any?>, email: String = "", isAdmin: Boolean? = null): UserEntity {
        val showBadges = data["showBadges"] as? Boolean ?: false
        return UserEntity(
            id = id,
            email = email,
            name = data["name"] as? String ?: "Rajagiri Student",
            age = (data["age"] as? Number)?.toInt() ?: 18,
            department = data["department"] as? String ?: "",
            year = (data["year"] as? Number)?.toInt() ?: 0,
            bio = data["bio"] as? String ?: "",
            interests = interestsToCsv(data["interests"]),
            photoUri = (data["photoUrl"] as? String)?.ifBlank { null },
            avatarColorIndex = (data["avatarColorIndex"] as? Number)?.toInt() ?: 0,
            avatarEmoji = data["avatarEmoji"] as? String ?: "🎓",
            isVerified = data["verified"] as? Boolean ?: true,
            // Rules only let admins set showBadges, so it doubles as the public "is an officer" signal.
            isAdmin = isAdmin ?: showBadges,
            isSuspended = data["suspended"] as? Boolean ?: false,
            warningCount = (data["warningCount"] as? Number)?.toInt() ?: 0,
            isVisibleInSparks = data["visible"] as? Boolean ?: true,
            showBadges = showBadges,
            gender = data["gender"] as? String ?: "",
            showMe = data["showMe"] as? String ?: "",
            orientation = data["orientation"] as? String ?: "",
            lookingFor = data["lookingFor"] as? String ?: "",
            pronouns = data["pronouns"] as? String ?: ""
        )
    }

    private fun interestsToCsv(raw: Any?): String = when (raw) {
        is List<*> -> raw.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
        is String -> raw
        else -> ""
    }

    fun messageFromMap(id: String, data: Map<String, Any?>, hasPendingWrites: Boolean): ChatMessage = ChatMessage(
        id = id,
        senderId = data["senderId"] as? String ?: "",
        text = data["text"] as? String ?: "",
        // createdAt is a server timestamp, which reads as null until the write is acknowledged.
        timestamp = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
        isPending = hasPendingWrites
    )

    fun reportFromMap(id: String, data: Map<String, Any?>): ReportEntity = ReportEntity(
        id = id,
        reporterId = data["reporterId"] as? String ?: "",
        reportedUserId = data["reportedUserId"] as? String ?: "",
        reporterEmail = data["reporterEmail"] as? String ?: "",
        reportedEmail = data["reportedEmail"] as? String ?: "",
        reportedUserName = data["reportedUserName"] as? String ?: "",
        reasonCategory = data["reason"] as? String ?: "",
        description = data["description"] as? String ?: "",
        status = data["status"] as? String ?: "PENDING",
        chatTranscriptSnapshot = data["transcript"] as? String ?: "",
        createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L,
        adminNote = data["adminNote"] as? String ?: ""
    )

    fun auditLogFromMap(id: String, data: Map<String, Any?>): AuditLogEntity = AuditLogEntity(
        id = id,
        adminEmail = data["adminEmail"] as? String ?: "",
        action = data["action"] as? String ?: "",
        targetUserId = data["targetUserId"] as? String ?: "",
        targetUserEmail = data["targetUserEmail"] as? String ?: "",
        reason = data["reason"] as? String ?: "",
        timestamp = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
    )
}
