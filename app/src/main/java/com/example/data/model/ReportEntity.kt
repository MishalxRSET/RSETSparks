package com.example.data.model

/** A safety report in `reports/{id}`, reviewed by admins. */
data class ReportEntity(
    val id: String,
    val reporterId: String,
    val reportedUserId: String,
    val reporterEmail: String,
    val reportedEmail: String,
    val reportedUserName: String,
    val reasonCategory: String,
    val description: String,
    val status: String = "PENDING", // "PENDING", "INVESTIGATING", "RESOLVED_WARNED", "RESOLVED_SUSPENDED", "DISMISSED"
    val chatTranscriptSnapshot: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val adminNote: String = ""
)
