package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // Firebase Auth UID or generated student ID
    val email: String,
    val name: String,
    val age: Int,
    val department: String,
    val year: Int,
    val bio: String,
    val interests: String, // Comma separated e.g. "Coding, Robotics, Filter Coffee"
    val photoUri: String? = null, // Content URI of camera capture or picked photo
    val firebaseUid: String = "", // Linked Firebase Auth Account UID
    val avatarColorIndex: Int = 0,
    val avatarEmoji: String = "🎓",
    val isVerified: Boolean = true,
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false,
    val warningCount: Int = 0,
    val isVisibleInSparks: Boolean = true,
    val campusZone: String = "Rajagiri Valley Campus, Kakkanad",
    val showBadges: Boolean = false
) {
    fun getInterestsList(): List<String> =
        if (interests.isBlank()) emptyList() else interests.split(",").map { it.trim() }

    fun getEffectiveDepartment(): String =
        if (isAdmin && !showBadges) "B.Tech Computer Science" else department

    fun getEffectiveBio(): String =
        if (isAdmin && !showBadges) "Verified Rajagiri student. Excited to meet fellow campus peers!" else bio
}
