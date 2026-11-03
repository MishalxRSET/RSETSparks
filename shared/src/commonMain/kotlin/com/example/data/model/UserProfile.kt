package com.example.data.model

/** Profile details entered during onboarding or profile editing, before they are written to Firestore. */
data class UserProfile(
    val name: String,
    val bio: String,
    val interests: List<String>,
    // content:// = new photo to upload, https:// = keep the existing uploaded photo, null = no photo
    val photoUri: String? = null,
    val department: String,
    val year: Int,
    val age: Int,
    val gender: String = "", // a key of GENDERS
    val showMe: String = "", // a key of SHOW_ME: whose profiles appear in this student's deck
    val orientation: String = "", // optional, a key of ORIENTATIONS
    val lookingFor: String = "", // optional, a key of LOOKING_FOR
    val pronouns: String = "" // optional free text
) {
    companion object {
        const val MIN_AGE = 18
        const val MAX_AGE = 99
        const val MAX_BIO_LENGTH = 500
        const val MAX_NAME_LENGTH = 60
        const val MAX_INTERESTS = 15
        const val MAX_PRONOUNS_LENGTH = 20

        // Stored keys -> labels. firestore.rules lists the same keys.
        val GENDERS = linkedMapOf(
            "MAN" to "Man", "WOMAN" to "Woman", "TRANS_MAN" to "Trans man", "TRANS_WOMAN" to "Trans woman", "NON_BINARY" to "Non-binary"
        )
        val SHOW_ME = linkedMapOf("MEN" to "Men", "WOMEN" to "Women", "EVERYONE" to "Everyone")
        val ORIENTATIONS = linkedMapOf(
            "STRAIGHT" to "Straight", "GAY" to "Gay", "LESBIAN" to "Lesbian", "BISEXUAL" to "Bisexual", "QUEER" to "Queer"
        )
        val LOOKING_FOR = linkedMapOf(
            "RELATIONSHIP" to "A relationship", "CASUAL" to "Something casual", "FRIENDS" to "Friends", "NOT_SURE" to "Not sure yet"
        )

        /** Whether someone who chose [showMe] wants to see a student of [gender]. Trans men count as men, trans women as women. */
        fun wantsToSee(showMe: String, gender: String): Boolean = when (showMe) {
            "MEN" -> gender == "MAN" || gender == "TRANS_MAN"
            "WOMEN" -> gender == "WOMAN" || gender == "TRANS_WOMAN"
            "EVERYONE" -> gender in GENDERS
            else -> false
        }

        /** Two students appear in each other's decks only when each fits the other's "Show me". */
        fun mutualMatch(a: UserEntity, b: UserEntity): Boolean =
            wantsToSee(a.showMe, b.gender) && wantsToSee(b.showMe, a.gender)

        /** Returns a user-facing error, or null when the profile is valid. Mirrors firestore.rules. */
        fun validate(profile: UserProfile): String? = when {
            profile.name.isBlank() -> "Please enter your name."
            profile.name.length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or fewer."
            profile.age < MIN_AGE -> "RSETSparks is only for students aged $MIN_AGE and over."
            profile.age > MAX_AGE -> "Please enter a valid age."
            profile.bio.length > MAX_BIO_LENGTH -> "Bio must be $MAX_BIO_LENGTH characters or fewer."
            profile.interests.isEmpty() -> "Please select at least one interest."
            profile.interests.size > MAX_INTERESTS -> "Please choose at most $MAX_INTERESTS interests."
            profile.gender !in GENDERS -> "Please choose your gender."
            profile.showMe !in SHOW_ME -> "Please choose who you'd like to see."
            profile.orientation.isNotEmpty() && profile.orientation !in ORIENTATIONS -> "Please choose a valid orientation."
            profile.lookingFor.isNotEmpty() && profile.lookingFor !in LOOKING_FOR -> "Please choose what you're looking for."
            profile.pronouns.trim().length > MAX_PRONOUNS_LENGTH -> "Pronouns must be $MAX_PRONOUNS_LENGTH characters or fewer."
            else -> null
        }
    }
}
