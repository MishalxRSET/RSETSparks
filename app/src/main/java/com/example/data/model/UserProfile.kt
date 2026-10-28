package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a verified student's profile on RSETSparks.
 * Includes fields for bio, list of selected interests, photo URI, and institutional metadata.
 * Serves as both a domain model and Room entity for local storage caching synchronized with Firestore.
 */
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "", // Firebase Auth UID
    val email: String = "",
    val name: String = "",
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val photoUri: String? = null, // URI from Camera capture or Photo Picker
    val department: String = "B.Tech Computer Science",
    val year: Int = 2026,
    val age: Int = 21,
    val isVerified: Boolean = true,
    val isSyncedWithFirestore: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val showBadges: Boolean = false
) {
    /**
     * Converts UserProfile into a Map suitable for Cloud Firestore document storage.
     */
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "email" to email,
        "name" to name,
        "bio" to bio,
        "interests" to interests,
        "photoUri" to (photoUri ?: ""),
        "department" to department,
        "year" to year,
        "age" to age,
        "isVerified" to isVerified,
        "updatedAt" to updatedAt,
        "showBadges" to showBadges
    )

    /**
     * Converts UserProfile into UserEntity for discovery card matching and messaging compatibility.
     */
    fun toUserEntity(): UserEntity {
        val isOfficer = email.trim().equals("u2504040@rajagiri.edu.in", ignoreCase = true)
        val defaultAdminName = if (isOfficer && (name.isBlank() || name == "Rajagiri Student" || name == "Officer Thomas Kurian")) {
            "Mohammed Mishal M"
        } else {
            name
        }
        return UserEntity(
            id = id,
            email = email,
            name = defaultAdminName.ifBlank { email.substringBefore("@").replace(".", " ") },
            age = age,
            department = department,
            year = year,
            bio = bio,
            interests = interests.joinToString(", "),
            photoUri = photoUri,
            firebaseUid = id,
            avatarColorIndex = if (isOfficer && showBadges) 8 else (0..6).random(),
            avatarEmoji = "🎓",
            isVerified = isVerified,
            isAdmin = isOfficer,
            isVisibleInSparks = !isOfficer,
            showBadges = showBadges
        )
    }

    companion object {
        /**
         * Reconstitutes UserProfile from a Firestore document snapshot map.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(id: String, data: Map<String, Any?>): UserProfile {
            val rawInterests = when (val raw = data["interests"]) {
                is List<*> -> raw.mapNotNull { it?.toString() }
                is String -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else -> emptyList()
            }

            return UserProfile(
                id = id,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                bio = data["bio"] as? String ?: "",
                interests = rawInterests,
                photoUri = (data["photoUri"] as? String)?.ifBlank { null },
                department = data["department"] as? String ?: "B.Tech Computer Science",
                year = (data["year"] as? Number)?.toInt() ?: 2026,
                age = (data["age"] as? Number)?.toInt() ?: 21,
                isVerified = data["isVerified"] as? Boolean ?: true,
                isSyncedWithFirestore = true,
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                showBadges = data["showBadges"] as? Boolean ?: false
            )
        }
    }
}
