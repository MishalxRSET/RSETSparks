package com.example.data.model

/** A student profile as shown in the app. Backed by the Firestore document `users/{id}`. */
data class UserEntity(
    val id: String,
    val email: String = "", // Only loaded for the signed-in user and for admins (privateProfiles/{id})
    val name: String,
    val age: Int,
    val department: String,
    val year: Int,
    val bio: String,
    val interests: String, // Comma separated e.g. "Coding, Robotics, Filter Coffee"
    val photoUri: String? = null, // Download URL of the profile photo in Firebase Storage
    val avatarColorIndex: Int = 0,
    val avatarEmoji: String = "🎓",
    val isVerified: Boolean = true,
    val isAdmin: Boolean = false,
    val isSuspended: Boolean = false,
    val warningCount: Int = 0,
    val isVisibleInSparks: Boolean = true,
    val campusZone: String = "Rajagiri Valley Campus, Kakkanad",
    val showBadges: Boolean = false,
    val gender: String = "", // keys from UserProfile.GENDERS etc.; blank on profiles made before these fields existed
    val showMe: String = "",
    val orientation: String = "",
    val lookingFor: String = "",
    val pronouns: String = ""
) {
    /** e.g. "Woman · she/her · Bisexual"; empty when nothing is set. */
    fun identityLine(): String = listOfNotNull(
        UserProfile.GENDERS[gender], pronouns.ifBlank { null }, UserProfile.ORIENTATIONS[orientation]
    ).joinToString(" · ")

    fun lookingForLabel(): String? = UserProfile.LOOKING_FOR[lookingFor]

    fun getInterestsList(): List<String> =
        if (interests.isBlank()) emptyList() else interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
