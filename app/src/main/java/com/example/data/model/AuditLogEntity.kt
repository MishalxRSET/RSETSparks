package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val adminEmail: String,
    val action: String, // "VIEWED_CHAT_TRANSCRIPT", "SUSPENDED_USER", "ISSUED_WARNING", "VERIFIED_USER", "DISMISSED_REPORT"
    val targetUserId: String,
    val targetUserEmail: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
