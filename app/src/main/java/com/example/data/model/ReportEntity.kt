package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val reportedUserId: String,
    val reporterEmail: String,
    val reportedEmail: String,
    val reportedUserName: String,
    val reasonCategory: String, // "Harassment", "Impersonation", "Inappropriate Content", "Campus Safety Risk", "Suspicious Account"
    val description: String,
    val status: String = "PENDING", // "PENDING", "INVESTIGATING", "RESOLVED_WARNED", "RESOLVED_SUSPENDED", "DISMISSED"
    val chatTranscriptSnapshot: String = "", // Decrypted transcript snapshot for admin review
    val createdAt: Long = System.currentTimeMillis(),
    val adminNote: String = ""
)
