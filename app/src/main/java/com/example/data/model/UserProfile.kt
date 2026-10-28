package com.example.data.model

/** Profile details collected during onboarding, before they are written to Firestore. */
data class UserProfile(
    val name: String,
    val bio: String,
    val interests: List<String>,
    val localPhotoUri: String? = null, // content:// URI picked or captured on this device
    val department: String,
    val year: Int,
    val age: Int
) {
    companion object {
        const val MIN_AGE = 18
        const val MAX_AGE = 99
        const val MAX_BIO_LENGTH = 500
        const val MAX_NAME_LENGTH = 60
        const val MAX_INTERESTS = 15

        /** Returns a user-facing error, or null when the profile is valid. Mirrors firestore.rules. */
        fun validate(profile: UserProfile): String? = when {
            profile.name.isBlank() -> "Please enter your name."
            profile.name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or fewer."
            profile.age < MIN_AGE -> "RSETSparks is only for students aged $MIN_AGE and over."
            profile.age > MAX_AGE -> "Please enter a valid age."
            profile.bio.length > MAX_BIO_LENGTH -> "Bio must be $MAX_BIO_LENGTH characters or fewer."
            profile.interests.isEmpty() -> "Please select at least one interest."
            profile.interests.size > MAX_INTERESTS -> "Please choose at most $MAX_INTERESTS interests."
            else -> null
        }
    }
}
